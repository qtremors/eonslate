package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.finance.monthlyMinor
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.RankedBar
import dev.qtremors.eonslate.core.ui.components.RankedBars
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

/** What a row asks of the subscriptions it lists. */
internal class PlanActions(
    val open: (RecurringPayment) -> Unit,
    val edit: (RecurringPayment) -> Unit,
    val setPaused: (RecurringPayment, Boolean) -> Unit,
    val delete: (RecurringPayment) -> Unit,
)

/** Subscriptions and income that match the search and kind filter: paused last, then soonest due. */
internal fun Ledger.plans(query: String, kind: TransactionKind?): List<RecurringPayment> {
    val term = query.trim()
    return recurring.asSequence()
        .filter { kind == null || it.kind == kind }
        .filter { term.isBlank() || listOf(it.note, it.category, it.details).any { text -> text.contains(term, ignoreCase = true) } }
        .sortedWith(compareBy<RecurringPayment>({ it.paused }, { it.nextEpochDay ?: Long.MAX_VALUE }, { it.nextMinute ?: 0 }))
        .toList()
}

/**
 * The Subscriptions page: the kind filter, a month of income, costs and net for each currency,
 * every active item by its monthly amount, then the list itself.
 */
internal fun LazyListScope.planItems(
    ledger: Ledger,
    plans: List<RecurringPayment>,
    kind: TransactionKind?,
    onKind: (TransactionKind?) -> Unit,
    today: LocalDate,
    actions: PlanActions,
) {
    val accounts = ledger.accounts.associateBy { it.id }
    if (ledger.recurring.isNotEmpty()) item(key = "plan-kinds") { PlanKindChips(kind, onKind) }
    val active = plans.filter { !it.paused && it.accountId in accounts }
    // Each currency gets its own totals and chart, so amounts are never added across currencies.
    active.groupBy { accounts.getValue(it.accountId).currency }.forEach { (currency, group) ->
        item(key = "plan-totals-$currency") {
            val locale = LocalValueDisplay.current.numberLocale
            val into = group.filter { it.kind == TransactionKind.INCOME }.sumOf { it.monthlyMinor() }.roundToLong()
            val out = group.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.monthlyMinor() }.roundToLong()
            Totals(
                income = ValueFormat.money(into, currency, locale),
                expenses = ValueFormat.money(out, currency, locale),
                net = ValueFormat.money(into - out, currency, locale),
                title = stringResource(R.string.finance_per_month) + " · " + currency,
            )
        }
        item(key = "plan-chart-$currency") {
            val locale = LocalValueDisplay.current.numberLocale
            val ranked = group.sortedByDescending { it.monthlyMinor() }
            ChartCard(stringResource(R.string.finance_monthly_amounts)) {
                RankedBars(
                    ranked.map {
                        val amount = ValueFormat.money(it.monthlyMinor().roundToLong(), currency, locale)
                        RankedBar(it.note.ifBlank { "?" }, if (it.kind == TransactionKind.INCOME) "+$amount" else amount, it.monthlyMinor().toFloat())
                    },
                    MaterialTheme.colorScheme.primary,
                    onClick = { actions.open(ranked[it]) },
                )
            }
        }
    }
    if (plans.isEmpty()) item(key = "plan-empty") {
        Text(
            stringResource(if (ledger.recurring.isEmpty()) R.string.finance_no_recurring else R.string.finance_no_matches),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
    itemsIndexed(plans, key = { _, payment -> "recurring-" + payment.id }) { index, payment ->
        val account = accounts[payment.accountId] ?: return@itemsIndexed
        ConnectedItem(index, plans.size) { PlanRow(payment, account, today, actions) }
    }
}

@Composable
private fun PlanKindChips(kind: TransactionKind?, onKind: (TransactionKind?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(null to R.string.finance_kind_all_plans, TransactionKind.EXPENSE to R.string.finance_expenses, TransactionKind.INCOME to R.string.finance_incomes)
            .forEach { (option, label) ->
                FilterChip(
                    selected = option == kind,
                    onClick = { onKind(option) },
                    label = { Text(stringResource(label)) },
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
    }
}

/**
 * One schedule: how often and exactly when it is next due, whether its amount varies, and the
 * start of its notes. A long press offers pause or resume, edit and delete.
 */
@Composable
private fun PlanRow(payment: RecurringPayment, account: Account, today: LocalDate, actions: PlanActions) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val name = payment.note.ifBlank { account.name }
    val due = payment.nextEpochDay
    val time = payment.nextOccurrence?.takeIf { payment.nextMinute != null }?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val since = payment.startEpochDay?.let {
        stringResource(R.string.finance_since, LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())))
    }
    val supporting = listOfNotNull(
        if (payment.paused) stringResource(R.string.finance_paused) else intervalLabel(payment.interval, payment.every),
        if (!payment.paused && due != null) {
            listOfNotNull(displayDate(due), time).joinToString(" ") + " (" + dueLabel(due - today.toEpochDay()) + ")"
        } else null,
        when {
            payment.variable && payment.autoEstimate -> stringResource(R.string.finance_variable_estimated)
            payment.variable -> stringResource(R.string.finance_variable_badge)
            else -> null
        },
        if (payment.interval == PaymentInterval.HOURLY) stringResource(R.string.finance_posts_daily) else null,
        since,
        payment.details.lineSequence().firstOrNull { it.isNotBlank() }?.trim(),
    ).joinToString(" · ")
    Box {
        ConnectedRow(
            title = name,
            supporting = supporting,
            leading = { SubscriptionIcon(payment.icon, name, color = payment.color) },
            trailing = {
                Amount(payment.kind, ValueFormat.money(payment.minor, account.currency, LocalValueDisplay.current.numberLocale))
            },
            onClick = { actions.open(payment) },
            onLongClick = { menu = true },
        )
        EonslateMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            groups = listOf(
                listOf(
                    if (payment.paused) MenuAction(stringResource(R.string.finance_resume), Icons.Rounded.PlayArrow) { actions.setPaused(payment, false) }
                    else MenuAction(stringResource(R.string.finance_pause), Icons.Rounded.Pause) { actions.setPaused(payment, true) },
                    MenuAction(stringResource(R.string.finance_edit), Icons.Rounded.Edit) { actions.edit(payment) },
                ),
                listOf(MenuAction(stringResource(R.string.finance_delete), Icons.Rounded.Delete) { confirmDelete = true }),
            ),
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            icon = Icons.Rounded.Delete,
            title = stringResource(R.string.finance_remove_recurring),
            message = stringResource(R.string.finance_remove_recurring_message),
            confirmLabel = stringResource(R.string.finance_delete),
            onConfirm = {
                confirmDelete = false
                actions.delete(payment)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
