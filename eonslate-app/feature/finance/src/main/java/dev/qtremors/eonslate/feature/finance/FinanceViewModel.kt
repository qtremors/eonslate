package dev.qtremors.eonslate.feature.finance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
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
        viewModelScope.launch { finance.postDue(today().toEpochDay()) }
    }

    fun today() = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.of(clock.zoneId())).toLocalDate()

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
    fun delete(transaction: Transaction, onDone: () -> Unit) = write(onDone) {
        finance.deleteTransaction(transaction.id)
        true
    }

    fun schedule(expected: RecurringPayment?, payment: RecurringPayment, onDone: () -> Unit) = write(onDone) {
        val day = today().toEpochDay()
        if (expected == null) finance.addRecurring(payment, day)
        else finance.updateRecurring(expected, payment, day)
    }

    fun delete(payment: RecurringPayment, onDone: () -> Unit) = write(onDone) {
        finance.deleteRecurring(payment.id)
        true
    }

    fun record(payment: RecurringPayment, minor: Long, day: Long, onDone: () -> Unit) = write(onDone) {
        finance.recordRecurring(payment, minor, day)
    }

    fun save(budget: CategoryBudget, onDone: () -> Unit) = write(onDone) { finance.saveBudget(budget) }

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

internal data class InboxState(
    val scanning: Boolean = false,
    val messages: List<PaymentMessage>? = null,
    val failed: Boolean = false,
)
