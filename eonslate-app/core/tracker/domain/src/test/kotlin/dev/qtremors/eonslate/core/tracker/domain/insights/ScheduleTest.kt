package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class ScheduleTest {
    // 2026-11-02 is a Monday.
    private fun day(d: Int): LocalDate = LocalDate.of(2026, 11, d)
    private val mwf = Schedule(setOf(MONDAY, WEDNESDAY, FRIDAY))

    @Test
    fun weekdayStreaksSkipDaysOff() {
        // Mon, Wed, Fri, then Mon: four due days in a row, the weekend and Tue/Thu skipped.
        val streak = Streaks.of(listOf(day(2), day(4), day(6), day(9)), today = day(10), schedule = mwf)
        assertEquals(4, streak.current)
        assertEquals(4, streak.longest)
    }

    @Test
    fun aMissedDueDayBreaksTheRunButTodayDoesNot() {
        // Wednesday the 4th is missed; Friday and Monday start a new run.
        val broken = Streaks.of(listOf(day(2), day(6), day(9)), today = day(9), schedule = mwf)
        assertEquals(2, broken.current)
        // Wednesday the 11th is today and not logged yet: the run still stands.
        val waiting = Streaks.of(listOf(day(2), day(4), day(6), day(9)), today = day(11), schedule = mwf)
        assertEquals(4, waiting.current)
    }

    @Test
    fun aWeeklyTargetCountsWeeksMet() {
        val three = Schedule(timesPerWeek = 3)
        // Week of the 2nd: three days. Week of the 9th: three days. This week (16th): one so far.
        val days = listOf(day(2), day(3), day(5), day(9), day(12), day(14), day(16))
        val streak = Streaks.of(days, today = day(17), schedule = three)
        assertTrue(streak.inWeeks)
        assertEquals(2, streak.current)
    }

    @Test
    fun weeklyTargetsStayDueUntilMet() {
        val two = Schedule(timesPerWeek = 2)
        assertTrue(two.isDue(day(4), setOf(day(2))))
        assertFalse(two.isDue(day(4), setOf(day(2), day(3))))
        // A day already logged stays due, so it counts as done today.
        assertTrue(two.isDue(day(3), setOf(day(2), day(3))))
    }

    @Test
    fun todayCountsOnlyWhatIsDue() {
        val schedules = mapOf("gym" to mwf, "water" to Schedule.DAILY)
        // Tuesday: only water is due.
        val progress = Today.progress(schedules, mapOf("water" to listOf(day(3))), day(3))
        assertEquals(TodayProgress(logged = 1, total = 1), progress)
    }

    @Test
    fun storedMasksRoundTripAndBadOnesFallBackToDaily() {
        assertEquals(mwf, Schedule.of(mwf.daysMask, 0))
        assertEquals(Schedule.AS_NEEDED, Schedule.of(0, 0))
        assertEquals(Schedule.DAILY, Schedule.of(127, 9))
    }

    @Test
    fun asNeededIsNeverDueAndKeepsNoStreak() {
        assertFalse(Schedule.AS_NEEDED.isDue(day(3), setOf(day(3))))
        assertEquals(0, Streaks.of(listOf(day(2), day(3)), today = day(3), schedule = Schedule.AS_NEEDED).current)
        val progress = Today.progress(mapOf("bike" to Schedule.AS_NEEDED), mapOf("bike" to listOf(day(3))), day(3))
        assertEquals(TodayProgress(logged = 0, total = 0), progress)
    }
}
