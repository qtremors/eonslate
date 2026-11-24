package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.ui.components.SectionHeader
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
import androidx.compose.ui.res.pluralStringResource
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
import dev.qtremors.eonslate.core.ui.components.Treemap
import dev.qtremors.eonslate.core.ui.components.TreemapTile
import dev.qtremors.eonslate.core.ui.components.treemapSplit
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
    val add: () -> Unit = {},
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
    chosenCurrency: String,
    onCurrency: (String) -> Unit,
) {
    val accounts = ledger.accounts.associateBy { it.id }
    val active = plans.filter { !it.paused && it.accountId in accounts }
    // One currency at a time, the same one on every tab; amounts are never added across currencies.
    val currency = ledger.shownCurrency(chosenCurrency)
    val group = active.filter { accounts.getValue(it.accountId).currency == currency }
    if (active.isNotEmpty()) run {
        item(key = "plan-totals") {
            val locale = LocalValueDisplay.current.numberLocale
            val into = group.filter { it.kind == TransactionKind.INCOME }.sumOf { it.monthlyMinor() }.roundToLong()
            val out = group.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.monthlyMinor() }.roundToLong()
            Totals(
                income = ValueFormat.money(into, currency, locale),
                expenses = ValueFormat.money(out, currency, locale),
                net = ValueFormat.money(into - out, currency, locale),
                title = stringResource(R.string.finance_per_month),
                badge = { CurrencyPicker(currency, ledger.currencies(), onCurrency) },
            )
        }
        // Costs and income each get their own map, sized within themselves, so a salary never
        // shrinks the subscriptions to slivers. Tiles wear each item's own tile colours.
        listOf(TransactionKind.EXPENSE, TransactionKind.INCOME).forEach { kind ->
            val ranked = group.filter { it.kind == kind }.sortedByDescending { it.monthlyMinor() }
            if (ranked.isNotEmpty()) item(key = "plan-chart-$kind") {
                val locale = LocalValueDisplay.current.numberLocale
                ChartCard(stringResource(if (kind == TransactionKind.INCOME) R.string.finance_income_per_month else R.string.finance_costs_per_month)) {
                    // The largest get their own tile; the many small ones share one, never a scatter of specks.
                    val kept = treemapSplit(ranked.map { it.monthlyMinor() })
                    val rest = ranked.drop(kept)
                    val tiles = ranked.take(kept).map {
                        val amount = ValueFormat.money(it.monthlyMinor().roundToLong(), currency, locale)
                        val name = it.note.ifBlank { "?" }
                        val colors = subscriptionColors(it.icon, name, it.color)
                        TreemapTile(name, amount, it.monthlyMinor().toFloat(), colors.container, colors.content)
                    } + if (rest.isEmpty()) emptyList() else {
                        val sum = rest.sumOf { it.monthlyMinor() }
                        listOf(
                            TreemapTile(
                                pluralStringResource(R.plurals.finance_more_plans, rest.size, rest.size),
                                ValueFormat.money(sum.roundToLong(), currency, locale), sum.toFloat(),
                                androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant,
                                clickable = false,
                            ),
                        )
                    }
                    Treemap(
                        tiles = tiles,
                        contentDescription = ranked.joinToString { it.note + " " + ValueFormat.money(it.monthlyMinor().roundToLong(), currency, locale) },
                        height = when (tiles.size) {
                            1 -> 88.dp
                            2, 3 -> 140.dp
                            else -> 220.dp
                        },
                        onClick = { actions.open(ranked[it]) },
                    )
                }
            }
        }
    }
    if (plans.isEmpty()) item(key = "plan-empty") {
        EmptyWithAction(
            stringResource(if (ledger.recurring.isEmpty()) R.string.finance_no_recurring else R.string.finance_no_matches),
            stringResource(R.string.finance_new_recurring),
            actions.add,
        )
    }
    // What goes out, then what comes in, each in its own group.
    listOf(TransactionKind.EXPENSE, TransactionKind.INCOME).forEach { side ->
        val group = plans.filter { it.kind == side && it.accountId in accounts }
        if (group.isEmpty()) return@forEach
        item(key = "plans-$side") {
            SectionHeader(stringResource(if (side == TransactionKind.INCOME) R.string.recurring_in else R.string.recurring_out))
        }
        itemsIndexed(group, key = { _, payment -> "recurring-" + payment.id }) { index, payment ->
            ConnectedItem(index, group.size) { PlanRow(payment, accounts.getValue(payment.accountId), today, actions) }
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
    val supporting = listOfNotNull(
        if (payment.paused) stringResource(R.string.finance_paused) else intervalLabel(payment.interval, payment.every),
        if (!payment.paused && due != null) {
            val date = listOfNotNull(displayDate(due), time).joinToString(" ")
            val payout = if (payment.kind == TransactionKind.INCOME) stringResource(R.string.finance_payout, date) else date
            payout + " (" + dueLabel(due - today.toEpochDay()).replaceFirstChar { it.lowercase(Locale.getDefault()) } + ")"
        } else null,
        when {
            payment.variable && payment.autoEstimate -> stringResource(R.string.finance_variable_estimated)
            payment.variable -> stringResource(R.string.finance_variable_badge)
            else -> null
        },
        if (payment.interval == PaymentInterval.HOURLY) stringResource(R.string.finance_posts_daily) else null,
        payment.priceChange?.let {
            stringResource(R.string.recurring_price_coming, ValueFormat.money(it.minor, account.currency, LocalValueDisplay.current.numberLocale), displayDate(it.fromEpochDay))
        },
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
