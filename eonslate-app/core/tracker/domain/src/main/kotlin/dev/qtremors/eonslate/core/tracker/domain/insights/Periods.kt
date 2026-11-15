package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** How far a chart looks back, and how it groups days. */
enum class Period(val buckets: Int) {
    DAYS(30),
    WEEKS(12),
    MONTHS(12),
}

/** One column of a chart: the days from [start] to [end], both included. */
data class Bucket(val start: LocalDate, val end: LocalDate) {
    operator fun contains(day: LocalDate): Boolean = day in start..end
}

/** A value now and the value one period before, such as this week's count and last week's. */
data class Comparison<T>(val current: T, val previous: T)

fun EntryTime.localDay(): LocalDate =
    Instant.ofEpochMilli(occurredAt).atZone(runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())).toLocalDate()

object Periods {

    /**
     * The columns for [period] ending with the one holding [today], oldest first. Weeks start on
     * [weekStart] (the locale's first day); months are calendar months. The last column may still
     * be running, so it ends today rather than in the future.
     */
    fun buckets(period: Period, today: LocalDate, weekStart: DayOfWeek): List<Bucket> = when (period) {
        Period.DAYS -> (period.buckets - 1 downTo 0).map { back -> today.minusDays(back.toLong()).let { Bucket(it, it) } }
        Period.WEEKS -> {
            val thisWeek = today.with(TemporalAdjusters.previousOrSame(weekStart))
            (period.buckets - 1 downTo 0).map { back ->
                val start = thisWeek.minusWeeks(back.toLong())
                Bucket(start, minOf(start.plusDays(6), today))
            }
        }
        Period.MONTHS -> {
            val thisMonth = today.withDayOfMonth(1)
            (period.buckets - 1 downTo 0).map { back ->
                val start = thisMonth.minusMonths(back.toLong())
                Bucket(start, minOf(start.plusMonths(1).minusDays(1), today))
            }
        }
    }

    /** How many entries fell in each bucket. */
    fun counts(times: List<EntryTime>, buckets: List<Bucket>): List<Int> {
        val days = times.map { it.localDay() }
        return buckets.map { bucket -> days.count { it in bucket } }
    }

    /**
     * One value per bucket for a numeric field, or null where nothing was logged. Money and time
     * add up (a week's spending, a week's sleep). Plain numbers take the last value of a single
     * day and the exact average of a longer bucket (a week's typical weight).
     */
    fun numeric(field: FieldDefinition, entries: List<Entry>, buckets: List<Bucket>): List<BigDecimal?> {
        val dated = entries.sortedBy { it.occurredAt }.map { it.localDay() to it }
        return buckets.map { bucket ->
            val inBucket = dated.filter { it.first in bucket }.map { it.second }
            when (val config = field.config) {
                is FieldConfig.Number -> {
                    val values = inBucket.mapNotNull { it.values[field.id] as? FieldValue.Number }.map { BigDecimal.valueOf(it.scaled, config.decimals) }
                    when {
                        values.isEmpty() -> null
                        bucket.start == bucket.end -> values.last()
                        else -> values.fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(values.size), config.decimals + 1, RoundingMode.HALF_UP)
                    }
                }
                is FieldConfig.Money -> {
                    val digits = MoneyAmount.fractionDigits(config.currencyCode)
                    inBucket.mapNotNull { it.values[field.id] as? FieldValue.Money }.takeIf { it.isNotEmpty() }
                        ?.fold(BigDecimal.ZERO.setScale(digits)) { acc, v -> acc + BigDecimal.valueOf(v.minor, digits) }
                }
                is FieldConfig.Duration -> inBucket.mapNotNull { it.values[field.id] as? FieldValue.Duration }.takeIf { it.isNotEmpty() }
                    ?.fold(BigDecimal.ZERO) { acc, v -> acc + BigDecimal.valueOf(v.seconds) }
                else -> null
            }
        }
    }

    /** The last bucket against the one before it, for "this week compared with last week". */
    fun <T> compare(values: List<T>): Comparison<T>? =
        if (values.size < 2) null else Comparison(values[values.size - 1], values[values.size - 2])
}
