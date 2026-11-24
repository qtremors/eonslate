package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Interval
import dev.qtremors.eonslate.core.tracker.domain.model.IntervalUnit
import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class IntervalsTest {
    private val zone = "Asia/Kolkata"
    private val what = FieldDefinition(
        "what", "bike", "What",
        FieldConfig.SingleChoice(listOf(ChoiceOption("oil", "Oil change"), ChoiceOption("service", "Servicing"), ChoiceOption("fuel", "Fuel"))),
    )
    private val km = FieldDefinition("km", "bike", "Odometer", FieldConfig.Number("km"))
    private val oil = Interval("i1", "what", "oil", amountFieldId = "km", amount = 3000)
    private val service = Interval("i2", "what", "service", every = 6, unit = IntervalUnit.MONTH)
    private val bike = Tracker(
        id = "bike", name = "Bike", fields = listOf(what, km), createdAt = 0,
        schedule = Schedule.AS_NEEDED, intervals = listOf(oil, service),
    )
    private var n = 0
    private fun e(day: LocalDate, job: String, odometer: Long? = null) = Entry(
        id = "e${n++}", trackerId = "bike", zoneId = zone, createdAt = 0,
        occurredAt = day.atTime(10, 0).atZone(ZoneId.of(zone)).toInstant().toEpochMilli(),
        values = buildMap {
            put("what", FieldValue.SingleChoice(job))
            odometer?.let { put("km", FieldValue.Number(it)) }
        },
    )

    @Test
    fun anOilChangeIsDueThreeThousandKilometresLaterReadFromAnyLog() {
        val entries = listOf(
            e(LocalDate.of(2026, 4, 1), "oil", 24_000),
            e(LocalDate.of(2026, 8, 1), "oil", 27_000),
            e(LocalDate.of(2026, 11, 1), "fuel", 28_760),
        )
        val status = Intervals.status(bike, entries, LocalDate.of(2026, 11, 14)).first { it.interval == oil }
        assertEquals("Oil change", status.label)
        assertEquals(30_000L, status.dueAt)
        assertEquals(28_760L, status.current)
        // 4,760 km over 214 days is about 22 km a day, so 13 days on the odometer reads about 29,049.
        assertEquals(951L, status.amountLeft)
        assertTrue(status.estimated)
        assertFalse(status.due)
        val later = entries + e(LocalDate.of(2026, 12, 1), "fuel", 30_050)
        assertTrue(Intervals.status(bike, later, LocalDate.of(2026, 12, 2)).first { it.interval == oil }.due)
    }

    @Test
    fun servicingIsDueSixMonthsAfterTheLastOneAndANeverLoggedJobIsNotDue() {
        val entries = listOf(e(LocalDate.of(2026, 1, 10), "service"))
        val before = Intervals.status(bike, entries, LocalDate.of(2026, 7, 9)).first { it.interval == service }
        assertEquals(LocalDate.of(2026, 7, 10), before.dueOn)
        assertEquals(1L, before.daysLeft)
        assertFalse(before.due)
        assertTrue(Intervals.status(bike, entries, LocalDate.of(2026, 7, 10)).first { it.interval == service }.due)
        val oilNever = Intervals.status(bike, entries, LocalDate.of(2026, 7, 10)).first { it.interval == oil }
        assertNull(oilNever.last)
        assertFalse(oilNever.due)
    }

    @Test
    fun aRhythmIsLearnedFromHistoryWithNoSetup() {
        val plain = bike.copy(intervals = emptyList())
        val entries = listOf(
            e(LocalDate.of(2025, 4, 1), "oil", 21_000),
            e(LocalDate.of(2025, 8, 1), "oil", 24_000),
            e(LocalDate.of(2026, 4, 1), "oil", 27_000),
            e(LocalDate.of(2026, 5, 1), "fuel", 27_800),
        )
        val rhythm = Intervals.learned(plain, entries, LocalDate.of(2026, 5, 2)).single { it.label == "Oil change" }
        assertEquals(3_000L, rhythm.growth)
        assertEquals(30_000L, rhythm.nextAt)
        assertEquals(2_200L, rhythm.amountLeft)
        // Two logs are not yet a rhythm.
        assertTrue(Intervals.learned(plain, entries.take(2), LocalDate.of(2026, 5, 2)).none { it.label == "Oil change" })
    }

    @Test
    fun intervalsMustPointAtRealFields() {
        assertTrue(oil.isValid(bike.fields))
        assertFalse(oil.copy(optionId = "missing").isValid(bike.fields))
        assertFalse(Interval("x").isValid(bike.fields))
        assertFalse(oil.copy(amountFieldId = "what").isValid(bike.fields))
    }

    @Test
    fun partsListEveryOptionWithItsLastLogIntervalAndRhythm() {
        val parted = bike.copy(fields = listOf(what.copy(config = (what.config as FieldConfig.SingleChoice).copy(parts = true)), km))
        val entries = listOf(
            e(LocalDate.of(2026, 4, 1), "oil", 24_000),
            e(LocalDate.of(2026, 8, 1), "oil", 27_000),
            e(LocalDate.of(2026, 10, 1), "fuel", 28_000),
            e(LocalDate.of(2026, 10, 15), "fuel", 28_400),
            e(LocalDate.of(2026, 11, 1), "fuel", 28_760),
        )
        val parts = Parts.of(parted, entries, LocalDate.of(2026, 11, 14))
        assertEquals(listOf("oil", "service", "fuel"), parts.map { it.optionId })
        assertEquals(951L, parts[0].interval?.amountLeft)
        assertEquals(LocalDate.of(2026, 8, 1), parts[0].last)
        assertEquals(0, parts[1].count)
        assertNull(parts[1].last)
        assertEquals(3, parts[2].count)
        assertEquals("fuel", parts[2].rhythm?.optionId)
        assertTrue(Parts.of(bike, entries, LocalDate.of(2026, 11, 14)).isEmpty())
    }

    @Test
    fun withTheOdometerReadOnlyAtOilChangesTheUsualPaceCountsDown() {
        // Read only at each change: 3,000 km every four months, about 24.6 km a day.
        val entries = listOf(
            e(LocalDate.of(2026, 4, 1), "oil", 24_000),
            e(LocalDate.of(2026, 8, 1), "oil", 27_000),
        )
        val status = Intervals.status(bike, entries, LocalDate.of(2026, 10, 1)).first { it.interval == oil }
        assertEquals(27_000L, status.current)
        assertTrue(status.estimated)
        assertTrue(status.amountLeft!! in 1_400L..1_600L)
        assertEquals(LocalDate.of(2026, 12, 1), status.amountDueOn)
        // Past the usual day it falls due with no new reading.
        assertTrue(Intervals.status(bike, entries, LocalDate.of(2026, 12, 5)).first { it.interval == oil }.due)
        // A reading made today is taken as it is.
        val today = entries + e(LocalDate.of(2026, 10, 1), "fuel", 28_000)
        val read = Intervals.status(bike, today, LocalDate.of(2026, 10, 1)).first { it.interval == oil }
        assertFalse(read.estimated)
        assertEquals(2_000L, read.amountLeft)
    }

    @Test
    fun oneReadingHasNoPace() {
        val status = Intervals.status(bike, listOf(e(LocalDate.of(2026, 8, 1), "oil", 27_000)), LocalDate.of(2026, 10, 1)).first { it.interval == oil }
        assertFalse(status.estimated)
        assertEquals(3_000L, status.amountLeft)
    }

    @Test
    fun aSetDailyAmountStandsInForTheLearnedPace() {
        val thirty = bike.copy(intervals = listOf(oil.copy(perDay = 30), service))
        val entries = listOf(e(LocalDate.of(2026, 8, 1), "oil", 27_000))
        val status = Intervals.status(thirty, entries, LocalDate.of(2026, 9, 1)).first { it.interval.id == "i1" }
        // 31 days at 30 km a day: about 27,930, so about 2,070 left, due 100 days after the change.
        assertEquals(2_070L, status.amountLeft)
        assertEquals(LocalDate.of(2026, 11, 9), status.amountDueOn)
        assertFalse(oil.copy(perDay = 0).isValid(bike.fields))
        assertFalse(service.copy(perDay = 30).isValid(bike.fields))
    }

    @Test
    fun aCommuteOnWorkdaysCountsOnlyThoseDays() {
        val workdays = 0b0011111
        val commute = bike.copy(intervals = listOf(oil.copy(perDay = 30, countDays = workdays), service))
        // Changed on Monday 3 Aug at 27,000 km.
        val entries = listOf(e(LocalDate.of(2026, 8, 3), "oil", 27_000))
        // Two weeks later, Monday 17 Aug: ten workdays, 300 km.
        val status = Intervals.status(commute, entries, LocalDate.of(2026, 8, 17)).first { it.interval.id == "i1" }
        assertEquals(2_700L, status.amountLeft)
        // 3,000 km is 100 workdays, 20 weeks: Monday 21 Dec.
        assertEquals(LocalDate.of(2026, 12, 21), status.amountDueOn)
        assertEquals(5L, Intervals.countedDays(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10), workdays))
        assertEquals(LocalDate.of(2026, 8, 4), Intervals.afterCountedDays(LocalDate.of(2026, 8, 3), 1, workdays))
        // Friday plus one workday is Monday.
        assertEquals(LocalDate.of(2026, 8, 10), Intervals.afterCountedDays(LocalDate.of(2026, 8, 7), 1, workdays))
        assertFalse(oil.copy(countDays = 0).isValid(bike.fields))
        assertFalse(service.copy(countDays = workdays).isValid(bike.fields))
    }
}
