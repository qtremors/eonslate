package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.domain.finance.asTransaction
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val finance: FinanceRepository,
    settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: AppClock,
    val detection: SmsDetection,
) : ViewModel() {
    val ledger: StateFlow<Ledger?> = finance.observeLedger()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currency = settings.tracking.map { it.currencyCode() }
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
        viewModelScope.launch { detection.runOnce() }
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
    fun save(transaction: Transaction, onDone: () -> Unit) = write(onDone) { finance.saveTransaction(transaction) }
    fun delete(account: Account, onDone: () -> Unit) = write(onDone) { finance.deleteAccount(account.id) }
    /** Deletes several transactions in one go, as selection mode asks. */
    fun deleteTransactions(ids: Collection<String>, onDone: () -> Unit) = write(onDone) {
        finance.deleteTransactions(ids)
        true
    }

    fun delete(transaction: Transaction, onDone: () -> Unit) = write(onDone) {
        finance.deleteTransaction(transaction.id)
        true
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
                val messages = PaymentInbox.scan(context, clock.nowMillis(), ZoneId.of(clock.zoneId()), days)
                _inbox.value = InboxState(messages = messages)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _inbox.value = InboxState(failed = true)
            }
        }
    }

    /** Adds [messages] as transactions in one write, and forgets them as found. */
    suspend fun approveAll(messages: List<PaymentMessage>) {
        val data = ledger.value ?: return
        val today = today().toEpochDay()
        val transactions = messages.mapNotNull { it.asTransaction(ids.newId(), data.accounts, today) }
        if (finance.appendTransactions(transactions)) {
            messages.forEach(detection::reviewed)
            _inbox.value = InboxState(messages = emptyList())
        }
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
