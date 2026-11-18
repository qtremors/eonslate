package dev.qtremors.eonslate.feature.finance

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
import java.util.Locale

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
    onSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = Tracking.deviceCurrency())
    val data = ledger
    LaunchedEffect(data?.accounts?.isEmpty()) { if (data != null) viewModel.ensureAccount() }
    val today = remember { viewModel.today() }
    val summary = remember(data, currency, today) { data?.let { MoneySummary.of(it, today, currency) } }
    // The balance card's space is held while loading, so nothing below it jumps when it arrives.
    if (summary == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = modifier.fillMaxWidth().height(LOADING_HEIGHT)) {
            Box(contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
        return
    }
    MoneyOverviewContent(summary, onTransactions, onSubscriptions, modifier)
}

@Composable
fun MoneyOverviewContent(summary: MoneySummary, onTransactions: () -> Unit, onSubscriptions: () -> Unit, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val money = { minor: BigInteger -> ValueFormat.money(minor.toLong(), summary.currency, locale) }
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
                Line(payment.note, ValueFormat.date(payment.nextEpochDay!!, locale), money(BigInteger.valueOf(payment.minor)))
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
                        Text(ValueFormat.date(t.epochDay, locale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Amount(t.kind, money(BigInteger.valueOf(t.minor)))
                }
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
private fun Line(title: String, detail: String, amount: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = ValueTextStyle, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BudgetLine(health: BudgetHealth, money: (BigInteger) -> String) {
    val colors = MaterialTheme.colorScheme
    val color: Color = if (health.over) colors.error else LocalSignals.current.positive
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(health.budget.category, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            if (health.over) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                Text(stringResource(R.string.finance_over_budget), style = MaterialTheme.typography.labelMedium, color = colors.error)
            } else {
                Text(money(health.spent) + " / " + money(health.limit), style = ValueTextStyle, color = colors.onSurfaceVariant)
            }
        }
        Meter(health.fraction, color, contentDescription = health.budget.category + " " + money(health.spent) + " / " + money(health.limit))
    }
}
