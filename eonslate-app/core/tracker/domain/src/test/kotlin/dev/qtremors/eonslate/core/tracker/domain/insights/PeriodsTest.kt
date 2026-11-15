package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class PeriodsTest {
    // A Wednesday.
    private val today = LocalDate.of(2026, 11, 11)

    private fun millis(day: LocalDate) = day.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    private fun time(day: LocalDate) = EntryTime(millis(day), "UTC")
    private fun entry(day: LocalDate, values: Map<String, FieldValue>, hour: Int = 12) = Entry(
        id = "$day-$hour", trackerId = "t", occurredAt = day.atTime(hour, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        zoneId = "UTC", values = values, createdAt = 0,
    )

    @Test
    fun daysAreSingleDayBucketsEndingToday() {
        val buckets = Periods.buckets(Period.DAYS, today, DayOfWeek.MONDAY)
        assertEquals(30, buckets.size)
        assertEquals(Bucket(today, today), buckets.last())
        assertEquals(today.minusDays(29), buckets.first().start)
    }

    @Test
    fun weeksStartOnTheLocalesDayAndStopAtToday() {
        val monday = Periods.buckets(Period.WEEKS, today, DayOfWeek.MONDAY)
        assertEquals(12, monday.size)
        assertEquals(Bucket(LocalDate.of(2026, 11, 9), today), monday.last())
        assertEquals(Bucket(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8)), monday[10])
        val sunday = Periods.buckets(Period.WEEKS, today, DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 8), sunday.last().start)
    }

    @Test
    fun monthsAreCalendarMonths() {
        val months = Periods.buckets(Period.MONTHS, today, DayOfWeek.MONDAY)
        assertEquals(Bucket(LocalDate.of(2026, 11, 1), today), months.last())
        assertEquals(Bucket(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), months[2])
        assertEquals(LocalDate.of(2025, 12, 1), months.first().start)
    }

    @Test
    fun countsFallInTheirBucket() {
        val buckets = Periods.buckets(Period.WEEKS, today, DayOfWeek.MONDAY)
        val times = listOf(today, today.minusDays(1), LocalDate.of(2026, 11, 8), LocalDate.of(2026, 1, 1)).map(::time)
        val counts = Periods.counts(times, buckets)
        assertEquals(2, counts.last())
        assertEquals(1, counts[10])
        // Out of range entries count nowhere.
        assertEquals(3, counts.sum())
    }

    @Test
    fun numbersAverageOverAWeekAndMoneyAddsUp() {
        val weight = FieldDefinition("w", "t", "Weight", FieldConfig.Number("kg", 1))
        val spent = FieldDefinition("m", "t", "Spent", FieldConfig.Money("INR"))
        val entries = listOf(
            entry(LocalDate.of(2026, 11, 9), mapOf("w" to FieldValue.Number(720), "m" to FieldValue.Money(10_000))),
            entry(LocalDate.of(2026, 11, 10), mapOf("w" to FieldValue.Number(725), "m" to FieldValue.Money(2_550))),
            entry(today, mapOf("w" to FieldValue.Number(731))),
        )
        val weeks = Periods.buckets(Period.WEEKS, today, DayOfWeek.MONDAY)
        // (72.0 + 72.5 + 73.1) / 3 = 72.533..., one more decimal than the field.
        assertEquals(BigDecimal("72.53"), Periods.numeric(weight, entries, weeks).last())
        assertEquals(BigDecimal("125.50"), Periods.numeric(spent, entries, weeks).last())
        assertNull(Periods.numeric(weight, entries, weeks)[10])
    }

    @Test
    fun aSingleDayKeepsItsLastNumber() {
        val weight = FieldDefinition("w", "t", "Weight", FieldConfig.Number("kg", 1))
        val entries = listOf(
            entry(today, mapOf("w" to FieldValue.Number(730)), hour = 7),
            entry(today, mapOf("w" to FieldValue.Number(724)), hour = 21),
        )
        val days = Periods.buckets(Period.DAYS, today, DayOfWeek.MONDAY)
        assertEquals(BigDecimal("72.4"), Periods.numeric(weight, entries, days).last())
    }

    @Test
    fun theLastBucketIsComparedWithTheOneBefore() {
        assertEquals(Comparison(5, 3), Periods.compare(listOf(1, 3, 5)))
        assertNull(Periods.compare(listOf(4)))
    }
}
