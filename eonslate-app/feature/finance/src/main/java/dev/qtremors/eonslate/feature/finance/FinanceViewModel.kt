package dev.qtremors.eonslate.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val finance: FinanceRepository,
    settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: AppClock,
) : ViewModel() {
    val ledger: StateFlow<Ledger?> = finance.observeLedger()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currency = settings.tracking.map { it.currencyCode() }

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()

    init {
        viewModelScope.launch { finance.postDue(today().toEpochDay()) }
    }

    fun today() = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.of(clock.zoneId())).toLocalDate()

    fun newId() = ids.newId()

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

    fun schedule(payment: RecurringPayment, onDone: () -> Unit) = write(onDone) {
        finance.addRecurring(payment, today().toEpochDay())
    }

    fun delete(payment: RecurringPayment, onDone: () -> Unit) = write(onDone) {
        finance.deleteRecurring(payment.id)
        true
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
