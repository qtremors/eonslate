package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import java.util.Locale
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import java.time.LocalDate

@Composable
internal fun RecurringEditor(
    existing: RecurringPayment?,
    accounts: List<Account>,
    today: LocalDate,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (RecurringPayment) -> Unit,
    onDelete: (() -> Unit)?,
    onRecord: (() -> Unit)?,
) {
    var accountId by rememberSaveable { mutableStateOf(existing?.accountId ?: accounts.first().id) }
    val account = accounts.firstOrNull { it.id == accountId } ?: accounts.first()
    var amount by rememberSaveable { mutableStateOf(existing?.let { moneyInput(it.minor, account.currency) }.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category.orEmpty()) }
    var interval by rememberSaveable { mutableStateOf(existing?.interval ?: PaymentInterval.MONTHLY) }
    var every by rememberSaveable { mutableStateOf((existing?.every ?: 1).toString()) }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: TransactionKind.EXPENSE) }
    var paused by rememberSaveable { mutableStateOf(existing?.paused ?: false) }
    var variable by rememberSaveable { mutableStateOf(existing?.variable ?: false) }
    var date by rememberSaveable { mutableStateOf(existing?.nextEpochDay?.let(EpochDays::format) ?: today.toString()) }
    val parsed = parseMoney(amount, account.currency)?.takeIf { it > 0 }
    val day = parseDay(date)
    val repeat = parseInterval(every)
    val title = stringResource(if (existing == null) R.string.finance_new_recurring else R.string.finance_edit_recurring)

    EditorSheet(title, busy, failed, onDismiss) {
        ConnectedChoices(listOf(TransactionKind.EXPENSE, TransactionKind.INCOME), kind, { it.label() }, { if (!busy) kind = it })
        AccountChoice(stringResource(R.string.finance_account), account, accounts, !busy) {
            if (it.currency != account.currency) amount = ""
            accountId = it.id
        }
        AmountField(amount, { amount = it }, account.currency, busy)
        FormField(note, { note = it }, stringResource(R.string.finance_payment_name), maxLength = Transaction.NOTE_LIMIT, enabled = !busy)
        CategoryPicker(category, { category = it }, kind, busy)
        PeriodFields(interval, { interval = it }, every, { every = it }, busy)
        DateField(
            date, { date = it },
            stringResource(if (existing == null) R.string.finance_first_due else R.string.finance_next_due),
            busy,
        )
        ToggleRow(stringResource(R.string.finance_variable), variable, { variable = it }, enabled = !busy)
        if (existing != null) ToggleRow(stringResource(R.string.finance_paused), paused, { paused = it }, enabled = !busy)
        if (onRecord != null) {
            TextButton(shapes = ButtonDefaults.shapes(), enabled = !busy, onClick = onRecord) {
                Text(stringResource(R.string.finance_record_due))
            }
        }
        SaveRow(
            canSave = !busy && parsed != null && day != null && repeat != null,
            onSave = {
                if (parsed != null && day != null && repeat != null) {
                    val anchor = existing?.takeIf { it.nextEpochDay == day }?.anchorDay ?: LocalDate.ofEpochDay(day).dayOfMonth
                    onSave(RecurringPayment("", account.id, parsed, note.trim(), interval, anchor, day, kind, repeat, paused, variable, category.trim()))
                }
            },
            delete = onDelete?.let {
                Deletion(stringResource(R.string.finance_remove_recurring), R.string.finance_remove_recurring_message, it)
            },
            busy = busy,
        )
    }
}

@Composable
internal fun CategoryBudgetEditor(
    existing: CategoryBudget?,
    accounts: List<Account>,
    today: LocalDate,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (CategoryBudget) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var accountId by rememberSaveable { mutableStateOf(existing?.accountId ?: accounts.first().id) }
    val account = accounts.firstOrNull { it.id == accountId } ?: accounts.first()
    var amount by rememberSaveable { mutableStateOf(existing?.let { moneyInput(it.minor, account.currency) }.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category.orEmpty()) }
    var interval by rememberSaveable { mutableStateOf(existing?.interval ?: PaymentInterval.MONTHLY) }
    var every by rememberSaveable { mutableStateOf((existing?.every ?: 1).toString()) }
    var date by rememberSaveable { mutableStateOf(existing?.startEpochDay?.let(EpochDays::format) ?: today.withDayOfMonth(1).toString()) }
    val parsed = parseMoney(amount, account.currency)?.takeIf { it > 0 }
    val day = parseDay(date)
    val repeat = parseInterval(every)
    val title = stringResource(if (existing == null) R.string.finance_new_budget else R.string.finance_edit_budget)
    EditorSheet(title, busy, failed, onDismiss) {
        AccountChoice(stringResource(R.string.finance_account), account, accounts, !busy) {
            if (it.currency != account.currency) amount = ""
            accountId = it.id
        }
        CategoryPicker(category, { category = it }, TransactionKind.EXPENSE, busy)
        AmountField(amount, { amount = it }, account.currency, busy)
        PeriodFields(interval, { interval = it }, every, { every = it }, busy)
        DateField(date, { date = it }, stringResource(R.string.finance_budget_start), busy)
        SaveRow(
            canSave = !busy && parsed != null && day != null && repeat != null && category.isNotBlank(),
            onSave = {
                if (parsed != null && day != null && repeat != null) {
                    onSave(CategoryBudget("", account.id, category.trim(), parsed, interval, repeat, day))
                }
            },
            delete = onDelete?.let {
                Deletion(stringResource(R.string.finance_remove_budget), R.string.finance_remove_budget_message, it)
            },
            busy = busy,
        )
    }
}

@Composable
internal fun RecordPaymentEditor(
    payment: RecurringPayment,
    account: Account,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onRecord: (Long, Long) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf(moneyInput(payment.minor, account.currency)) }
    var date by rememberSaveable { mutableStateOf(EpochDays.format(checkNotNull(payment.nextEpochDay))) }
    val parsed = parseMoney(amount, account.currency)?.takeIf { it > 0 }
    val day = parseDay(date)
    EditorSheet(stringResource(R.string.finance_record_due), busy, failed, onDismiss) {
        AmountField(amount, { amount = it }, account.currency, busy)
        DateField(date, { date = it }, stringResource(R.string.finance_date), busy)
        SaveRow(
            canSave = !busy && parsed != null && day != null,
            onSave = { if (parsed != null && day != null) onRecord(parsed, day) },
            delete = null,
            busy = busy,
        )
    }
}

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit, currency: String, busy: Boolean) {
    FormField(
        value, onChange, stringResource(R.string.finance_amount), suffix = currency,
        keyboardType = KeyboardType.Decimal, maxLength = 24, enabled = !busy,
    )
}


/** A date picked from a calendar; [value] stays an ISO date, as the editors store it. */
@Composable
internal fun DateField(value: String, onChange: (String) -> Unit, label: String, busy: Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    val day = EpochDays.parse(value)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(shapes = ButtonDefaults.shapes(), enabled = !busy, onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(day?.let { ValueFormat.date(it, Locale.getDefault()) } ?: value)
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day?.times(DAY_MILLIS))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS)).toString()) }
                    open = false
                }) { Text(stringResource(R.string.finance_save)) }
            },
        ) { DatePicker(state = state) }
    }
}

private const val DAY_MILLIS = 86_400_000L

@Composable
private fun PeriodFields(interval: PaymentInterval, onInterval: (PaymentInterval) -> Unit, count: String, onCount: (String) -> Unit, busy: Boolean) {
    ConnectedChoices(PaymentInterval.entries, interval, { it.label() }, { if (!busy) onInterval(it) })
    FormField(
        count, onCount, stringResource(R.string.finance_interval_count), keyboardType = KeyboardType.Number,
        suffix = stringResource(
            when (interval) {
                PaymentInterval.DAILY -> R.string.finance_days
                PaymentInterval.WEEKLY -> R.string.finance_weeks
                PaymentInterval.MONTHLY -> R.string.finance_months
                PaymentInterval.YEARLY -> R.string.finance_years
            }
        ),
        maxLength = 3, enabled = !busy,
    )
}

private fun parseDay(date: String) = EpochDays.parse(date)?.takeIf { it in Transaction.DATES }

private fun parseInterval(count: String) = count.toIntOrNull()?.takeIf { it in 1..RecurringPayment.MAX_INTERVAL }
