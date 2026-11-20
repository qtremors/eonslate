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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.qtremors.eonslate.core.tracker.domain.finance.CashFlow
import dev.qtremors.eonslate.core.tracker.domain.finance.CashFlowBucket
import dev.qtremors.eonslate.core.tracker.domain.finance.CashFlowRange
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.theme.LocalReduceMotion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.math.BigInteger

/** Categories shown before "Show all". */
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
    onAddBudget: () -> Unit = {},
    /** Opens Transactions on this month in one category. */
    onCategory: (String) -> Unit = { onTransactions() },
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FoundPaymentsBanner(viewModel.detection, onReviewSms)
        MoneyOverviewContent(summary, today, onTransactions, onSubscriptions, onAdd = onAdd, onAddBudget = onAddBudget, onCategory = onCategory)
    }
}

@Composable
fun MoneyOverviewContent(
    summary: MoneySummary,
    today: LocalDate,
    onTransactions: () -> Unit,
    onSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (TransactionKind) -> Unit = {},
    onAddBudget: () -> Unit = {},
    onCategory: (String) -> Unit = { onTransactions() },
) {
    val locale = LocalValueDisplay.current.numberLocale
    val money = { minor: BigInteger -> ValueFormat.money(minor, summary.currency, locale) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(stringResource(R.string.overview_balance))
                CurrencyBadge(summary.currency)
            }
            AnimatedMoney(summary.balance, money)
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

        if (summary.entries.isNotEmpty()) Card { CashFlowContent(summary, today, money) }

        // Every category this month, none folded away; a tap opens its transactions.
        if (summary.categories.isNotEmpty()) Card(onClick = onTransactions) {
            var all by rememberSaveable { mutableStateOf(false) }
            Label(stringResource(R.string.overview_by_category))
            val none = stringResource(R.string.overview_uncategorized)
            val total = summary.monthExpense
            val shown = if (all) summary.categories else summary.categories.take(TOP_CATEGORIES)
            RankedBars(
                shown.map { (name, value) ->
                    val percent = if (total.signum() == 0) 0 else value.multiply(BigInteger.valueOf(100)).divide(total).toInt()
                    RankedBar(name.ifBlank { none }, money(value) + " · " + percent + "%", value.toFloat())
                },
                MaterialTheme.colorScheme.primary,
                onClick = { index -> shown[index].first.takeIf { it.isNotBlank() }?.let(onCategory) ?: onTransactions() },
            )
            if (summary.categories.size > TOP_CATEGORIES) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { all = !all }) {
                    Text(if (all) stringResource(R.string.finance_show_fewer) else stringResource(R.string.finance_show_all, summary.categories.size))
                }
            }
        }

        // Budgets always show: their meters, or a way to set the first one.
        val hasBudgets = summary.budgets.isNotEmpty()
        Card(onClick = if (hasBudgets) onTransactions else onAddBudget) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.overview_budgets), Modifier.weight(1f))
                TextButton(shapes = ButtonDefaults.shapes(), onClick = if (hasBudgets) onTransactions else onAddBudget) {
                    Text(stringResource(if (hasBudgets) R.string.overview_manage_budgets else R.string.overview_add_budget))
                }
            }
            if (!hasBudgets) {
                Text(stringResource(R.string.overview_no_budgets), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                        val title = t.note.ifBlank { t.category.ifBlank { stringResource(R.string.overview_uncategorized) } }
                        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        // The category under the name, then the day.
                        Text(
                            listOfNotNull(t.category.trim().takeIf { it.isNotEmpty() && it != title }, displayDate(t.epochDay)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                    }
                    Amount(t.kind, money(BigInteger.valueOf(t.minor)))
                }
            }
        }
    }
}

/**
 * Payments found in SMS, on Home and Transactions: review them or add them all. After a batch it
 * says what was added and what was left out, until dismissed.
 */
@Composable
internal fun FoundPaymentsBanner(detection: SmsDetection, onReview: () -> Unit, modifier: Modifier = Modifier) {
    val pending by detection.pending.collectAsStateWithLifecycle()
    val result by detection.result.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val found = pending.size
    val outcome = result
    if (found == 0 && outcome == null) return
    val colors = MaterialTheme.colorScheme
    PressableCard(modifier.fillMaxWidth(), color = colors.primaryContainer) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Sms, contentDescription = null, tint = colors.onPrimaryContainer)
            Text(
                if (found > 0) pluralStringResource(R.plurals.sms_found, found, found) else batchText(outcome!!),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onPrimaryContainer,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (found > 0) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { scope.launch { detection.approveAll() } }) {
                    Text(stringResource(R.string.sms_add_all), color = colors.onPrimaryContainer)
                }
                Button(shapes = ButtonDefaults.shapes(), onClick = onReview) { Text(stringResource(R.string.sms_review)) }
            } else {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = detection::resultShown) {
                    Text(stringResource(R.string.sms_ok), color = colors.onPrimaryContainer)
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
    val signals = LocalSignals.current
    val budget = health.budget
    // Red past the limit, amber from 80%, otherwise the budget color.
    val color: Color = when {
        health.over -> colors.error
        health.warning -> signals.warning
        else -> budget?.color?.let { financeBadgeColors(it, health.title).content } ?: signals.positive
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FinanceBadge(budget?.icon, budget?.color, health.title, budget?.category.orEmpty(), size = 32.dp, fallback = "Wallet")
            Text(health.title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (health.over || health.warning) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = color, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                Text(
                    if (health.over) stringResource(R.string.finance_over_budget) + " · " + health.percent + "%"
                    else stringResource(R.string.overview_budget_used, health.percent),
                    style = MaterialTheme.typography.labelMedium, color = color,
                )
            } else {
                Text(money(health.spent) + " / " + money(health.limit), style = ValueTextStyle, color = colors.onSurfaceVariant)
            }
        }
        Meter(health.fraction, color, contentDescription = health.title + " " + money(health.spent) + " / " + money(health.limit))
    }
}

/** The currency every amount on Home is in, as a small tag beside the balance. */
@Composable
private fun CurrencyBadge(code: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
        Text(
            code,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** The balance counts up to its amount when Home opens, unless motion is reduced; it always ends exact. */
@Composable
private fun AnimatedMoney(amount: BigInteger, money: (BigInteger) -> String) {
    val still = LocalReduceMotion.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(amount, still) {
        if (still) progress.snapTo(1f) else progress.animateTo(1f, tween(700))
    }
    val shown = if (progress.value >= 1f) amount
    else amount.toBigDecimal().multiply(java.math.BigDecimal(progress.value.toDouble())).toBigInteger()
    Text(money(shown), style = HeroTextStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
}

/**
 * Income and spending over a chosen range: its totals, a bar per day, month or year, and the
 * picked bar with its spending, income and top category.
 */
@Composable
private fun CashFlowContent(summary: MoneySummary, today: LocalDate, money: (BigInteger) -> String) {
    var range by rememberSaveable { mutableStateOf(CashFlowRange.HALF_YEAR) }
    var picked by rememberSaveable(range) { mutableStateOf<Int?>(null) }
    val flow = remember(summary, range, today) { summary.cashFlow(range, today) }
    val locale = LocalValueDisplay.current.numberLocale
    val label = { bucket: CashFlowBucket ->
        val start = LocalDate.ofEpochDay(bucket.days.first)
        when (flow.step) {
            CashFlow.Step.DAY -> start.format(DateTimeFormatter.ofPattern("d MMM", locale))
            CashFlow.Step.MONTH -> start.format(DateTimeFormatter.ofPattern(if (start.year == today.year) "MMM" else "MMM yyyy", locale))
            CashFlow.Step.YEAR -> start.year.toString()
        }
    }
    Label(stringResource(R.string.overview_cash_flow))
    ConnectedChoices(CashFlowRange.entries, range, { it.label() }, { range = it })
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Figure(stringResource(R.string.overview_in), "+" + money(flow.income), Modifier.weight(1f), LocalSignals.current.positive)
        Figure(stringResource(R.string.overview_out), "−" + money(flow.spending), Modifier.weight(1f), MaterialTheme.colorScheme.error)
        Figure(stringResource(R.string.overview_net), money(flow.net), Modifier.weight(1f))
    }
    val values = flow.buckets.map { it.spending.toFloat() }
    val spent = values.any { it > 0f }
    if (spent) {
        BarChart(
            values = values,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            selected = picked,
            onSelect = { picked = it },
            highlight = values.lastIndex,
            peakLabel = money(flow.buckets.maxBy { it.spending }.spending),
            contentDescription = flow.buckets.joinToString { label(it) + " " + money(it.spending) },
        )
        ChartAxis(start = label(flow.buckets.first()), end = label(flow.buckets.last()))
    }
    val bucket = picked?.let(flow.buckets::getOrNull)
    if (bucket == null) {
        if (spent) Text(stringResource(R.string.overview_tap_bar), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text(label(bucket), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        val uncategorized = stringResource(R.string.overview_uncategorized)
        Text(
            listOfNotNull(
                stringResource(R.string.overview_spent, money(bucket.spending)),
                stringResource(R.string.overview_earned, money(bucket.income)),
                bucket.topCategory?.let { stringResource(R.string.overview_top, it.ifBlank { uncategorized }, money(bucket.topAmount)) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CashFlowRange.label() = stringResource(
    when (this) {
        CashFlowRange.WEEK -> R.string.overview_range_week
        CashFlowRange.MONTH -> R.string.overview_range_month
        CashFlowRange.HALF_YEAR -> R.string.overview_range_half
        CashFlowRange.YEAR -> R.string.overview_range_year
        CashFlowRange.ALL -> R.string.overview_range_all
    }
)
