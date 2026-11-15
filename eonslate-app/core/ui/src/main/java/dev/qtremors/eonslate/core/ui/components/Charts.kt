package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape

/*
 * Eonslate's charts follow one set of rules: one series per chart in the tracker's pigment (so no
 * legend; the card title names it), thin marks with 4 dp rounded data ends anchored to the
 * baseline, 2 dp gaps between bars, a 2 dp line with 8 dp markers ringed in the surface color, a
 * hairline baseline and nothing else of a grid. A tap picks a mark to read it, and every chart is
 * paired with a table view and a spoken summary, so no value is reachable by color alone.
 */

private val BarGap = 2.dp
/** Bars never grow into blocks: with few columns they stay this wide, centered in their column. */
private val MaxBar = 28.dp
private val DataEnd = 4.dp
private val LineWidth = 2.dp
private val Marker = 8.dp
private val Ring = 2.dp

/** Which bar or point a tap at [x] lands on, for [count] equal columns across [width]. */
fun chartIndexAt(x: Float, width: Float, count: Int): Int? =
    if (count <= 0 || width <= 0f || x < 0f || x > width) null
    else (x / (width / count)).toInt().coerceIn(0, count - 1)

/** The range a chart's value axis spans: always including zero, never empty. */
fun chartRange(values: List<Float>): ClosedFloatingPointRange<Float> {
    val low = minOf(0f, values.minOrNull() ?: 0f)
    val high = maxOf(0f, values.maxOrNull() ?: 0f)
    return if (high - low == 0f) low..(low + 1f) else low..high
}

/**
 * Bars for one value per column, such as entries per day. Negative values hang below a zero
 * baseline. [selected] is drawn full strength and the rest recede; a tap reports the column.
 */
@Composable
fun BarChart(
    values: List<Float>,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
    highlight: Int? = null,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    height: Dp = 140.dp,
    /** Written over the tallest bar, such as "3"; the one direct label a bar chart carries. */
    peakLabel: String? = null,
) {
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    // The tap handler lives as long as the column count; read the latest pick through these.
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(values.size) {
                detectTapGestures { offset ->
                    val index = chartIndexAt(offset.x, size.width.toFloat(), values.size)
                    currentOnSelect(if (index == currentSelected) null else index)
                }
            }
    ) {
        if (values.isEmpty()) return@Canvas
        val range = chartRange(values)
        val span = range.endInclusive - range.start
        // Headroom above the tallest bar for its label, so the label never sits on the bar.
        val label = peakLabel?.let { measurer.measure(it, labelStyle) }
        val plotTop = label?.let { it.size.height + 4.dp.toPx() } ?: 0f
        val plotHeight = size.height - plotTop
        val zeroY = plotTop + plotHeight * (range.endInclusive / span)
        val column = size.width / values.size
        val gap = BarGap.toPx().coerceAtMost(column / 3f)
        val radius = DataEnd.toPx().coerceAtMost((column - gap) / 2f)

        values.forEachIndexed { i, value ->
            if (value == 0f) return@forEachIndexed
            val width = minOf(column - gap, MaxBar.toPx())
            val left = i * column + (column - width) / 2f
            val right = left + width
            val valueY = plotTop + plotHeight * ((range.endInclusive - value) / span)
            val top = minOf(valueY, zeroY)
            val bottom = maxOf(valueY, zeroY)
            val base = when (i) {
                highlight -> highlightColor
                else -> color
            }
            val fill = if (selected != null && selected != i) base.copy(alpha = 0.4f) else base
            drawBar(left, top, right, bottom, radius, roundTop = value > 0f, color = fill)
        }
        drawLine(baseline, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.dp.toPx())

        val peak = values.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0f }
        if (label != null && peak != null) {
            val text = label
            val centerX = peak.index * column + column / 2f
            val top = plotTop + plotHeight * ((range.endInclusive - peak.value) / span) - text.size.height - 2.dp.toPx()
            drawText(
                text,
                topLeft = Offset(
                    (centerX - text.size.width / 2f).coerceIn(0f, size.width - text.size.width),
                    top.coerceAtLeast(0f),
                ),
            )
        }
    }
}

/** A bar rounded only at its data end, square where it meets the baseline. */
private fun DrawScope.drawBar(left: Float, top: Float, right: Float, bottom: Float, radius: Float, roundTop: Boolean, color: Color) {
    val r = CornerRadius(radius, radius)
    val rect = if (roundTop) {
        RoundRect(left, top, right, bottom, topLeftCornerRadius = r, topRightCornerRadius = r)
    } else {
        RoundRect(left, top, right, bottom, bottomLeftCornerRadius = r, bottomRightCornerRadius = r)
    }
    drawPath(Path().apply { addRoundRect(rect) }, color)
}

/**
 * A line through one value per column, such as a day's weight. Columns without a value are
 * skipped and the line joins the values either side, since a missed day is not a zero.
 */
@Composable
fun LineChart(
    values: List<Float?>,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
    height: Dp = 140.dp,
    /** Written beside the latest point, such as "72.5 kg"; the one direct label a line carries. */
    endLabel: String? = null,
) {
    val surface = MaterialTheme.colorScheme.surfaceBright
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val guide = MaterialTheme.colorScheme.outlineVariant
    val currentValues by rememberUpdatedState(values)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(values.size) {
                detectTapGestures { offset ->
                    val index = chartIndexAt(offset.x, size.width.toFloat(), values.size)
                    // A tap on a day without a value picks nothing rather than an empty column.
                    currentOnSelect(index?.takeIf { currentValues[it] != null && it != currentSelected })
                }
            }
    ) {
        val present = values.withIndex().filter { it.value != null }
        if (present.isEmpty()) return@Canvas
        val numbers = present.map { it.value!! }
        val low = numbers.min()
        val high = numbers.max()
        // Pad the range so the line never runs along the very top or bottom edge.
        val pad = if (high == low) maxOf(1f, kotlin.math.abs(high) * 0.1f) else (high - low) * 0.12f
        val bottom = low - pad
        val top = high + pad
        val column = size.width / values.size
        val markerRadius = Marker.toPx() / 2f
        fun point(i: Int, v: Float) = Offset(i * column + column / 2f, size.height * ((top - v) / (top - bottom)))

        if (selected != null && values.getOrNull(selected) != null) {
            val x = selected * column + column / 2f
            drawLine(guide, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
        val path = Path()
        present.forEachIndexed { n, (i, v) ->
            val p = point(i, v!!)
            if (n == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        if (present.size > 1) {
            drawPath(path, color, style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // Markers only while they stay apart; a dense month reads better as the line alone.
        val showAll = column >= Marker.toPx() * 1.5f
        present.forEach { (i, v) ->
            if (showAll || i == selected || i == present.last().index) {
                val p = point(i, v!!)
                val r = if (i == selected) markerRadius * 1.4f else markerRadius
                drawCircle(surface, radius = r + Ring.toPx(), center = p)
                drawCircle(color, radius = r, center = p)
            }
        }
        if (endLabel != null) {
            val (lastIndex, lastValue) = present.last()
            val p = point(lastIndex, lastValue!!)
            val text = measurer.measure(endLabel, labelStyle)
            // Above the point, or below it when the point sits at the top edge.
            val above = p.y - markerRadius - Ring.toPx() - text.size.height
            val y = if (above >= 0f) above else p.y + markerRadius + Ring.toPx()
            drawText(text, topLeft = Offset((p.x - text.size.width).coerceAtLeast(0f), y))
        }
    }
}

/** The start and end of a chart's time axis, such as "12 Oct" and "Today", under the plot. */
@Composable
fun ChartAxis(start: String, end: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp)) {
        AxisText(start)
        Spacer(Modifier.weight(1f))
        AxisText(end)
    }
}

/** One label under each column, such as the scores 1 to 5 under a rating's bars. */
@Composable
fun ChartTicks(labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp)) {
        labels.forEach { label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { AxisText(label) }
        }
    }
}

@Composable
private fun AxisText(text: String) {
    // Axis text is muted ink and tabular, so ticks line up; never the series color.
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // The plot's semantics already describe the chart; ticks would only repeat it.
        modifier = Modifier.clearAndSetSemantics {},
    )
}

/** A share of a whole, such as the yes rate of a habit: a track in the same hue, filled to [fraction]. */
@Composable
fun Meter(fraction: Float, color: Color, contentDescription: String, modifier: Modifier = Modifier) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(SlateTileShape)
            .background(color.copy(alpha = 0.22f))
            .semantics { this.contentDescription = contentDescription }
    ) {
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            if (clamped > 0f) {
                drawRoundRect(color, size = Size(size.width * clamped, size.height), cornerRadius = CornerRadius(DataEnd.toPx()))
            }
        }
    }
}
