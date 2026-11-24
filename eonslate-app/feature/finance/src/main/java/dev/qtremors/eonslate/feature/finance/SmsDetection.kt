package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.domain.finance.PartyBook
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentDuplicates
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.domain.finance.asTransaction
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What one "Add all" did: how many were added, how many have no account in their currency, and how many could not be saved. */
data class BatchResult(val added: Int, val noAccount: Int = 0, val failed: Int = 0)

/**
 * Payment SMS found in the inbox. It scans when detection is turned on, when the app comes back
 * to the front, and when asked; with auto-approve, sure matches are added at once. The rest wait
 * in [pending] for review. Recorded, discarded, and already-entered payments are left out.
 */
@Singleton
class SmsDetection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val finance: FinanceRepository,
    private val ids: IdGenerator,
    private val clock: AppClock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private var lastScan = Long.MIN_VALUE
    private val _pending = MutableStateFlow<List<PaymentMessage>>(emptyList())
    val pending: StateFlow<List<PaymentMessage>> = _pending.asStateFlow()
    private val _result = MutableStateFlow<BatchResult?>(null)
    /** The latest batch added without review or with "Add all", until it has been shown. */
    val result: StateFlow<BatchResult?> = _result.asStateFlow()

    init {
        // Turning detection on scans straight away, rather than at the next launch.
        scope.launch {
            settings.sms.map { it.detect }.distinctUntilChanged().collect { on -> if (on) refresh(force = true) }
        }
    }

    /**
     * Scans again unless one ran in the last minute; [force] scans regardless, as after access is
     * granted. A scan already running is not repeated.
     */
    suspend fun refresh(force: Boolean = false) {
        if (!lock.tryLock()) return
        try {
            val now = clock.nowMillis()
            if (!force && now - lastScan < MIN_INTERVAL) return
            lastScan = now
            scan()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A failed read leaves what was found before; the next return to the app tries again.
        } finally {
            lock.unlock()
        }
    }

    /** Asks for a scan from a place with no coroutine of its own, such as an activity resuming. */
    fun refreshLater() {
        scope.launch { refresh() }
    }

    private suspend fun scan() {
        val sms = settings.sms.first()
        if (!sms.detect || !PaymentInbox.hasAccess(context)) {
            _pending.value = emptyList()
            return
        }
        val zone = ZoneId.of(clock.zoneId())
        val ledger = finance.observeLedger().first()
        val handled = handledKeys(ledger, sms.discarded)
        val found = PaymentInbox.scan(context, clock.nowMillis(), zone, DAYS) { it in handled }
        val fresh = PaymentDuplicates.fresh(found, ledger, today())
            .filter { m -> ledger.accounts.any { it.currency == m.currency } }
        val (sure, review) = fresh.partition { sms.autoApprove && it.confidence >= 1f }
        _pending.value = review
        if (sure.isNotEmpty()) {
            val result = add(sure, ledger)
            if (result.added > 0) _result.value = result
        }
    }

    /** Fingerprints of payments already recorded or discarded, so a scan can skip them before counting. */
    internal fun handledKeys(ledger: Ledger, discarded: Set<String>): Set<String> =
        ledger.transactions.mapNotNullTo(HashSet(discarded)) { it.smsKey }

    /** Adds [messages], in one write when it can; any that fail together are tried one by one. */
    suspend fun approve(messages: List<PaymentMessage>): BatchResult {
        val ledger = finance.observeLedger().first()
        val result = add(PaymentDuplicates.fresh(messages, ledger, today()), ledger)
        val done = messages.mapNotNullTo(HashSet()) { it.key }
        _pending.value = _pending.value.filter { it.key !in done }
        _result.value = result
        return result
    }

    /** Adds every held payment. */
    suspend fun approveAll(): BatchResult = approve(_pending.value)

    private suspend fun add(messages: List<PaymentMessage>, ledger: Ledger): BatchResult {
        val day = today()
        // Each payee becomes a person or business, found by name or made once for the batch.
        val book = PartyBook(ledger.parties, ids, clock.nowMillis())
        val drafts = messages.map { it to it.asTransaction(ids.newId(), ledger.accounts, day, book) }
        val noAccount = drafts.count { it.second == null }
        val transactions = drafts.mapNotNull { it.second }
        if (transactions.isEmpty() || finance.appendTransactions(transactions, book.created)) return BatchResult(transactions.size, noAccount)
        book.created.forEach { finance.saveParty(it) }
        // One bad entry fails the batch; save the rest alone so a duplicate costs only itself.
        var added = 0
        for (transaction in transactions) if (finance.saveTransaction(transaction)) added++
        return BatchResult(added, noAccount, transactions.size - added)
    }

    /** Dismisses [messages] for good, from automatic and manual scans alike. */
    suspend fun discard(messages: List<PaymentMessage>) {
        val keys = messages.mapNotNull { it.key }.toSet()
        settings.updateSms { it.copy(discarded = it.discarded + keys) }
        _pending.value = _pending.value.filter { it.key !in keys }
    }

    /** A payment with this fingerprint was saved; it no longer waits. */
    fun recorded(key: String) {
        _pending.value = _pending.value.filter { it.key != key }
    }

    fun resultShown() {
        _result.value = null
    }

    private fun today(): Long = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.of(clock.zoneId())).toLocalDate().toEpochDay()

    private companion object {
        const val DAYS = 30
        const val MIN_INTERVAL = 60_000L
    }
}
