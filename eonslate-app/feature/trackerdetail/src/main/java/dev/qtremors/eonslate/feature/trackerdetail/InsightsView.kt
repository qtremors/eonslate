package dev.qtremors.eonslate.feature.trackerdetail

import dev.qtremors.eonslate.core.tracker.ui.intervalDuePoint
import dev.qtremors.eonslate.core.tracker.ui.intervalRemaining
import dev.qtremors.eonslate.core.ui.components.WeekdayBars
import dev.qtremors.eonslate.core.tracker.domain.insights.Weekdays
import dev.qtremors.eonslate.core.ui.components.YearHeatMap
import dev.qtremors.eonslate.core.ui.components.ProgressRing
import dev.qtremors.eonslate.core.tracker.domain.model.goal
import dev.qtremors.eonslate.core.tracker.domain.insights.localDay
import dev.qtremors.eonslate.core.tracker.domain.insights.HeatLevels
import dev.qtremors.eonslate.core.tracker.domain.insights.ConsistencyOf
import dev.qtremors.eonslate.core.tracker.ui.rhythmText
import dev.qtremors.eonslate.core.tracker.ui.intervalLastText
import dev.qtremors.eonslate.core.tracker.ui.intervalText
import dev.qtremors.eonslate.core.tracker.domain.insights.Intervals
import dev.qtremors.eonslate.core.tracker.ui.goalText
import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import dev.qtremors.eonslate.core.tracker.domain.model.GoalPeriod
import dev.qtremors.eonslate.core.tracker.domain.insights.Goals
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.insights.Bucket
import dev.qtremors.eonslate.core.tracker.domain.insights.FieldInsights
import dev.qtremors.eonslate.core.tracker.domain.insights.FieldStats
import dev.qtremors.eonslate.core.tracker.domain.insights.Period
import dev.qtremors.eonslate.core.tracker.domain.insights.Periods
import dev.qtremors.eonslate.core.tracker.domain.insights.Records
import dev.qtremors.eonslate.core.tracker.domain.insights.Streaks
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.ui.FieldTypes
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.ValueWords
import dev.qtremors.eonslate.core.tracker.ui.rememberValueWords
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.BarChart
import dev.qtremors.eonslate.core.ui.components.ChartAxis
import dev.qtremors.eonslate.core.ui.components.ChartTicks
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.LineChart
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * A tracker's insights: its streak, how often it was logged in the chosen period compared with
 * the one before, and what each field adds up to. Charts read in the tracker's pigment; the
 * running period and records wear ember. Field stats cover the whole history.
 */
@Composable
internal fun InsightsView(state: DetailState, tracker: Tracker, today: LocalDate, modifier: Modifier = Modifier, onDay: (LocalDate) -> Unit = {}) {
    val words = rememberValueWords()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val locale = Locale.getDefault()
    val pigment = remember(tracker.colorIndex, dark) { BadgePalette.mark(tracker.colorIndex, dark) }
    val logged = remember(state.times) { state.times.map { it.day() } }
    // With daily goals a day counts once they are met; logged days stand in until entries load.
    val days = remember(logged, state.allEntries, tracker) { state.allEntries?.let { Goals.doneDays(tracker, it).toList() } ?: logged }
    val streak = remember(days, today, tracker.schedule, state.firstDay) { Streaks.of(days, today, tracker.schedule, state.firstDay) }
    val unit = if (streak.inWeeks) R.plurals.insights_weeks else R.plurals.insights_days
    var period by rememberSaveable { mutableStateOf(Period.DAYS) }
    val buckets = remember(period, today, state.firstDay) { Periods.buckets(period, today, state.firstDay) }
    val counts = remember(state.times, buckets) { Periods.counts(state.times, buckets) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // As needed is never due, so it has no streak to show.
        if (!tracker.schedule.isAsNeeded) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = stringResource(R.string.insights_current_streak),
                value = pluralStringResource(unit, streak.current, streak.current),
                accent = streak.longestIsCurrent,
                modifier = Modifier.weight(1f),
                // Fills toward the best run, so a number reads as progress.
                progress = if (streak.longest > 0 && !streak.longestIsCurrent) streak.current.toFloat() / streak.longest else null,
                progressColor = pigment,
            )
            val shortDay = remember(locale) { DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM")) }
            StatTile(
                label = stringResource(R.string.insights_longest_streak),
                value = pluralStringResource(unit, streak.longest, streak.longest),
                supporting = when {
                    streak.longest == 0 -> null
                    streak.longestIsCurrent -> stringResource(R.string.insights_streak_ongoing)
                    else -> streak.longestEnd?.let { stringResource(R.string.insights_streak_ended, it.format(shortDay)) }
                },
                modifier = Modifier.weight(1f),
            )
        }

        ConsistencyRow(tracker, days, logged, today, state.firstDay, pigment)
        HeatMapCard(tracker, state, days, today, pigment, onDay)

        state.allEntries?.let { all ->
            IntervalTiles(tracker, all, today)
            GoalTiles(tracker, all, today, state.firstDay)
        }

        ConnectedChoices(
            options = Period.entries,
            selected = period,
            label = { stringResource(periodName(it)) },
            onSelect = { period = it },
        )

        Periods.compare(counts)?.let { (now, before) ->
            StatTile(
                label = stringResource(currentName(period)),
                value = pluralStringResource(R.plurals.insights_entries, now, now),
                supporting = when {
                    now > before -> stringResource(R.string.insights_more_than, now - before, stringResource(previousName(period)))
                    now < before -> stringResource(R.string.insights_fewer_than, before - now, stringResource(previousName(period)))
                    else -> stringResource(R.string.insights_same_as, stringResource(previousName(period)))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        ActivityCard(buckets = buckets, counts = counts, period = period, pigment = pigment)

        // Which weekdays carry the most entries, over the whole history.
        val weekdays = remember(state.times, state.firstDay) {
            Weekdays.totals(state.times.groupingBy { it.day() }.eachCount().mapValues { it.value.toDouble() }, state.firstDay)
        }
        Weekdays.busiest(weekdays)?.let { busiest ->
            val dayName = { d: java.time.DayOfWeek -> d.getDisplayName(java.time.format.TextStyle.FULL, locale) }
            ChartCard(
                title = stringResource(R.string.insights_by_weekday),
                caption = stringResource(R.string.insights_busiest, dayName(busiest)),
                rows = weekdays.map { (d, n) -> dayName(d) to n.toInt().toString() },
            ) {
                WeekdayBars(
                    totals = weekdays,
                    color = pigment,
                    contentDescription = stringResource(R.string.insights_busiest, dayName(busiest)),
                    peakLabel = weekdays.maxOf { it.second }.toInt().toString(),
                )
            }
        }

        val all = state.allEntries
        if (all == null) {
            LoadingState()
        } else {
            tracker.fields.forEach { field ->
                FieldInsightCard(field = field, entries = all, words = words, pigment = pigment, buckets = buckets, period = period)
            }
        }
    }
}

private fun periodName(period: Period): Int = when (period) {
    Period.DAYS -> R.string.insights_period_days
    Period.WEEKS -> R.string.insights_period_weeks
    Period.MONTHS -> R.string.insights_period_months
}

private fun currentName(period: Period): Int = when (period) {
    Period.DAYS -> R.string.insights_today
    Period.WEEKS -> R.string.insights_this_week
    Period.MONTHS -> R.string.insights_this_month
}

private fun previousName(period: Period): Int = when (period) {
    Period.DAYS -> R.string.insights_yesterday
    Period.WEEKS -> R.string.insights_last_week
    Period.MONTHS -> R.string.insights_last_month
}

/** How a bucket is named: a day, the week it starts, or a month. */
@Composable
private fun bucketLabel(bucket: Bucket, period: Period): String {
    val locale = Locale.getDefault()
    val day = remember(locale) { DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM")) }
    val short = remember(locale) { DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM")) }
    return when (period) {
        Period.DAYS -> bucket.start.format(day)
        Period.WEEKS -> stringResource(R.string.insights_week_of, bucket.start.format(short))
        Period.MONTHS -> bucket.start.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) +
            if (bucket.start.year != LocalDate.now().year) " " + bucket.start.year else ""
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    supporting: String? = null,
    progress: Float? = null,
    progressColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = if (accent) colors.tertiaryContainer else colors.surfaceBright, shape = SlateCardShape, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MetricTextStyle, color = if (accent) colors.onTertiaryContainer else colors.onSurface, modifier = Modifier.weight(1f, fill = false))
                if (accent) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.onTertiaryContainer, modifier = Modifier.padding(start = 6.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (accent) colors.onTertiaryContainer else colors.onSurfaceVariant)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            if (progress != null) Meter(progress, progressColor, contentDescription = label, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** A card holding one chart, its reading of the picked mark, and its table view. */
@Composable
private fun ChartCard(
    title: String,
    caption: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    chart: @Composable () -> Unit,
) {
    var table by rememberSaveable(title) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading?.invoke()
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = if (leading != null) 12.dp else 0.dp))
                if (rows.isNotEmpty()) {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { table = !table }) {
                        Text(stringResource(if (table) R.string.insights_show_chart else R.string.insights_show_table))
                    }
                }
            }
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (table) {
                rows.forEach { (label, value) ->
                    Row {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                }
            } else {
                chart()
            }
        }
    }
}

@Composable
private fun ActivityCard(buckets: List<Bucket>, counts: List<Int>, period: Period, pigment: Color) {
    var selected by rememberSaveable(period) { mutableStateOf<Int?>(null) }
    val total = counts.sum()
    val labels = buckets.map { bucketLabel(it, period) }
    val busiest = counts.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }
    val caption = selected?.let { i ->
        stringResource(R.string.insights_day_reading, labels[i], pluralStringResource(R.plurals.insights_entries, counts[i], counts[i]))
    } ?: pluralStringResource(R.plurals.insights_entries_in_period, total, total, stringResource(periodName(period)).lowercase(Locale.getDefault()))
    val summary = stringResource(R.string.insights_activity_summary, stringResource(periodName(period)), total, busiest?.let { labels[it.index] } ?: "–")
    ChartCard(
        title = stringResource(
            when (period) {
                Period.DAYS -> R.string.insights_activity
                Period.WEEKS -> R.string.insights_activity_weeks
                Period.MONTHS -> R.string.insights_activity_months
            }
        ),
        caption = caption,
        rows = counts.withIndex().filter { it.value > 0 }.reversed().map { labels[it.index] to it.value.toString() },
    ) {
        BarChart(
            values = counts.map { it.toFloat() },
            color = pigment,
            contentDescription = summary,
            selected = selected,
            onSelect = { selected = it },
            highlight = counts.lastIndex,
            peakLabel = busiest?.value?.toString(),
        )
        ChartAxis(start = labels.first(), end = stringResource(currentName(period)))
    }
}

@Composable
private fun FieldInsightCard(
    field: FieldDefinition,
    entries: List<Entry>,
    words: ValueWords,
    pigment: Color,
    buckets: List<Bucket>,
    period: Period,
) {
    val locale = LocalValueDisplay.current.numberLocale
    val stats = remember(field, entries) { FieldInsights.stats(field, entries) }
    val leading: @Composable () -> Unit = { IconBadge(icon = FieldTypes.icon(field.type), key = field.type.storageKey, size = 32.dp, iconSize = 18.dp) }

    when (stats) {
        is FieldStats.Numeric -> {
            val decimals = fieldDecimals(field)
            val series = remember(field, entries, buckets) { Periods.numeric(field, entries, buckets) }
            val labels = buckets.map { bucketLabel(it, period) }
            var selected by rememberSaveable(field.id, period) { mutableStateOf<Int?>(null) }
            val sums = field.config is FieldConfig.Money || field.config is FieldConfig.Duration
            // A week's or month's average of a plain number carries one more decimal.
            val seriesDecimals = if (!sums && period != Period.DAYS) decimals + 1 else decimals
            val picked = selected?.let { i -> series.getOrNull(i)?.let { i to it } }
            val caption = when {
                stats.count == 0 -> stringResource(R.string.insights_nothing_yet)
                picked != null -> stringResource(R.string.insights_day_reading, labels[picked.first], ValueFormat.amount(field, picked.second, words, locale, seriesDecimals))
                sums -> stringResource(
                    R.string.insights_numeric_sum,
                    ValueFormat.amount(field, stats.sum, words, locale, decimals),
                    ValueFormat.amount(field, stats.average, words, locale, decimals),
                )
                else -> stringResource(
                    R.string.insights_numeric_range,
                    ValueFormat.amount(field, stats.average, words, locale, decimals + 1),
                    ValueFormat.amount(field, stats.min, words, locale, decimals),
                    ValueFormat.amount(field, stats.max, words, locale, decimals),
                )
            }
            ChartCard(
                title = field.name,
                caption = caption,
                rows = series.withIndex().filter { it.value != null }.reversed()
                    .map { labels[it.index] to ValueFormat.amount(field, it.value!!, words, locale, seriesDecimals) },
                leading = leading,
            ) {
                if (stats.count > 0) {
                    val description = stringResource(R.string.insights_field_summary, field.name, stringResource(periodName(period)), caption)
                    if (sums) {
                        BarChart(
                            values = series.map { it?.toFloat() ?: 0f },
                            color = pigment,
                            contentDescription = description,
                            selected = selected,
                            onSelect = { selected = it },
                        )
                    } else {
                        LineChart(
                            values = series.map { it?.toFloat() },
                            color = pigment,
                            contentDescription = description,
                            selected = selected,
                            onSelect = { selected = it },
                            endLabel = series.lastOrNull { it != null }?.let { ValueFormat.amount(field, it, words, locale, seriesDecimals) },
                        )
                    }
                    ChartAxis(start = labels.first(), end = stringResource(currentName(period)))
                    val record = remember(field, entries) { Records.best(field, entries) }
                    if (record != null && stats.count > 1) {
                        RecordLine(
                            text = stringResource(
                                if (sums) R.string.insights_record_day else R.string.insights_record,
                                ValueFormat.amount(field, record.value, words, locale, decimals),
                                displayDate(record.day.toEpochDay()),
                            ),
                        )
                    }
                }
            }
        }
        is FieldStats.Rating -> {
            val max = (field.config as FieldConfig.Rating).max
            val caption = if (stats.count == 0) stringResource(R.string.insights_nothing_yet)
            else pluralStringResource(
                R.plurals.insights_rating_average,
                stats.count,
                ValueFormat.decimal(stats.average.setScale(1, RoundingMode.HALF_UP), 1, locale),
                max,
                stats.count,
            )
            var selected by rememberSaveable(field.id) { mutableStateOf<Int?>(null) }
            val scores = stats.distribution.entries.sortedBy { it.key }
            val shownCaption = selected?.let(scores::getOrNull)?.let {
                stringResource(R.string.insights_rating_reading, it.key, pluralStringResource(R.plurals.insights_times, it.value, it.value))
            } ?: caption
            ChartCard(
                title = field.name,
                caption = shownCaption,
                rows = scores.map { words.rating(it.key, max) to it.value.toString() },
                leading = leading,
            ) {
                if (stats.count > 0) {
                    BarChart(
                        values = scores.map { it.value.toFloat() },
                        color = pigment,
                        contentDescription = stringResource(R.string.insights_scores_summary, field.name, caption),
                        selected = selected,
                        onSelect = { selected = it },
                        height = 96.dp,
                    )
                    ChartTicks(scores.map { it.key.toString() })
                }
            }
        }
        is FieldStats.YesNo -> {
            val caption = if (stats.count == 0) stringResource(R.string.insights_nothing_yet)
            else stringResource(R.string.insights_yes_rate, stats.yesPercent, stats.yes, stats.count)
            ChartCard(
                title = field.name,
                caption = caption,
                rows = listOf(words.yes to stats.yes.toString(), words.no to stats.no.toString()),
                leading = leading,
            ) {
                if (stats.count > 0) Meter(stats.yes.toFloat() / stats.count, pigment, contentDescription = caption)
            }
        }
        is FieldStats.Choices -> {
            val labels = when (val c = field.config) {
                is FieldConfig.SingleChoice -> c.options.associate { it.id to it.label }
                is FieldConfig.MultiChoice -> c.options.associate { it.id to it.label }
                else -> emptyMap()
            }
            val caption = if (stats.count == 0) stringResource(R.string.insights_nothing_yet)
            else pluralStringResource(R.plurals.insights_answered, stats.count, stats.count)
            ChartCard(
                title = field.name,
                caption = caption,
                rows = stats.picks.map { (id, n) -> labels[id].orEmpty() to n.toString() },
                leading = leading,
            ) {
                if (stats.count > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Most picked first: the order the eye wants for "which is most common".
                        stats.picks.sortedByDescending { it.second }.forEach { (id, n) ->
                            val label = labels[id].orEmpty()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(n.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Meter(
                                    fraction = n.toFloat() / stats.count,
                                    color = pigment,
                                    contentDescription = stringResource(R.string.insights_choice_reading, label, pluralStringResource(R.plurals.insights_times, n, n)),
                                )
                            }
                        }
                    }
                }
            }
        }
        is FieldStats.Other -> ChartCard(
            title = field.name,
            caption = if (stats.count == 0) stringResource(R.string.insights_nothing_yet)
            else pluralStringResource(R.plurals.insights_filled_in, stats.count, stats.count),
            rows = emptyList(),
            leading = leading,
        ) {}
    }
}

/** A personal record under a chart, in ember with a trophy. */
@Composable
private fun RecordLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(end = 6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** How many fraction digits a field's own values carry. */
private fun fieldDecimals(field: FieldDefinition): Int = when (val c = field.config) {
    is FieldConfig.Number -> c.decimals
    is FieldConfig.Money -> MoneyAmount.fractionDigits(c.currencyCode)
    else -> 0
}

/** One tile per goal: how often it was met, where today stands, and the run of days or weeks met. */
@Composable
private fun GoalTiles(tracker: Tracker, all: List<Entry>, today: LocalDate, firstDay: java.time.DayOfWeek) {
    val progress = remember(tracker, all, today, firstDay) { Goals.progress(tracker, all, today, firstDay) }
    progress.forEach { p ->
        val weekly = p.goal.per == GoalPeriod.WEEK
        val met = remember(p.field, all, firstDay) { if (weekly) Goals.metWeeks(p.field, all, firstDay) else Goals.metDays(p.field, all) }
        val run = remember(met, today, firstDay) {
            if (weekly) Streaks.of(met, today, Schedule(timesPerWeek = 1), firstDay).current else Streaks.of(met, today).current
        }
        val unit = if (weekly) R.plurals.insights_weeks else R.plurals.insights_days
        StatTile(
            label = stringResource(R.string.insights_goal, p.field.name),
            value = pluralStringResource(if (weekly) R.plurals.insights_goal_weeks_met else R.plurals.insights_goal_days_met, met.size, met.size),
            supporting = goalText(p) + " · " + stringResource(R.string.insights_goal_run, pluralStringResource(unit, run, run)),
            accent = p.met,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * One tile per set interval: what is next and when it was last done. Then, for jobs without one,
 * the rhythm learned from history, so an As needed tracker shows its pace with no setup.
 */
@Composable
private fun IntervalTiles(tracker: Tracker, all: List<Entry>, today: LocalDate) {
    val statuses = remember(tracker, all, today) { Intervals.status(tracker, all, today) }
    statuses.forEach { status ->
        StatTile(
            label = status.label ?: stringResource(R.string.insights_interval),
            value = if (status.last == null) stringResource(R.string.insights_interval_waiting) else intervalRemaining(status, tracker),
            supporting = listOfNotNull(intervalDuePoint(status, tracker), intervalLastText(status, tracker)).joinToString(" · "),
            accent = status.due,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (!tracker.schedule.isAsNeeded) return
    val covered = statuses.map { it.label }.toSet()
    val rhythms = remember(tracker, all, today) { Intervals.learned(tracker, all, today) }.filter { it.label !in covered }
    rhythms.forEach { rhythm ->
        StatTile(
            label = rhythm.label ?: stringResource(R.string.insights_rhythm),
            value = rhythmText(rhythm, tracker).substringAfter(": ").replaceFirstChar { it.uppercase() },
            supporting = stringResource(R.string.insights_rhythm_hint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The share of due days done since the tracker began, as a ring, beside the day it began. */
@Composable
private fun ConsistencyRow(tracker: Tracker, done: List<LocalDate>, logged: List<LocalDate>, today: LocalDate, firstDay: java.time.DayOfWeek, pigment: androidx.compose.ui.graphics.Color) {
    val zone = java.time.ZoneId.systemDefault()
    val created = java.time.Instant.ofEpochMilli(tracker.createdAt).atZone(zone).toLocalDate()
    val start = (logged.minOrNull()?.let { minOf(it, created) } ?: created)
    val consistency = remember(tracker.schedule, done, start, today, firstDay) { ConsistencyOf.of(tracker.schedule, done.toSet(), start, today, firstDay) }
    val locale = Locale.getDefault()
    val longDate = remember(locale) { DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMyyyy")) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (consistency != null && consistency.due > 0) {
            Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = Modifier.weight(1f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgressRing(
                        fraction = consistency.share,
                        color = pigment,
                        contentDescription = stringResource(R.string.insights_consistency) + " " + consistency.percent + "%",
                        size = 64.dp,
                        thickness = 8.dp,
                    ) { Text("${consistency.percent}%", style = MaterialTheme.typography.labelLarge) }
                    Column {
                        Text(stringResource(R.string.insights_consistency), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            pluralStringResource(
                                if (consistency.inWeeks) R.plurals.insights_consistency_weeks else R.plurals.insights_consistency_days,
                                consistency.due, consistency.done, consistency.due,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        StatTile(
            label = stringResource(R.string.insights_started),
            value = start.format(longDate),
            modifier = if (consistency != null && consistency.due > 0) Modifier.weight(0.8f) else Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A year of days, shaded by how much was logged, or by how far a daily goal got. A tap opens the
 * day in the calendar.
 */
@Composable
private fun HeatMapCard(tracker: Tracker, state: DetailState, done: List<LocalDate>, today: LocalDate, pigment: androidx.compose.ui.graphics.Color, onDay: (LocalDate) -> Unit) {
    val daily = remember(tracker) { Goals.fields(tracker).firstOrNull { it.config.goal?.per == GoalPeriod.DAY } }
    val all = state.allEntries
    val levels = remember(state.times, all, daily) {
        if (daily != null && all != null) {
            // With a daily goal: a quarter step for each quarter of the way, full once met.
            all.groupBy { it.localDay() }.mapValues { (_, list) ->
                val progress = Goals.progress(tracker.copy(fields = listOf(daily)), list, list.first().localDay()).first()
                if (progress.met) 4 else (progress.fraction * 4).toInt().coerceIn(1, 3)
            }
        } else {
            HeatLevels.of(state.times.groupingBy { it.day() }.eachCount().mapValues { it.value.toDouble() })
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.insights_year), style = MaterialTheme.typography.titleSmall)
            YearHeatMap(
                levels = levels,
                today = today,
                firstDay = state.firstDay,
                color = pigment,
                contentDescription = done.count { it.isAfter(today.minusYears(1)) }.let { pluralStringResource(R.plurals.insights_year_summary, it, it) },
                onDay = onDay,
            )
        }
    }
}
