package dev.qtremors.eonslate.feature.trackerdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.DayOfWeek
import java.util.Locale

/**
 * A month of slate tiles. Days with entries wear the tracker's pigment, deeper with more entries;
 * today is outlined; the picked day is filled with the primary color.
 */
@Composable
internal fun MonthCalendar(
    month: YearMonth,
    counts: Map<LocalDate, Int>,
    colorIndex: Int,
    selected: LocalDate?,
    today: LocalDate,
    firstDay: DayOfWeek,
    onMonth: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val grid = remember(month, firstDay) { DetailModels.monthGrid(month, firstDay) }
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val pigment = remember(colorIndex, dark) { BadgePalette.forIndex(colorIndex, dark) }
    val haptics = rememberAppHaptics()
    val dayFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }

    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onMonth(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.detail_previous_month))
                }
                Text(
                    text = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + month.year,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = { onMonth(month.plusMonths(1)) },
                    enabled = month < YearMonth.from(today),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.detail_next_month))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailModels.weekdays(firstDay).forEach { weekday ->
                    Text(
                        text = weekday.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (month != YearMonth.from(today)) {
                // Months can be paged far back; one tap returns to this one.
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onMonth(YearMonth.from(today)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.detail_back_to_today)) }
            }
            grid.forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val count = counts[day] ?: 0
                                val isSelected = day == selected
                                val background = when {
                                    isSelected -> colors.primary
                                    count > 0 -> pigment.container
                                    else -> colors.surfaceContainerHigh.copy(alpha = 0.5f)
                                }
                                val content = when {
                                    isSelected -> colors.onPrimary
                                    count > 0 -> pigment.content
                                    day > today -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> colors.onSurfaceVariant
                                }
                                val description = if (count > 0) {
                                    pluralStringResource(R.plurals.detail_day_entries, count, day.format(dayFormat), count)
                                } else day.format(dayFormat)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(SlateTileShape)
                                        .background(background)
                                        .then(if (day == today && !isSelected) Modifier.border(2.dp, colors.tertiary, SlateTileShape) else Modifier)
                                        .selectable(selected = isSelected, enabled = day <= today, role = Role.Button) {
                                            haptics.tick()
                                            onDay(day)
                                        }
                                        .semantics { contentDescription = description },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = content,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
