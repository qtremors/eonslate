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
        assertEquals(1_240L, status.amountLeft)
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
}
