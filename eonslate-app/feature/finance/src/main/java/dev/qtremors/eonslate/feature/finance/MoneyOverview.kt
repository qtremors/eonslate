package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.material3.FilledTonalButton
import kotlinx.coroutines.launch
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LoadingIndicator
import dev.qtremors.eonslate.core.ui.components.PressableCard
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.settings.Tracking
import dev.qtremors.eonslate.core.tracker.domain.finance.BudgetHealth
import dev.qtremors.eonslate.core.tracker.domain.finance.MoneySummary
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.BarChart
import dev.qtremors.eonslate.core.ui.components.ChartAxis
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.components.RankedBar
import dev.qtremors.eonslate.core.ui.components.RankedBars
import dev.qtremors.eonslate.core.ui.theme.HeroTextStyle
import dev.qtremors.eonslate.core.ui.theme.LocalSignals
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.ValueTextStyle
import java.math.BigInteger
import java.time.format.TextStyle

/** How many categories get their own bar before the rest fold into "Other". */
private const val TOP_CATEGORIES = 5

private val LOADING_HEIGHT = 260.dp

/**
 * The money half of Home: the balance up front, this month in and out, where it went, budgets,
 * what is due, and the latest transactions. Tapping a card goes to the tab that manages it.
 */
@Composable
fun MoneyOverview(
    onTransactions: () -> Unit,
    onReviewSms: () -> Unit,
    onAdd: (TransactionKind) -> Unit = {},
    onSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = Tracking.deviceCurrency())
    val data = ledger
    LaunchedEffect(data?.accounts?.isEmpty()) { if (data != null) viewModel.ensureAccount() }
    val today = remember { viewModel.today() }
    val summary = remember(data, currency, today) {
        data?.let { MoneySummary.of(it, today, it.accounts.singleOrNull()?.currency ?: currency) }
    }
    // The balance card's space is held while loading, so nothing below it jumps when it arrives.
    if (summary == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = modifier.fillMaxWidth().height(LOADING_HEIGHT)) {
            Box(contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
        return
    }
    val pending by viewModel.detection.pending.collectAsStateWithLifecycle()
    val added by viewModel.detection.added.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (pending.isNotEmpty() || added > 0) {
            SmsBanner(
                found = pending.size,
                added = added,
                onReview = onReviewSms,
                onAddAll = { scope.launch { viewModel.detection.approveAll() } },
                onDismiss = { viewModel.detection.addedShown() },
            )
        }
        MoneyOverviewContent(summary, onTransactions, onSubscriptions, onAdd = onAdd)
    }
}

@Composable
fun MoneyOverviewContent(
    summary: MoneySummary,
    onTransactions: () -> Unit,
    onSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (TransactionKind) -> Unit = {},
) {
    val locale = LocalValueDisplay.current.numberLocale
    val money = { minor: BigInteger -> ValueFormat.money(minor, summary.currency, locale) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Label(stringResource(R.string.overview_balance))
            Text(money(summary.balance), style = HeroTextStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Label(stringResource(R.string.overview_this_month))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Figure(stringResource(R.string.overview_in), "+" + money(summary.monthIncome), Modifier.weight(1f), LocalSignals.current.positive)
                Figure(stringResource(R.string.overview_out), "−" + money(summary.monthExpense), Modifier.weight(1f), MaterialTheme.colorScheme.error)
                Figure(stringResource(R.string.overview_net), money(summary.monthNet), Modifier.weight(1f))
            }
            // Quick add, as the old app had: one tap from Home to the editor.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onAdd(TransactionKind.INCOME) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.overview_add_income))
                }
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onAdd(TransactionKind.EXPENSE) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.overview_add_expense))
                }
            }
        }

        if (summary.trend.any { it.second.signum() > 0 }) Card(onClick = onTransactions) {
            Label(stringResource(R.string.overview_spending))
            val values = summary.trend.map { it.second.toFloat() }
            val peak = summary.trend.maxBy { it.second }
            BarChart(
                values = values,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                highlight = values.lastIndex,
                peakLabel = money(peak.second),
                contentDescription = summary.trend.joinToString { (m, v) -> m.month.getDisplayName(TextStyle.SHORT, locale) + " " + money(v) },
            )
            ChartAxis(
                start = summary.trend.first().first.month.getDisplayName(TextStyle.SHORT, locale),
                end = summary.trend.last().first.month.getDisplayName(TextStyle.SHORT, locale),
            )
        }

        if (summary.categories.isNotEmpty()) Card(onClick = onTransactions) {
            Label(stringResource(R.string.overview_by_category))
            val other = stringResource(R.string.overview_other)
            val none = stringResource(R.string.overview_uncategorized)
            val shown = summary.categories.take(TOP_CATEGORIES)
            val rest = summary.categories.drop(TOP_CATEGORIES).fold(BigInteger.ZERO) { a, c -> a + c.second }
            val bars = shown.map { (name, value) -> RankedBar(name.ifBlank { none }, money(value), value.toFloat()) } +
                (if (rest.signum() > 0) listOf(RankedBar(other, money(rest), rest.toFloat())) else emptyList())
            RankedBars(bars, MaterialTheme.colorScheme.primary)
        }

        if (summary.budgets.isNotEmpty()) Card(onClick = onTransactions) {
            Label(stringResource(R.string.overview_budgets))
            summary.budgets.forEach { BudgetLine(it, money) }
        }

        if (summary.dueSoon.isNotEmpty()) Card(onClick = onSubscriptions) {
            Label(stringResource(R.string.overview_due))
            summary.dueSoon.forEach { payment ->
                DuePaymentLine(payment, displayDate(payment.nextEpochDay!!), money(BigInteger.valueOf(payment.minor)))
            }
        }

        Card(onClick = onTransactions) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.overview_recent), Modifier.weight(1f))
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onTransactions) { Text(stringResource(R.string.overview_all)) }
            }
            if (summary.recent.isEmpty()) {
                Text(stringResource(R.string.finance_empty_activity), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            summary.recent.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.note.ifBlank { t.category.ifBlank { stringResource(R.string.overview_uncategorized) } },
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                        )
                        Text(displayDate(t.epochDay), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Amount(t.kind, money(BigInteger.valueOf(t.minor)))
                }
            }
        }
    }
}

/** Payments found in SMS: review them, add them all, or a note of how many were added. */
@Composable
private fun SmsBanner(found: Int, added: Int, onReview: () -> Unit, onAddAll: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    PressableCard(Modifier.fillMaxWidth(), color = colors.primaryContainer) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Sms, contentDescription = null, tint = colors.onPrimaryContainer)
            Text(
                if (found > 0) pluralStringResource(R.plurals.sms_found, found, found) else pluralStringResource(R.plurals.sms_added, added, added),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onPrimaryContainer,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (found > 0) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onAddAll) { Text(stringResource(R.string.sms_add_all), color = colors.onPrimaryContainer) }
                Button(shapes = ButtonDefaults.shapes(), onClick = onReview) { Text(stringResource(R.string.sms_review)) }
            } else {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.sms_ok), color = colors.onPrimaryContainer) }
            }
        }
    }
}

@Composable
private fun Card(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    PressableCard(Modifier.fillMaxWidth(), onClick = onClick, color = colors.surfaceBright) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = color, maxLines = 1)
    }
}

@Composable
private fun DuePaymentLine(payment: RecurringPayment, detail: String, amount: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(payment.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Amount(payment.kind, amount)
    }
}

@Composable
private fun BudgetLine(health: BudgetHealth, money: (BigInteger) -> String) {
    val colors = MaterialTheme.colorScheme
    val budget = health.budget
    val color: Color = if (health.over) colors.error
        else budget.color?.let { financeBadgeColors(it, budget.title).content } ?: LocalSignals.current.positive
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FinanceBadge(budget.icon, budget.color, budget.title, budget.category, size = 32.dp, fallback = "Wallet")
            Text(budget.title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (health.over) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                Text(stringResource(R.string.finance_over_budget), style = MaterialTheme.typography.labelMedium, color = colors.error)
            } else {
                Text(money(health.spent) + " / " + money(health.limit), style = ValueTextStyle, color = colors.onSurfaceVariant)
            }
        }
        Meter(health.fraction, color, contentDescription = budget.title + " " + money(health.spent) + " / " + money(health.limit))
    }
}
