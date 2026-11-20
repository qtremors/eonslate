package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.BudgetHealth
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.FitText
import dev.qtremors.eonslate.core.ui.components.ChartPalette
import dev.qtremors.eonslate.core.ui.components.ChartSlice
import dev.qtremors.eonslate.core.ui.components.LegendRow
import dev.qtremors.eonslate.core.ui.components.ProgressRing
import dev.qtremors.eonslate.core.ui.components.SegmentedBar
import dev.qtremors.eonslate.core.ui.theme.LocalSignals
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import java.math.BigInteger
import java.util.Locale

/** Parts shown on their own before the rest fold into "Other". */
internal const val CHART_PARTS = 5

/**
 * The chart hue of each spending category, by its key. The eight with the most spending of all
 * time get the eight hues, in list order; the rest share the "Other" gray, never a made-up ninth
 * hue. Ranked over all time, not the range shown, so a category keeps its hue whatever is filtered,
 * and the biggest spenders are never the gray ones.
 */
internal fun chartSlots(ledger: Ledger): Map<String, Int> {
    val spent = HashMap<String, Double>()
    for (t in ledger.transactions) {
        if (t.kind == TransactionKind.EXPENSE) spent.merge(t.category.trim().lowercase(Locale.ROOT), t.minor.toDouble(), Double::plus)
    }
    val categories = Categories.all(ledger).filter { it.kind.fits(TransactionKind.EXPENSE) }
    val top = categories.sortedByDescending { spent[it.key] ?: 0.0 }.take(ChartPalette.SIZE).map { it.key }.toSet()
    return categories.filter { it.key in top }.mapIndexed { index, category -> category.key to index }.toMap()
}

@Composable
internal fun categoryColor(name: String, slots: Map<String, Int>): Color =
    slots[name.trim().lowercase(Locale.ROOT)]?.let { ChartPalette.series(it) } ?: ChartPalette.other()

/** The largest [CHART_PARTS] categories as slices, and the rest as one "Other". */
@Composable
internal fun categorySlices(
    entries: List<Pair<String, BigInteger>>,
    slots: Map<String, Int>,
    money: (BigInteger) -> String,
): List<ChartSlice> {
    val none = stringResource(R.string.overview_uncategorized)
    val folded = (entries.count { it.second.signum() > 0 } - CHART_PARTS).coerceAtLeast(0)
    val other = pluralStringResource(R.plurals.overview_more, folded, folded)
    val sorted = entries.filter { it.second.signum() > 0 }.sortedByDescending { it.second }
    val rest = sorted.drop(CHART_PARTS).fold(BigInteger.ZERO) { sum, it -> sum + it.second }
    return sorted.take(CHART_PARTS).map { (name, value) ->
        ChartSlice(name.ifBlank { none }, money(value), value.toFloat(), categoryColor(name, slots))
    } + if (rest.signum() > 0) listOf(ChartSlice(other, money(rest), rest.toFloat(), ChartPalette.other())) else emptyList()
}

/** A budget's use as shown: whole percent, with anything past 999% as ">999%" rather than a long number. */
internal fun budgetPercent(percent: Int): String = if (percent > 999) ">999%" else "$percent%"

internal fun share(part: BigInteger, whole: BigInteger): String {
    if (whole.signum() == 0) return "0%"
    val percent = part.toBigDecimal().multiply(java.math.BigDecimal(100)).divide(whole.toBigDecimal(), 0, java.math.RoundingMode.HALF_UP)
    return if (percent.signum() == 0 && part.signum() > 0) "<1%" else "$percent%"
}

/**
 * Where the shown spending went, for comparing parts exactly: one bar split into the largest
 * categories, then a row per category with its colour, amount and share. A row picks its
 * category, as a filter.
 */
@Composable
internal fun CategoryBreakdown(
    entries: List<Pair<String, BigInteger>>,
    slots: Map<String, Int>,
    money: (BigInteger) -> String,
    picked: String?,
    onPick: (String) -> Unit,
) {
    var all by rememberSaveable { mutableStateOf(false) }
    val sorted = entries.filter { it.second.signum() > 0 }.sortedByDescending { it.second }
    val whole = sorted.fold(BigInteger.ZERO) { sum, it -> sum + it.second }
    val none = stringResource(R.string.overview_uncategorized)
    val slices = categorySlices(sorted, slots, money)
    SegmentedBar(slices, contentDescription = slices.joinToString { it.label + " " + it.valueText })
    Column(Modifier.padding(top = 4.dp)) {
        (if (all) sorted else sorted.take(CHART_PARTS)).forEach { (name, value) ->
            LegendRow(
                color = categoryColor(name, slots),
                label = name.ifBlank { none },
                valueText = money(value) + " · " + share(value, whole),
                emphasized = picked != null && name.equals(picked, ignoreCase = true),
                onClick = name.takeIf { it.isNotBlank() }?.let { { onPick(it) } },
            )
        }
    }
    if (sorted.size > CHART_PARTS) {
        TextButton(shapes = ButtonDefaults.shapes(), onClick = { all = !all }) {
            Text(if (all) stringResource(R.string.finance_show_fewer) else stringResource(R.string.finance_show_all, sorted.size))
        }
    }
}

/**
 * Every budget in one currency together: a ring for the share of the total limit spent, beside
 * the total limit, the total spent and what is left or over.
 */
@Composable
internal fun BudgetTotals(group: List<BudgetHealth>, currency: String) {
    val colors = MaterialTheme.colorScheme
    val signals = LocalSignals.current
    val locale = LocalValueDisplay.current.numberLocale
    val limit = group.fold(BigInteger.ZERO) { sum, it -> sum + it.limit }
    val spent = group.fold(BigInteger.ZERO) { sum, it -> sum + it.spent }
    val total = BudgetHealth("", currency, spent, limit)
    val color = when {
        total.over -> colors.error
        total.warning -> signals.warning
        else -> colors.primary
    }
    val money = { minor: BigInteger -> ValueFormat.money(minor, currency, locale) }
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ProgressRing(
                fraction = total.fraction,
                color = color,
                contentDescription = stringResource(R.string.finance_budget_total_used, budgetPercent(total.percent)),
            ) {
                Text(budgetPercent(total.percent), style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = colors.onSurface, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Figure(stringResource(R.string.finance_budget_total_limit), money(limit), colors.onSurface)
                Figure(stringResource(R.string.finance_budget_total_spent), money(spent), colors.onSurface)
                if (total.over) Figure(stringResource(R.string.finance_budget_total_over), money(total.remaining.negate()), colors.error)
                else Figure(stringResource(R.string.finance_budget_total_left), money(total.remaining), colors.onSurface)
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FitText(value, style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = color)
    }
}
