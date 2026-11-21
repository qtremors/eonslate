package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The day an entry happened on, in the zone it was logged in. */
fun Entry.localDay(): LocalDate =
    Instant.ofEpochMilli(occurredAt).atZone(runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())).toLocalDate()

/**
 * A run of consecutive days with at least one entry.
 *
 * @property current the run that ends today, or yesterday when today has nothing logged yet, so a
 * streak is not shown as broken before the day is over. Zero when the last entry is older.
 * @property longest the longest run ever, which is at least [current].
 */
data class Streak(
    val current: Int,
    val longest: Int,
    val lastDay: LocalDate?,
    /** The last day of the longest run; the most recent one when two runs tie. */
    val longestEnd: LocalDate? = null,
    /** Weeks for a weekly target, which counts weeks met; days otherwise. */
    val inWeeks: Boolean = false,
) {
    /** True when the longest run is the one still going. */
    val longestIsCurrent: Boolean get() = current > 0 && current == longest
}

object Streaks {
    /**
     * The streak under [schedule]. Every day: consecutive days logged. Chosen weekdays: due days
     * logged in a row, so a day off never breaks it and an entry on a day off is a bonus. A weekly
     * target: weeks in a row that met it. Today, or this week, never breaks a run before it ends.
     */
    fun of(
        days: Collection<LocalDate>,
        today: LocalDate,
        schedule: Schedule,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): Streak = when {
        // Nothing is ever due, so there is no run to keep.
        schedule.isAsNeeded -> Streak(0, 0, days.maxOrNull())
        schedule.isDaily -> of(days, today)
        schedule.isWeekly -> weekly(days.toSortedSet(), today, schedule.timesPerWeek, weekStart)
        else -> weekdays(days.toSortedSet(), today, schedule.days)
    }

    private fun weekdays(logged: java.util.SortedSet<LocalDate>, today: LocalDate, due: Set<DayOfWeek>): Streak {
        if (logged.isEmpty()) return Streak(0, 0, null)
        var run = 0
        var longest = 0
        var longestEnd: LocalDate? = null
        var day = logged.first()
        val last = maxOf(today, logged.last())
        while (!day.isAfter(last)) {
            if (day.dayOfWeek in due) {
                if (day in logged) {
                    run++
                    if (run >= longest) {
                        longest = run
                        longestEnd = day
                    }
                } else if (day.isBefore(today)) {
                    run = 0
                }
            }
            day = day.plusDays(1)
        }
        return Streak(current = run, longest = longest, lastDay = logged.last(), longestEnd = longestEnd)
    }

    private fun weekly(logged: java.util.SortedSet<LocalDate>, today: LocalDate, target: Int, weekStart: DayOfWeek): Streak {
        if (logged.isEmpty()) return Streak(0, 0, null, inWeeks = true)
        fun startOf(day: LocalDate) = day.with(TemporalAdjusters.previousOrSame(weekStart))
        val perWeek = logged.groupingBy(::startOf).eachCount()
        val thisWeek = startOf(today)
        var run = 0
        var longest = 0
        var longestEnd: LocalDate? = null
        var week = startOf(logged.first())
        while (!week.isAfter(thisWeek)) {
            if ((perWeek[week] ?: 0) >= target) {
                run++
                if (run >= longest) {
                    longest = run
                    longestEnd = minOf(week.plusDays(6), today)
                }
            } else if (week.isBefore(thisWeek)) {
                run = 0
            }
            week = week.plusWeeks(1)
        }
        return Streak(current = run, longest = longest, lastDay = logged.last(), longestEnd = longestEnd, inWeeks = true)
    }

    fun of(days: Collection<LocalDate>, today: LocalDate): Streak {
        val sorted = days.toSortedSet()
        if (sorted.isEmpty()) return Streak(0, 0, null)

        var longest = 0
        var longestEnd: LocalDate? = null
        var run = 0
        var previous: LocalDate? = null
        for (day in sorted) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            if (run >= longest) {
                longest = run
                longestEnd = day
            }
            previous = day
        }

        val anchor = when {
            today in sorted -> today
            today.minusDays(1) in sorted -> today.minusDays(1)
            else -> null
        }
        var current = 0
        var day = anchor
        while (day != null && day in sorted) {
            current++
            day = day.minusDays(1)
        }
        return Streak(current = current, longest = longest, lastDay = sorted.last(), longestEnd = longestEnd)
    }
}

/**
 * What one field's logged values add up to. Numbers stay exact: sums are [BigDecimal] in the
 * field's own units, and averages carry two more decimals than the field, rounded half up.
 */
sealed interface FieldStats {
    val count: Int

    /** Number, money and duration fields. Duration values are in seconds. */
    data class Numeric(
        override val count: Int,
        val sum: BigDecimal,
        val average: BigDecimal,
        val min: BigDecimal,
        val max: BigDecimal,
        val latest: BigDecimal,
    ) : FieldStats

    data class Rating(
        override val count: Int,
        val average: BigDecimal,
        /** How many times each score was given, for every score on the scale. */
        val distribution: Map<Int, Int>,
    ) : FieldStats

    data class YesNo(override val count: Int, val yes: Int) : FieldStats {
        val no: Int get() = count - yes

        /** Share of yes answers in whole percent, rounded half up; zero with no answers. */
        val yesPercent: Int
            get() = if (count == 0) 0 else BigDecimal(yes * 100).divide(BigDecimal(count), 0, RoundingMode.HALF_UP).toInt()
    }

    /** How often each option was picked, in the field's option order. */
    data class Choices(override val count: Int, val picks: List<Pair<String, Int>>) : FieldStats

    data class Other(override val count: Int) : FieldStats
}

object FieldInsights {

    /**
     * Stats for [field] across [entries]. Values are read in time order, so "latest" is the one
     * that happened last, not the one logged last.
     */
    fun stats(field: FieldDefinition, entries: List<Entry>): FieldStats {
        val values = entries.sortedBy { it.occurredAt }.mapNotNull { it.values[field.id] }
        return when (val config = field.config) {
            is FieldConfig.Number -> numeric(values.filterIsInstance<FieldValue.Number>().map { BigDecimal.valueOf(it.scaled, config.decimals) }, config.decimals)
            is FieldConfig.Money -> {
                val digits = MoneyAmount.fractionDigits(config.currencyCode)
                numeric(values.filterIsInstance<FieldValue.Money>().map { BigDecimal.valueOf(it.minor, digits) }, digits)
            }
            is FieldConfig.Duration -> numeric(values.filterIsInstance<FieldValue.Duration>().map { BigDecimal.valueOf(it.seconds) }, 0)
            is FieldConfig.Rating -> {
                val ratings = values.filterIsInstance<FieldValue.Rating>().map { it.value }
                FieldStats.Rating(
                    count = ratings.size,
                    average = average(ratings.map { BigDecimal(it) }, 0),
                    distribution = (1..config.max).associateWith { score -> ratings.count { it == score } },
                )
            }
            FieldConfig.YesNo -> {
                val answers = values.filterIsInstance<FieldValue.YesNo>()
                FieldStats.YesNo(count = answers.size, yes = answers.count { it.value })
            }
            is FieldConfig.SingleChoice -> {
                val picked = values.filterIsInstance<FieldValue.SingleChoice>().map { it.optionId }
                FieldStats.Choices(picked.size, config.options.map { o -> o.id to picked.count { it == o.id } })
            }
            is FieldConfig.MultiChoice -> {
                val picked = values.filterIsInstance<FieldValue.MultiChoice>()
                FieldStats.Choices(picked.size, config.options.map { o -> o.id to picked.count { o.id in it.optionIds } })
            }
            is FieldConfig.Text, FieldConfig.Date -> FieldStats.Other(values.size)
        }
    }

    private fun numeric(values: List<BigDecimal>, decimals: Int): FieldStats.Numeric {
        if (values.isEmpty()) {
            val zero = BigDecimal.ZERO.setScale(decimals)
            return FieldStats.Numeric(0, zero, zero, zero, zero, zero)
        }
        return FieldStats.Numeric(
            count = values.size,
            sum = values.fold(BigDecimal.ZERO.setScale(decimals), BigDecimal::add),
            average = average(values, decimals),
            min = values.min(),
            max = values.max(),
            latest = values.last(),
        )
    }

    private fun average(values: List<BigDecimal>, decimals: Int): BigDecimal =
        if (values.isEmpty()) BigDecimal.ZERO.setScale(decimals + 2)
        else values.fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(values.size), decimals + 2, RoundingMode.HALF_UP)
}
