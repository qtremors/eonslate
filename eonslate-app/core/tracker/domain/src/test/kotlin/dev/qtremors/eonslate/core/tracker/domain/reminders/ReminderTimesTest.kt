package dev.qtremors.eonslate.core.tracker.domain.reminders

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimesTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val berlin = ZoneId.of("Europe/Berlin")

    // 2026-11-11 is a Wednesday.
    private fun at(h: Int, m: Int, zone: ZoneId = kolkata, date: LocalDate = LocalDate.of(2026, 11, 11)) =
        ZonedDateTime.of(date.atTime(h, m), zone)

    private fun reminder(minute: Int, days: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
        Reminder("r", "t", minute, days)

    @Test
    fun laterTodayIsToday() {
        assertEquals(at(21, 0), ReminderTimes.next(reminder(21 * 60), at(9, 0)))
    }

    @Test
    fun anAlarmOnTheMinuteSchedulesTomorrow() {
        assertEquals(at(9, 0).plusDays(1), ReminderTimes.next(reminder(9 * 60), at(9, 0)))
    }

    @Test
    fun weekdaysWrapToNextWeek() {
        val mondays = reminder(8 * 60, setOf(DayOfWeek.MONDAY))
        assertEquals(at(8, 0, date = LocalDate.of(2026, 11, 16)), ReminderTimes.next(mondays, at(9, 0)))
        val wednesdays = reminder(8 * 60, setOf(DayOfWeek.WEDNESDAY))
        assertEquals(at(8, 0, date = LocalDate.of(2026, 11, 18)), ReminderTimes.next(wednesdays, at(9, 0)))
    }

    @Test
    fun noDaysMeansNever() {
        assertNull(ReminderTimes.next(reminder(60, emptySet()), at(9, 0)))
    }

    @Test
    fun aTimeInTheSpringGapMovesForward() {
        // Berlin skips 02:00 to 03:00 on 2027-03-28.
        val before = at(1, 0, berlin, LocalDate.of(2027, 3, 28))
        val due = ReminderTimes.next(reminder(2 * 60 + 30), before)!!
        assertEquals(LocalDate.of(2027, 3, 28), due.toLocalDate())
        assertEquals(3, due.hour)
        assertEquals(30, due.minute)
    }

    @Test
    fun anAutumnRepeatFiresOnce() {
        // Berlin repeats 02:00 to 03:00 on 2026-10-25; the first 02:30 is due, then the next day.
        val first = ReminderTimes.next(reminder(2 * 60 + 30), at(1, 0, berlin, LocalDate.of(2026, 10, 25)))!!
        val after = ReminderTimes.next(reminder(2 * 60 + 30), first)!!
        assertEquals(LocalDate.of(2026, 10, 26), after.toLocalDate())
    }

    @Test
    fun loggedTodayUsesTheEntrysOwnZone() {
        val day = LocalDate.of(2026, 11, 11)
        // 2026-11-10 20:00 UTC is already the 11th in Kolkata.
        val millis = ZonedDateTime.of(2026, 11, 10, 20, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        val entry = Entry(id = "e", trackerId = "t", occurredAt = millis, zoneId = "Asia/Kolkata", createdAt = millis)
        assertTrue(ReminderTimes.loggedOn(entry, day))
        assertFalse(ReminderTimes.loggedOn(entry.copy(zoneId = "UTC"), day))
        assertFalse(ReminderTimes.loggedOn(null, day))
    }

    @Test
    fun daysRoundTripThroughTheMask() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
        assertEquals(1 or 64, Reminder.daysToMask(days))
        assertEquals(days, Reminder.maskToDays(65))
        assertEquals(127, Reminder.daysToMask(DayOfWeek.entries.toSet()))
    }
}
