package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.theme.ValueTextStyle
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Eight hues for telling categories apart, in a fixed order, stepped separately for light and dark
 * surfaces. Checked as a set for colour-blind separation; a ninth series is never a new hue but
 * folds into [other]. Three light steps sit under 3:1 on white, so every chart that uses them names
 * each part in text as well.
 */
object ChartPalette {
    private val light = listOf(0xFF2A78D6, 0xFFEB6834, 0xFF1BAF7A, 0xFFEDA100, 0xFFE87BA4, 0xFF008300, 0xFF6250D6, 0xFFE34948)
    private val dark = listOf(0xFF3987E5, 0xFFD95926, 0xFF199E70, 0xFFC98500, 0xFFD55181, 0xFF008300, 0xFF9085E9, 0xFFE66767)

    const val SIZE = 8

    /** The hue for slot [index]; past the eighth, the folded "Other" gray. */
    @Composable
    fun series(index: Int): Color {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val steps = if (isDark) dark else light
        return if (index in steps.indices) Color(steps[index]) else other()
    }

    /** A recessive gray for everything folded into "Other". */
    @Composable
    fun other(): Color = MaterialTheme.colorScheme.outline
}

/** One part of a whole: its name, the amount as written, the amount to scale by, and its colour. */
data class ChartSlice(val label: String, val valueText: String, val value: Float, val color: Color)

/**
 * A whole split into a few parts, read at a glance: a ring of arcs with a 2 dp gap between them.
 * The picked slice stands out and the rest recede; a tap picks a slice, a second tap clears it.
 * [center] sits in the hole, usually the total or the picked slice's amount.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    thickness: Dp = 14.dp,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
    center: @Composable () -> Unit = {},
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSlices by rememberUpdatedState(slices)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val c = Offset(this.size.width / 2f, this.size.height / 2f)
                        val dx = offset.x - c.x
                        val dy = offset.y - c.y
                        val radius = sqrt(dx * dx + dy * dy)
                        val outer = this.size.width / 2f
                        if (radius < outer - thickness.toPx() * 1.6f || radius > outer) return@detectTapGestures
                        // Angles run clockwise from twelve o'clock, as the slices are drawn.
                        val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 450f) % 360f
                        val sum = currentSlices.sumOf { it.value.toDouble() }.toFloat()
                        if (sum <= 0f) return@detectTapGestures
                        var start = 0f
                        val index = currentSlices.indexOfFirst { slice ->
                            val sweep = slice.value / sum * 360f
                            (angle >= start && angle < start + sweep).also { start += sweep }
                        }
                        if (index >= 0) currentOnSelect(if (index == currentSelected) null else index)
                    }
                }
        ) {
            if (total <= 0f) return@Canvas
            val stroke = thickness.toPx()
            val inset = stroke / 2f + 3.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val radius = arcSize.width / 2f
            val degrees = 180f / Math.PI.toFloat()
            // A slice too thin to be a pill (under one stroke plus a gap) is left out of the ring,
            // never drawn as a dot; its legend row still names it. One slice left is a whole ring.
            val pill = stroke / radius * degrees + 5.dp.toPx() / radius * degrees
            val several = slices.count { it.value / total * 360f >= pill } > 1
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = slice.value / total * 360f
                if (sweep <= 0f) return@forEachIndexed
                if (sweep < pill && slices.any { it.value / total * 360f >= pill }) {
                    start += sweep
                    return@forEachIndexed
                }
                val picked = selected == index
                val color = if (selected != null && !picked) slice.color.copy(alpha = 0.35f) else slice.color
                val width = if (picked) stroke + 4.dp.toPx() else stroke
                // Round caps reach half a stroke past each end, so the arc is drawn shorter by
                // that much plus a gap: every slice is its own pill. A sliver becomes a dot.
                val cap = if (several) (width / 2f) / radius * degrees else 0f
                val gap = if (several) 5.dp.toPx() / radius * degrees else 0f
                val drawn = sweep - cap * 2f - gap
                drawArc(
                    color = color,
                    startAngle = if (drawn > 0f) start + cap + gap / 2f else start + sweep / 2f,
                    sweepAngle = if (several) drawn.coerceAtLeast(0.01f) else 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = width, cap = if (several) StrokeCap.Round else StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Box(Modifier.padding(thickness + 12.dp), contentAlignment = Alignment.Center) { center() }
    }
}

/**
 * One whole as a single bar of blocks, each part its own colour with a 2 dp gap between, ends
 * rounded. It shows the split; the rows beside it carry the names and figures.
 */
@Composable
fun SegmentedBar(slices: List<ChartSlice>, contentDescription: String, modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
    ) {
        if (total <= 0f) return@Canvas
        val gap = 4.dp.toPx()
        val parts = slices.filter { it.value > 0f }
        val usable = size.width - gap * (parts.size - 1).coerceAtLeast(0)
        // Each part is a pill at least as wide as it is tall; the rest share what is left.
        val wanted = parts.map { maxOf(it.value / total * usable, size.height) }
        val scale = minOf(1f, usable / wanted.sum())
        var x = 0f
        parts.forEachIndexed { i, slice ->
            val width = wanted[i] * scale
            drawPill(x, 0f, x + width, size.height, slice.color)
            x += width + gap
        }
    }
}

/** A colour swatch, a name and a figure: one row of a chart's legend, which may be a button. */
@Composable
fun LegendRow(
    color: Color,
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(valueText, style = ValueTextStyle, color = colors.onSurfaceVariant)
    }
}

/**
 * One ratio against its limit as a ring: a track in the same hue, filled clockwise to [fraction].
 * Past the limit the ring is full; the colour and the text inside say by how much.
 */
@Composable
fun ProgressRing(
    fraction: Float,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    thickness: Dp = 12.dp,
    center: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).semantics { this.contentDescription = contentDescription }) {
            val stroke = thickness.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val track = color.copy(alpha = 0.24f)
            val degrees = 180f / Math.PI.toFloat()
            val radius = arcSize.width / 2f
            // Round caps reach half a stroke past each end; the arcs are shortened to leave a gap.
            val cap = (stroke / 2f) / radius * degrees
            val gap = 4.dp.toPx() / radius * degrees
            val sweep = fraction.coerceIn(0f, 1f) * 360f
            val round = Stroke(stroke, cap = StrokeCap.Round)
            when {
                sweep <= 0f -> drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                sweep >= 360f -> drawArc(color, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                else -> {
                    drawArc(color, -90f + cap, (sweep - cap * 2f).coerceAtLeast(0.01f), false, Offset(inset, inset), arcSize, style = round)
                    val rest = 360f - sweep - gap * 2f - cap * 2f
                    if (rest > 0f) drawArc(track, -90f + sweep + gap + cap, rest, false, Offset(inset, inset), arcSize, style = round)
                }
            }
        }
        Box(Modifier.padding(thickness + 6.dp), contentAlignment = Alignment.Center) { center() }
    }
}

/** One tile of a [Treemap]: its name, the amount as written, the amount to size by, and its colours. */
data class TreemapTile(
    val label: String,
    val valueText: String,
    val value: Float,
    val container: Color,
    val content: Color,
    /** False for a summary tile, such as the rest folded together: it opens nothing, comes last and is outlined. */
    val clickable: Boolean = true,
)

/**
 * Which items get their own tile: the largest, up to [maxTiles], while each holds at least
 * [minShare] of the whole. The rest are best folded into one tile; a single leftover keeps its own.
 */
fun treemapSplit(values: List<Double>, maxTiles: Int = 5, minShare: Double = 0.05): Int {
    val total = values.sum()
    if (total <= 0.0) return values.size
    val sorted = values.sortedDescending()
    var kept = sorted.indices.firstOrNull { it >= maxTiles || sorted[it] / total < minShare } ?: sorted.size
    if (sorted.size - kept == 1) kept = sorted.size
    return kept
}

/**
 * Parts sized by their share of the whole, as tiles filling a rectangle; the biggest are easiest to
 * spot, which is the point for what a month of subscriptions costs. Each tile names itself where
 * it fits and is a button.
 */
@Composable
fun Treemap(
    tiles: List<TreemapTile>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    onClick: (Int) -> Unit = {},
) {
    // Largest first, and a summary tile always last, however big.
    val parts = tiles.withIndex().filter { it.value.value > 0f }
        .sortedWith(compareBy<IndexedValue<TreemapTile>> { !it.value.clickable }.thenByDescending { it.value.value })
    if (parts.isEmpty()) return
    val outline = MaterialTheme.colorScheme.outlineVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val valueStyle = MaterialTheme.typography.labelMedium
    BoxWithConstraints(modifier.fillMaxWidth().height(height).semantics { this.contentDescription = contentDescription }) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val gap = with(density) { 4.dp.toPx() }
        val rects = squarify(parts.map { it.value.value }, 0f, 0f, widthPx, heightPx)
        parts.forEachIndexed { order, part ->
            val r = rects[order]
            val tile = part.value
            val w = with(density) { (r.w - gap).coerceAtLeast(1f).toDp() }
            val h = with(density) { (r.h - gap).coerceAtLeast(1f).toDp() }
            Box(
                Modifier
                    .offset { IntOffset(r.x.roundToInt(), r.y.roundToInt()) }
                    .size(w, h)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tile.container)
                    .then(
                        if (tile.clickable) Modifier.clickable { onClick(part.index) }
                        else Modifier.border(1.dp, outline, RoundedCornerShape(16.dp)),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                // Names only where they can be read; the list below names every item anyway.
                // One line of name, cut short if it must; the amount only when it fits whole, since a
                // clipped "₹2,17" reads as a different number.
                val inner = with(density) { (w - 24.dp).toPx() }
                if (w > 56.dp && h > 36.dp) {
                    Column {
                        Text(tile.label, style = labelStyle, color = tile.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (h > 56.dp && measurer.measure(tile.valueText, valueStyle, maxLines = 1).size.width <= inner) {
                            Text(tile.valueText, style = valueStyle, color = tile.content.copy(alpha = 0.8f), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

private data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)

/** Squarified treemap layout: rows of tiles along the shorter side, kept as close to square as it can. */
private fun squarify(values: List<Float>, x0: Float, y0: Float, width: Float, height: Float): List<Rect> {
    val total = values.sum()
    if (total <= 0f) return values.map { Rect(x0, y0, 0f, 0f) }
    val areas = values.map { it / total * width * height }
    val out = ArrayList<Rect>(values.size)
    var x = x0
    var y = y0
    var w = width
    var h = height
    var i = 0
    while (i < areas.size) {
        val side = min(w, h)
        val row = mutableListOf(areas[i])
        var j = i + 1
        while (j < areas.size && worst(row + areas[j], side) <= worst(row, side)) {
            row += areas[j]
            j++
        }
        val sum = row.sum()
        if (w >= h) {
            val colW = sum / h
            var cy = y
            row.forEach { a -> out += Rect(x, cy, colW, a / colW).also { cy += a / colW } }
            x += colW
            w -= colW
        } else {
            val rowH = sum / w
            var cx = x
            row.forEach { a -> out += Rect(cx, y, a / rowH, rowH).also { cx += a / rowH } }
            y += rowH
            h -= rowH
        }
        i = j
    }
    return out
}

private fun worst(row: List<Float>, side: Float): Float {
    val sum = row.sum()
    val maxA = row.max()
    val minA = row.min()
    val s2 = side * side
    return max(s2 * maxA / (sum * sum), (sum * sum) / (s2 * minA))
}

/**
 * Two values per column side by side, such as money in and out each month, on one shared axis
 * from zero. The picked column stands out; a tap reports the column.
 */
@Composable
fun PairedBarChart(
    pairs: List<Pair<Float, Float>>,
    firstColor: Color,
    secondColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
    height: Dp = 140.dp,
) {
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(pairs.size) {
                detectTapGestures { offset ->
                    val index = chartIndexAt(offset.x, size.width.toFloat(), pairs.size)
                    currentOnSelect(if (index == currentSelected) null else index)
                }
            }
    ) {
        if (pairs.isEmpty()) return@Canvas
        val peak = pairs.maxOf { max(it.first, it.second) }.takeIf { it > 0f } ?: return@Canvas
        val column = size.width / pairs.size
        val gap = minOf(3.dp.toPx(), column * 0.08f)
        val outer = minOf(PillGap.toPx(), column * 0.2f)
        val bar = ((column - outer - gap) / 2f).coerceIn(1.5.dp.toPx(), 14.dp.toPx())
        val bottom = size.height - PillLift.toPx()
        pairs.forEachIndexed { i, (a, b) ->
            val dim = selected != null && selected != i
            val left = i * column + (column - bar * 2 - gap) / 2f
            listOf(a to firstColor, b to secondColor).forEachIndexed { k, (value, color) ->
                if (value <= 0f) return@forEachIndexed
                val top = minOf(bottom * (1f - value / peak), bottom - PillMin.toPx())
                val x = left + k * (bar + gap)
                drawPill(x, top, x + bar, bottom, if (dim) color.copy(alpha = 0.35f) else color)
            }
        }
    }
}

/** A small swatch and name per series, for charts with more than one. */
@Composable
fun ChartKey(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
