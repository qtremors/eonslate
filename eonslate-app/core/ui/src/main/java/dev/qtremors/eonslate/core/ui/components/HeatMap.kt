package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val CELL = 14.dp
private val GAP = 3.dp
private val LABEL = 18.dp

/**
 * A year of days as small tiles, one column per week, shaded by [levels] (1 to 4, such as
 * quartiles of the day's amount) in one sequential ramp of [color]. Days without a level stay the
 * empty surface. It scrolls sideways and opens on today; a tap picks a day up to today.
 */
@Composable
fun YearHeatMap(
    levels: Map<LocalDate, Int>,
    today: LocalDate,
    firstDay: DayOfWeek,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    weeks: Int = 53,
    selected: LocalDate? = null,
    onDay: ((LocalDate) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val empty = colors.surfaceContainerHighest
    // One hue, light to dark: never a rainbow, readable on light, dark and pure black alike.
    val ramp = remember(empty, color) { listOf(0.16f, 0.36f, 0.62f, 1f).map { lerp(empty, color, it) } }
    val start = remember(today, firstDay, weeks) { today.with(TemporalAdjusters.previousOrSame(firstDay)).minusWeeks((weeks - 1).toLong()) }
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val locale = Locale.getDefault()
    val currentOnDay by rememberUpdatedState(onDay)
    val width = (CELL + GAP) * weeks
    val height = LABEL + (CELL + GAP) * 7

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.horizontalScroll(scroll)) {
            Canvas(
                Modifier
                    .size(width, height)
                    .semantics { this.contentDescription = contentDescription }
                    .pointerInput(start, today) {
                        detectTapGestures { offset ->
                            val step = (CELL + GAP).toPx()
                            val column = (offset.x / step).toInt()
                            val row = ((offset.y - LABEL.toPx()) / step).toInt()
                            if (row !in 0..6 || column !in 0 until weeks) return@detectTapGestures
                            val day = start.plusWeeks(column.toLong()).plusDays(row.toLong())
                            if (!day.isAfter(today)) currentOnDay?.invoke(day)
                        }
                    },
            ) {
                val cell = CELL.toPx()
                val step = (CELL + GAP).toPx()
                val top = LABEL.toPx()
                val radius = CornerRadius(4.dp.toPx())
                var lastMonth = -1
                for (column in 0 until weeks) {
                    val weekStart = start.plusWeeks(column.toLong())
                    // A month's name over the first week that starts in it.
                    if (weekStart.monthValue != lastMonth) {
                        lastMonth = weekStart.monthValue
                        if (column < weeks - 2) {
                            drawText(measurer, weekStart.month.getDisplayName(TextStyle.SHORT, locale), Offset(column * step, 0f), style = labelStyle)
                        }
                    }
                    for (row in 0..6) {
                        val day = weekStart.plusDays(row.toLong())
                        if (day.isAfter(today)) continue
                        val level = levels[day]
                        val fill = if (level == null) empty else ramp[(level - 1).coerceIn(0, 3)]
                        val at = Offset(column * step, top + row * step)
                        drawRoundRect(fill, at, Size(cell, cell), radius)
                        if (day == selected || day == today) {
                            drawRoundRect(colors.onSurface, at, Size(cell, cell), radius, style = Stroke(if (day == selected) 2.dp.toPx() else 1.dp.toPx()))
                        }
                    }
                }
            }
        }
        // The key: less to more.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResourceOrLess(true), style = labelStyle)
            (listOf(empty) + ramp).forEach { swatch ->
                Box(Modifier.size(CELL).background(swatch, RoundedCornerShape(4.dp)))
            }
            Text(stringResourceOrLess(false), style = labelStyle)
        }
    }
}

@Composable
private fun stringResourceOrLess(less: Boolean): String =
    androidx.compose.ui.res.stringResource(if (less) dev.qtremors.eonslate.core.ui.R.string.heat_less else dev.qtremors.eonslate.core.ui.R.string.heat_more)
