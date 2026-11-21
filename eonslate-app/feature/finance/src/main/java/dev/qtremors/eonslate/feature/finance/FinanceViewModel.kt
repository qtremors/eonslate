package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentDuplicates
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dev.qtremors.eonslate.core.tracker.domain.finance.MoneySummary
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val finance: FinanceRepository,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: AppClock,
    val detection: SmsDetection,
) : ViewModel() {
    val ledger: StateFlow<Ledger?> = finance.observeLedger()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currency = settings.tracking.map { it.currencyCode() }

    /** Home's money card: worked out off the main thread from the ledger and the shown currency. */
    val overview: StateFlow<Overview?> = combine(ledger.filterNotNull(), currency) { data, code ->
        Overview(MoneySummary.of(data, today(), data.shownCurrency(code)), chartSlots(data), data.currencies(), data.accounts.isEmpty())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class Overview(val summary: MoneySummary?, val slots: Map<String, Int>, val currencies: List<String>, val noAccounts: Boolean)

    /** Shows totals and charts in [code] on every tab; it is the app's currency setting. */
    fun showCurrency(code: String) {
        viewModelScope.launch { settings.updateTracking { it.copy(currency = code) } }
    }
    val firstDay = settings.tracking.map { it.firstDayOfWeek() }

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    val inboxAvailable = PaymentInbox.available(context)
    private val _inbox = MutableStateFlow(InboxState())
    internal val inbox = _inbox.asStateFlow()
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            val now = now()
            finance.postDue(now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute)
        }
        viewModelScope.launch { detection.refresh() }
    }

    private fun now() = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.of(clock.zoneId()))

    fun today() = now().toLocalDate()

    fun newId() = ids.newId()

    private var creatingAccount = false

    /** Money works from the first launch: one account in the app's currency, made once. */
    fun ensureAccount() {
        val data = ledger.value ?: return
        if (data.accounts.isNotEmpty() || _busy.value || creatingAccount) return
        creatingAccount = true
        viewModelScope.launch {
            val code = currency.first()
            finance.saveAccount(Account(ids.newId(), context.getString(R.string.finance_default_account), code))
        }
    }

    fun clearError() {
        _failed.value = false
    }

    fun save(account: Account, onDone: () -> Unit) = write(onDone) { finance.saveAccount(account) }
    /** Saves [transaction]; a reviewed payment stops waiting only once it is really saved. */
    fun save(transaction: Transaction, onDone: () -> Unit) = write(onDone) {
        finance.saveTransaction(transaction).also { saved -> if (saved) transaction.smsKey?.let(detection::recorded) }
    }
    fun delete(account: Account, onDone: () -> Unit) = write(onDone) { finance.deleteAccount(account.id) }
    /** Deletes several transactions in one go, as selection mode asks. */
    fun deleteTransactions(ids: Collection<String>, onDone: () -> Unit) = write(onDone) {
        finance.deleteTransactions(ids)
        true
    }

    /** Puts back a deleted transaction, as Undo asks; never dropped while another write is busy. */
    fun restore(transaction: Transaction) {
        viewModelScope.launch {
            try {
                finance.saveTransaction(transaction)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _failed.value = true
            }
        }
    }

    fun delete(transaction: Transaction, onDone: () -> Unit) = write(onDone) {
        finance.deleteTransaction(transaction.id)
        true
    }

    /** Removes icon files no subscription uses; read from the database, so a just-saved icon counts. */
    fun pruneIcons() {
        viewModelScope.launch {
            runCatching {
                val used = finance.observeLedger().first().recurring.mapNotNullTo(HashSet()) { it.icon }
                CustomIcons.prune(context, used)
            }
        }
    }

    fun schedule(expected: RecurringPayment?, payment: RecurringPayment, onDone: () -> Unit) = write(onDone) {
        val now = now()
        val day = now.toLocalDate().toEpochDay()
        val minute = now.hour * 60 + now.minute
        if (expected == null) finance.addRecurring(payment, day, minute)
        else finance.updateRecurring(expected, payment, day, minute)
    }

    /**
     * Pauses, or resumes from the next occurrence on or after today: payments that fell due while
     * it was paused are skipped, not posted.
     */
    fun setPaused(payment: RecurringPayment, paused: Boolean) = write({}) {
        val now = now()
        val today = now.toLocalDate().atStartOfDay()
        var next = payment.nextOccurrence
        var steps = 0
        while (!paused && next != null && next < today && steps++ < MAX_SKIPS) next = payment.after(next)
        val minute = next?.takeIf { payment.interval == PaymentInterval.HOURLY }?.let { it.hour * 60 + it.minute }
        val resumed = if (paused) payment.copy(paused = true)
        else payment.copy(paused = false, nextEpochDay = next?.toLocalDate()?.toEpochDay(), nextMinute = minute)
        finance.updateRecurring(payment, resumed, now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute)
    }

    fun delete(payment: RecurringPayment, onDone: () -> Unit) = write(onDone) {
        finance.deleteRecurring(payment.id)
        true
    }

    fun record(payment: RecurringPayment, minor: Long, day: Long, onDone: () -> Unit) = write(onDone) {
        finance.recordRecurring(payment, minor, day)
    }

    fun save(budget: CategoryBudget, onDone: () -> Unit) = write(onDone) { finance.saveBudget(budget) }

    fun save(category: CategoryChoice, previous: CategoryChoice?, onDone: () -> Unit) =
        write(onDone) { finance.saveCategory(category, previous) }

    fun delete(category: CategoryChoice, onDone: () -> Unit) = write(onDone) { finance.deleteCategory(category) }

    fun delete(budget: CategoryBudget, onDone: () -> Unit) = write(onDone) {
        finance.deleteBudget(budget.id)
        true
    }

    fun scanInbox(days: Int) {
        if (!inboxAvailable || days !in setOf(7, 30, 90)) return
        scanJob?.cancel()
        _inbox.value = InboxState(scanning = true)
        scanJob = viewModelScope.launch {
            try {
                // The same filter as automatic detection: nothing recorded, discarded or entered by hand.
                val data = finance.observeLedger().first()
                val handled = detection.handledKeys(data, settings.sms.first().discarded)
                val found = PaymentInbox.scan(context, clock.nowMillis(), ZoneId.of(clock.zoneId()), days) { it in handled }
                _inbox.value = InboxState(messages = PaymentDuplicates.fresh(found, data, today().toEpochDay()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _inbox.value = InboxState(failed = true)
            }
        }
    }

    /** Adds [messages], then drops them from the scan shown; the result says what happened. */
    suspend fun approveAll(messages: List<PaymentMessage>): BatchResult {
        val result = detection.approve(messages)
        val done = messages.mapNotNullTo(HashSet()) { it.key }
        _inbox.value = _inbox.value.let { state -> state.copy(messages = state.messages?.filter { it.key !in done }) }
        return result
    }

    fun clearInbox() {
        scanJob?.cancel()
        scanJob = null
        _inbox.value = InboxState()
    }

    private fun write(onDone: () -> Unit, action: suspend () -> Boolean) {
        if (_busy.value) return
        _busy.value = true
        _failed.value = false
        viewModelScope.launch {
            try {
                if (action()) onDone() else _failed.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _failed.value = true
            } finally {
                _busy.value = false
            }
        }
    }
}

/** Occurrences a resume will skip before giving up, so a decades-old hourly schedule cannot stall. */
private const val MAX_SKIPS = 100_000

internal data class InboxState(
    val scanning: Boolean = false,
    val messages: List<PaymentMessage>? = null,
    val failed: Boolean = false,
)
