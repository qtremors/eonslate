package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Compounding
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposit
import dev.qtremors.eonslate.core.tracker.domain.finance.DepositStatus
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposits
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.ProgressRing
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
internal fun Compounding.label(): String = stringResource(
    when (this) {
        Compounding.MONTHLY -> R.string.deposit_compound_monthly
        Compounding.QUARTERLY -> R.string.deposit_compound_quarterly
        Compounding.HALF_YEARLY -> R.string.deposit_compound_half_yearly
        Compounding.YEARLY -> R.string.deposit_compound_yearly
        Compounding.SIMPLE -> R.string.deposit_compound_simple
    },
)

/** "7.1%": the rate as people say it, with no trailing zeros. */
internal fun rateText(rateBp: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(rateBp / 100.0) + "%"

/** A typed rate in percent, "7.1" or "7,25", as hundredths of a percent; null unless it has at most two decimals. */
internal fun parseRate(value: String): Int? {
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
    val text = value.trim().removeSuffix("%").trim().replace(separator, '.')
    if (!Regex("[0-9]{1,2}(\\.[0-9]{0,2})?").matches(text)) return null
    return BigDecimal(text).movePointRight(2).toInt().takeIf { it in 1..Deposit.MAX_RATE_BP }
}

/** "Worth ₹1,03,500 today · ₹1,07,186 on 1 Jan 2027", or for a closed one "Closed 1 Jan 2027". */
@Composable
internal fun depositLine(status: DepositStatus): String {
    val locale = LocalValueDisplay.current.numberLocale
    val currency = status.account.currency
    val closed = status.deposit.closedEpochDay
    if (closed != null) return stringResource(R.string.deposit_closed_on, displayDate(closed))
    return listOf(
        stringResource(R.string.deposit_worth_today, ValueFormat.money(status.worth, currency, locale)),
        stringResource(R.string.deposit_on_day, ValueFormat.money(status.atMaturity, currency, locale), displayDate(status.deposit.maturityEpochDay)),
    ).joinToString(" · ")
}

/** When it matures, said plainly: "Matures in 12 days", "Matures today", "Matured 3 Jan". */
@Composable
internal fun maturityText(status: DepositStatus): String = when {
    status.daysLeft > 0 -> pluralStringResource(R.plurals.deposit_matures_in, status.daysLeft.toInt(), status.daysLeft.toInt())
    status.daysLeft == 0L -> stringResource(R.string.deposit_matures_today)
    else -> stringResource(R.string.deposit_matured_on, displayDate(status.deposit.maturityEpochDay))
}

/** Running deposits with how far through their term they are, then closed ones under their own header. */
@Composable
internal fun DepositSection(statuses: List<DepositStatus>, onOpen: (String) -> Unit) {
    val (running, closed) = statuses.partition { it.open }
    Column {
        DepositRows(running, onOpen)
        if (closed.isNotEmpty()) {
            SectionHeader(stringResource(R.string.deposit_closed))
            DepositRows(closed, onOpen)
        }
    }
}

@Composable
private fun DepositRows(statuses: List<DepositStatus>, onOpen: (String) -> Unit) {
    if (statuses.isEmpty()) return
    ConnectedGroup {
        statuses.forEachIndexed { index, status ->
            ConnectedItem(index, statuses.size) {
                ConnectedRow(
                    title = status.account.name,
                    supporting = depositLine(status),
                    leading = {
                        // A closed deposit ran its course, early or not.
                        val shown = if (status.open) status.fraction else 1f
                        ProgressRing(
                            fraction = shown,
                            color = if (status.open && status.matured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            contentDescription = "${(shown * 100).toInt()}%",
                            size = 40.dp,
                            thickness = 5.dp,
                        ) { Icon(Icons.Rounded.LockClock, contentDescription = null, modifier = Modifier.padding(10.dp)) }
                    },
                    onClick = { onOpen(status.account.id) },
                )
            }
        }
    }
}

/**
 * Makes or changes a fixed deposit: its name, the amount, where the money came from, the rate,
 * the term and how interest compounds. A live line under it says what it grows to.
 */
@Composable
internal fun DepositEditor(
    account: Account?,
    sources: List<Account>,
    defaultCurrency: String,
    busy: Boolean,
    failed: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Account, Account?) -> Unit,
    onDelete: (() -> Unit)?,
    newId: () -> String,
) {
    val terms = account?.deposit
    val locale = LocalValueDisplay.current.numberLocale
    var name by rememberSaveable { mutableStateOf(account?.name.orEmpty()) }
    // Where the money came from; none means it was already there, as for one opened before the app.
    var fromId by rememberSaveable { mutableStateOf(if (account == null) sources.firstOrNull()?.id else null) }
    val from = sources.firstOrNull { it.id == fromId }
    var code by rememberSaveable { mutableStateOf(account?.currency ?: defaultCurrency) }
    val currency = (from?.currency ?: code).trim().uppercase(Locale.ROOT)
    val known = MoneyAmount.isKnownCurrency(currency)
    var amount by rememberSaveable { mutableStateOf(terms?.let { moneyInput(it.principalMinor, account.currency) }.orEmpty()) }
    var rate by rememberSaveable { mutableStateOf(terms?.let { rateText(it.rateBp, Locale.getDefault()).removeSuffix("%") }.orEmpty()) }
    var start by rememberSaveable { mutableStateOf((terms?.startEpochDay?.let(LocalDate::ofEpochDay) ?: today).toString()) }
    var maturity by rememberSaveable {
        mutableStateOf((terms?.maturityEpochDay?.let(LocalDate::ofEpochDay) ?: today.plusYears(1)).toString())
    }
    var compounding by rememberSaveable { mutableStateOf(terms?.compounding ?: Compounding.QUARTERLY) }
    val principal = if (known) parseMoney(amount, currency)?.takeIf { it > 0 } else null
    val rateBp = parseRate(rate)
    val startDay = EpochDays.parse(start)?.takeIf { it in Transaction.DATES }
    val maturityDay = EpochDays.parse(maturity)?.takeIf { it in Transaction.DATES }
    val datesValid = startDay != null && maturityDay != null && maturityDay > startDay
    val deposit = if (principal != null && rateBp != null && datesValid) {
        Deposit(principal, rateBp, startDay!!, maturityDay!!, compounding, terms?.closedEpochDay)
    } else null
    val entered = listOf<Any?>(name, fromId, code, amount, rate, start, maturity, compounding)
    val opened = rememberSaveable { entered.toString() }
    EditorSheet(
        stringResource(if (account == null) R.string.deposit_new else R.string.deposit_edit),
        busy, failed, onDismiss, dirty = entered.toString() != opened,
    ) {
        FormField(
            name, { name = it }, stringResource(R.string.finance_name),
            placeholder = stringResource(R.string.deposit_name_hint), maxLength = Account.NAME_LIMIT, enabled = !busy,
        )
        if (account == null) SourceChoice(from, sources, !busy) { fromId = it?.id }
        if (account == null && from == null) FormField(
            code, { code = it }, stringResource(R.string.finance_currency),
            maxLength = 3, enabled = !busy, capitalization = KeyboardCapitalization.Characters,
        )
        FormField(
            amount, { amount = it }, stringResource(R.string.deposit_amount),
            supporting = if (account != null) stringResource(R.string.deposit_amount_hint) else null,
            keyboardType = KeyboardType.Decimal, suffix = currency.takeIf { known }, maxLength = 24, enabled = !busy,
        )
        FormField(
            rate, { rate = it }, stringResource(R.string.deposit_rate),
            error = if (rate.isNotBlank() && rateBp == null) stringResource(R.string.deposit_rate_invalid) else null,
            keyboardType = KeyboardType.Decimal, suffix = "%", maxLength = 6, enabled = !busy,
        )
        DateField(start, { start = it }, stringResource(R.string.deposit_start), busy)
        // The usual terms in one tap; the date below takes any other.
        val years = startDay?.let { s -> TERMS.firstOrNull { LocalDate.ofEpochDay(s).plusYears(it.toLong()).toEpochDay() == maturityDay } }
        ConnectedChoices(TERMS.map<Int, Int?> { it }, years, { pluralStringResource(R.plurals.deposit_years, it ?: 0, it ?: 0) }, { picked ->
            if (picked != null && startDay != null) maturity = LocalDate.ofEpochDay(startDay).plusYears(picked.toLong()).toString()
        })
        DateField(maturity, { maturity = it }, stringResource(R.string.deposit_maturity), busy)
        if (startDay != null && maturityDay != null && !datesValid) {
            Text(stringResource(R.string.deposit_dates_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        CompoundingChoice(compounding, !busy) { compounding = it }
        deposit?.let {
            Text(
                stringResource(
                    R.string.deposit_grows_to,
                    ValueFormat.money(Deposits.valueOn(it, it.maturityEpochDay), currency, locale),
                    displayDate(it.maturityEpochDay),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SaveRow(
            canSave = !busy && name.isNotBlank() && known && deposit != null,
            onSave = {
                deposit?.let {
                    // Money that was there from the start follows a changed amount; a funding transfer stays as it was.
                    val opening = when {
                        account == null -> 0L
                        account.openingMinor == terms?.principalMinor -> it.principalMinor
                        else -> account.openingMinor
                    }
                    onSave(
                        Account(account?.id ?: newId(), name.trim(), currency, opening, kind = AccountKind.DEPOSIT, deposit = it),
                        from.takeIf { account == null },
                    )
                }
            },
            delete = onDelete?.let { Deletion(stringResource(R.string.deposit_delete, name), R.string.deposit_delete_message, it) },
            busy = busy,
        )
    }
}

/** The account that paid for a new deposit, or none when the money was already in it. */
@Composable
private fun SourceChoice(selected: Account?, sources: List<Account>, enabled: Boolean, onPick: (Account?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.deposit_from_none)
    Box {
        ConnectedRow(
            title = selected?.label ?: none,
            supporting = stringResource(R.string.deposit_from),
            leading = { Icon(selected?.kind?.icon() ?: Icons.Rounded.AccountBalance, contentDescription = null) },
            trailing = { Icon(Icons.Rounded.ExpandMore, null) },
            enabled = enabled,
            onClick = { expanded = true },
        )
        EonslateMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            groups = listOf(
                sources.map {
                    MenuAction(it.label, it.kind.icon(), supporting = it.currency, selected = it.id == selected?.id, onClick = { onPick(it) })
                },
                listOf(MenuAction(none, Icons.Rounded.LockClock, selected = selected == null, onClick = { onPick(null) })),
            ),
        )
    }
}

@Composable
private fun CompoundingChoice(selected: Compounding, enabled: Boolean, onPick: (Compounding) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ConnectedRow(
            title = selected.label(),
            supporting = stringResource(R.string.deposit_compounding),
            leading = { Icon(Icons.Rounded.Percent, contentDescription = null) },
            trailing = { Icon(Icons.Rounded.ExpandMore, null) },
            enabled = enabled,
            onClick = { expanded = true },
        )
        EonslateMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            groups = listOf(Compounding.entries.map { MenuAction(it.label(), Icons.Rounded.Percent, selected = it == selected, onClick = { onPick(it) }) }),
        )
    }
}

/**
 * One deposit: what it is worth today and what it has earned, what it pays at maturity, its
 * terms, and the button that closes it once the money comes back.
 */
@Composable
internal fun DepositSheet(status: DepositStatus, onDismiss: () -> Unit, onClose: () -> Unit, onEdit: () -> Unit) {
    val locale = LocalValueDisplay.current.numberLocale
    val currency = status.account.currency
    val terms = status.deposit
    EditorSheet(status.account.name, busy = false, failed = false, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            val shown = if (status.open) status.fraction else 1f
            ProgressRing(
                fraction = shown,
                color = MaterialTheme.colorScheme.primary,
                contentDescription = "${(shown * 100).toInt()}%",
                size = 96.dp,
                thickness = 11.dp,
            ) { Text("${(shown * 100).toInt()}%", style = MaterialTheme.typography.titleMedium) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ValueFormat.money(status.worth, currency, locale), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.deposit_earned, ValueFormat.money(status.earned, currency, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (status.open) stringResource(R.string.deposit_on_day, ValueFormat.money(status.atMaturity, currency, locale), displayDate(terms.maturityEpochDay))
                    else stringResource(R.string.deposit_closed_on, displayDate(terms.closedEpochDay!!)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status.open) Text(
                    maturityText(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status.matured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ConnectedGroup {
            val rows = listOf(
                stringResource(R.string.deposit_amount) to ValueFormat.money(status.principal, currency, locale),
                stringResource(R.string.deposit_rate) to rateText(terms.rateBp, locale) + " · " + terms.compounding.label(),
                stringResource(R.string.deposit_term) to displayDate(terms.startEpochDay) + " – " + displayDate(terms.maturityEpochDay),
            )
            rows.forEachIndexed { index, (label, value) ->
                ConnectedItem(index, rows.size) { ConnectedRow(title = value, supporting = label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onEdit) { Text(stringResource(R.string.deposit_edit)) }
            // Once it matures, closing is what is left to do; before, it is an early withdrawal.
            if (status.open) {
                if (status.matured) Button(shapes = ButtonDefaults.shapes(), onClick = onClose) { Text(stringResource(R.string.deposit_close)) }
                else FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onClose) { Text(stringResource(R.string.deposit_close_early)) }
            }
        }
    }
}

/**
 * Closes a deposit: what came back, on which day and into which account. The amount starts at
 * what the terms say for that day; the bank's own figure wins.
 */
@Composable
internal fun DepositCloseSheet(
    status: DepositStatus,
    accounts: List<Account>,
    today: LocalDate,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    /** What came back, where to, the day, and the notes for the interest and charges rows. */
    onSave: (Long, Account, Long, String, String) -> Unit,
) {
    val currency = status.account.currency
    val locale = LocalValueDisplay.current.numberLocale
    val terms = status.deposit
    val firstDay = LocalDate.ofEpochDay(minOf(today.toEpochDay(), terms.maturityEpochDay).coerceAtLeast(terms.startEpochDay))
    var date by rememberSaveable { mutableStateOf(firstDay.toString()) }
    val day = EpochDays.parse(date)?.takeIf { it in Transaction.DATES && it >= terms.startEpochDay }
    var typed by rememberSaveable { mutableStateOf<String?>(null) }
    val estimate = day?.let { Deposits.valueOn(terms, it) }
    val amount = typed ?: estimate?.let { runCatching { moneyInput(it.longValueExact(), currency) }.getOrNull() }.orEmpty()
    val received = parseMoney(amount, currency)?.takeIf { it > 0 }
    val choices = accounts.filter { it.currency == currency && it.deposit == null }
    var intoId by rememberSaveable { mutableStateOf(choices.firstOrNull()?.id) }
    val into = choices.firstOrNull { it.id == intoId } ?: choices.firstOrNull()
    val gap = received?.let { java.math.BigInteger.valueOf(it) - status.balance }
    EditorSheet(stringResource(R.string.deposit_close_title, status.account.name), busy, failed, onDismiss, dirty = typed != null) {
        DateField(date, { date = it }, stringResource(R.string.finance_date), busy)
        FormField(
            amount, { typed = it }, stringResource(R.string.deposit_received),
            supporting = gap?.let {
                when (it.signum()) {
                    1 -> stringResource(R.string.deposit_interest_line, ValueFormat.money(it, currency, locale))
                    -1 -> stringResource(R.string.deposit_charges_line, ValueFormat.money(it.negate(), currency, locale))
                    else -> null
                }
            },
            keyboardType = KeyboardType.Decimal, suffix = currency, maxLength = 24, enabled = !busy,
        )
        if (into == null) Text(
            stringResource(R.string.deposit_no_account, currency),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        ) else AccountChoice(stringResource(R.string.deposit_into), into, choices, !busy) { intoId = it.id }
        val interestNote = stringResource(R.string.deposit_interest_note)
        val chargesNote = stringResource(R.string.deposit_charges_note)
        SaveRow(
            canSave = !busy && received != null && day != null && into != null,
            onSave = { if (received != null && day != null && into != null) onSave(received, into, day, interestNote, chargesNote) },
            delete = null,
            busy = busy,
            label = stringResource(R.string.deposit_close),
        )
    }
}

/** Terms offered in one tap, in years. */
private val TERMS = listOf(1, 2, 3, 5)
