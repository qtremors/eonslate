package dev.qtremors.eonslate.feature.trackerdetail

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.ui.ValueWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DetailModelsTest {
    private fun at(date: String, time: String, zone: String = "Asia/Kolkata"): Long =
        ZonedDateTime.of(LocalDate.parse(date), java.time.LocalTime.parse(time), ZoneId.of(zone)).toInstant().toEpochMilli()

    private fun entry(id: String, occurredAt: Long, zone: String = "Asia/Kolkata", values: Map<String, FieldValue> = emptyMap(), note: String = "") =
        Entry(id = id, trackerId = "t", occurredAt = occurredAt, zoneId = zone, values = values, note = note, createdAt = occurredAt)

    @Test
    fun entriesGroupByTheirOwnDayNewestFirst() {
        val groups = DetailModels.groupByDay(
            listOf(
                entry("a", at("2026-11-09", "08:00")),
                entry("b", at("2026-11-10", "07:00")),
                entry("c", at("2026-11-10", "21:00")),
            )
        )
        assertEquals(listOf(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-09")), groups.map { it.day })
        assertEquals(listOf("c", "b"), groups[0].entries.map { it.id })
    }

    @Test
    fun aLateNightEntryStaysOnItsLocalDay() {
        // 23:30 in Kolkata is 18:00 UTC: it belongs to the 10th, wherever the phone is now.
        val late = entry("late", at("2026-11-10", "23:30"))
        assertEquals(LocalDate.parse("2026-11-10"), late.day())
        val travelled = entry("ny", at("2026-11-10", "23:30", "America/New_York"), zone = "America/New_York")
        assertEquals(LocalDate.parse("2026-11-10"), travelled.day())
    }

    @Test
    fun countsAndTotals() {
        val today = LocalDate.parse("2026-11-10")
        val list = listOf(
            entry("a", at("2026-11-10", "08:00")),
            entry("b", at("2026-11-10", "09:00")),
            entry("c", at("2026-11-04", "09:00")),
            entry("d", at("2026-11-03", "09:00")),
        )
        val times = list.map { EntryTime(it.occurredAt, it.zoneId) }
        assertEquals(mapOf(today to 2, LocalDate.parse("2026-11-04") to 1, LocalDate.parse("2026-11-03") to 1), DetailModels.countsByDay(times))
        // The last seven days are the 4th through the 10th.
        assertEquals(DetailModels.Totals(all = 4, lastSevenDays = 3, activeDays = 3), DetailModels.totals(times, today))
    }

    @Test
    fun aDaysWindowHoldsItsEntriesInEveryZone() {
        val day = LocalDate.parse("2026-11-10")
        val window = dayWindow(day)
        listOf("Pacific/Kiritimati", "Pacific/Pago_Pago", "UTC", "Asia/Kolkata").forEach { zone ->
            val first = at("2026-11-10", "00:00", zone)
            val last = at("2026-11-10", "23:59", zone)
            assertTrue(zone, first in window && last in window)
        }
    }

    @Test
    fun monthGridPutsTheFirstUnderItsWeekday() {
        // November 2026 starts on a Sunday.
        val sundayFirst = DetailModels.monthGrid(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.parse("2026-11-01"), sundayFirst[0][0])
        assertEquals(5, sundayFirst.size)
        val mondayFirst = DetailModels.monthGrid(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(6, mondayFirst[0].count { it == null })
        assertEquals(LocalDate.parse("2026-11-01"), mondayFirst[0][6])
        assertEquals(6, mondayFirst.size)
        (sundayFirst + mondayFirst).forEach { assertEquals(7, it.size) }
        assertEquals(30, mondayFirst.flatten().count { it != null })
    }

    @Test
    fun februaryInALeapYearFitsExactly() {
        // February 2032 starts on a Sunday and has 29 days.
        val grid = DetailModels.monthGrid(YearMonth.of(2032, 2), DayOfWeek.SUNDAY)
        assertEquals(29, grid.flatten().count { it != null })
        assertNull(grid.last().last())
    }

    @Test
    fun weekdaysStartWhereTheLocaleSays() {
        assertEquals(DayOfWeek.MONDAY, DetailModels.weekdays(DayOfWeek.MONDAY).first())
        assertEquals(DayOfWeek.SATURDAY, DetailModels.weekdays(DayOfWeek.SUNDAY).last())
    }

    @Test
    fun summaryFollowsFieldOrderAndFallsBackToTheNote() {
        val words = ValueWords("Yes", "No", { h, m -> "$h h $m m" }, { m -> "$m m" }, { m, s -> "$m m $s s" }, { v, max -> "$v/$max" }, "Removed")
        val tracker = Tracker(
            id = "t",
            name = "Run",
            fields = listOf(
                FieldDefinition("k", "t", "Distance", FieldConfig.Number("km", 2)),
                FieldDefinition("d", "t", "Time", FieldConfig.Duration()),
                FieldDefinition("e", "t", "Effort", FieldConfig.Rating(5)),
                FieldDefinition("x", "t", "Shoes", FieldConfig.Text()),
            ),
            createdAt = 0,
        )
        val full = entry("a", 0, values = mapOf("e" to FieldValue.Rating(4), "k" to FieldValue.Number(525), "d" to FieldValue.Duration(1_920), "x" to FieldValue.Text("Old")))
        assertEquals("5.25 km · 32 m · 4/5", DetailModels.summary(full, tracker, words, Locale.US))
        assertEquals("Rest day", DetailModels.summary(entry("b", 0, note = "Rest day\nsecond line"), tracker, words, Locale.US))
    }
}
