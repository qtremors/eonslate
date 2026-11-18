package dev.qtremors.eonslate.feature.finance

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.domain.finance.asTransaction
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Payment SMS found when the app opens. With auto-approve they are added at once and [added]
 * says how many; otherwise they wait in [pending] for review. Already recorded, dismissed, and
 * payments in a currency no account uses are left out.
 */
@Singleton
class SmsDetection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val finance: FinanceRepository,
    private val ids: IdGenerator,
    private val clock: AppClock,
) {
    private val lock = Mutex()
    private var ran = false
    private val _pending = MutableStateFlow<List<PaymentMessage>>(emptyList())
    val pending: StateFlow<List<PaymentMessage>> = _pending.asStateFlow()
    private val _added = MutableStateFlow(0)
    val added: StateFlow<Int> = _added.asStateFlow()

    /** Scans once per launch; later calls do nothing. */
    suspend fun runOnce() = lock.withLock {
        if (ran) return@withLock
        ran = true
        scan()
    }

    private suspend fun scan() {
        val sms = settings.sms.first()
        if (!sms.detect || !PaymentInbox.hasAccess(context)) return
        val zone = ZoneId.of(clock.zoneId())
        val found = runCatching { PaymentInbox.scan(context, clock.nowMillis(), zone, DAYS) }.getOrDefault(emptyList())
        val ledger = finance.observeLedger().first()
        val known = ledger.transactions.mapNotNullTo(HashSet()) { it.smsKey }
        val fresh = found.filter { m ->
            m.key != null && m.key !in known && m.key !in sms.discarded && ledger.accounts.any { it.currency == m.currency }
        }
        if (fresh.isEmpty()) return
        if (sms.autoApprove) {
            val today = Instant.ofEpochMilli(clock.nowMillis()).atZone(zone).toLocalDate().toEpochDay()
            val transactions = fresh.mapNotNull { it.asTransaction(ids.newId(), ledger.accounts, today) }
            if (finance.appendTransactions(transactions)) _added.value = transactions.size
        } else {
            _pending.value = fresh
        }
    }

    /** Adds every held payment. */
    suspend fun approveAll() {
        val ledger = finance.observeLedger().first()
        val today = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.of(clock.zoneId())).toLocalDate().toEpochDay()
        val transactions = _pending.value.mapNotNull { it.asTransaction(ids.newId(), ledger.accounts, today) }
        if (finance.appendTransactions(transactions)) _pending.value = emptyList()
    }

    /** Dismisses [messages] for good. */
    suspend fun discard(messages: List<PaymentMessage>) {
        val keys = messages.mapNotNull { it.key }.toSet()
        settings.updateSms { it.copy(discarded = it.discarded + keys) }
        _pending.value = _pending.value.filter { it.key !in keys }
    }

    /** A payment reviewed one by one was recorded or skipped; it no longer waits. */
    fun reviewed(message: PaymentMessage) {
        _pending.value = _pending.value.filter { it.key != message.key }
    }

    fun addedShown() {
        _added.value = 0
    }

    private companion object {
        const val DAYS = 30
    }
}
