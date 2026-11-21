package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class ConsistencyTest {
    // 2026-11-02 is a Monday.
    private fun day(d: Int) = LocalDate.of(2026, 11, d)

    @Test
    fun dailyCountsDueDaysAndTodayOnlyOnceDone() {
        val c = ConsistencyOf.of(Schedule.DAILY, setOf(day(2), day(3), day(5)), start = day(2), today = day(6))!!
        // 2nd to 5th are due (4 days), today the 6th is not done yet so it does not count.
        assertEquals(3, c.done)
        assertEquals(4, c.due)
        assertEquals(75, c.percent)
    }

    @Test
    fun weekdaysCountOnlyTheirOwnDays() {
        val mwf = Schedule(setOf(MONDAY, WEDNESDAY, FRIDAY))
        val c = ConsistencyOf.of(mwf, setOf(day(2), day(6)), start = day(2), today = day(8))!!
        assertEquals(2, c.done)
        assertEquals(3, c.due)
    }

    @Test
    fun weeklyTargetsCountWeeksAndAsNeededHasNone() {
        val two = Schedule(timesPerWeek = 2)
        val c = ConsistencyOf.of(two, setOf(day(2), day(4), day(9)), start = day(2), today = day(10))!!
        // Week of the 2nd met; this week (9th) has one so far and is still running.
        assertEquals(1, c.done)
        assertEquals(1, c.due)
        assertNull(ConsistencyOf.of(Schedule.AS_NEEDED, setOf(day(2)), start = day(2), today = day(10)))
    }

    @Test
    fun heatLevelsSplitByQuartile() {
        val levels = HeatLevels.of(mapOf(day(1) to 1.0, day(2) to 2.0, day(3) to 3.0, day(4) to 400.0, day(5) to 0.0))
        assertEquals(setOf(day(1), day(2), day(3), day(4)), levels.keys)
        assertEquals(4, levels[day(4)])
        assertEquals(1, levels[day(1)])
        assertEquals(mapOf(day(1) to 4, day(2) to 4), HeatLevels.of(mapOf(day(1) to 1.0, day(2) to 1.0)))
    }
}

class WeekdaysTest {
    private fun day(d: Int) = java.time.LocalDate.of(2026, 11, d)

    @org.junit.Test
    fun totalsFollowTheWeekStartAndFindTheBusiestDay() {
        // The 2nd and 9th are Mondays, the 8th a Sunday.
        val totals = Weekdays.totals(mapOf(day(2) to 2.0, day(9) to 3.0, day(8) to 1.0), java.time.DayOfWeek.SUNDAY)
        org.junit.Assert.assertEquals(java.time.DayOfWeek.SUNDAY, totals.first().first)
        org.junit.Assert.assertEquals(5.0, totals[1].second, 0.0)
        org.junit.Assert.assertEquals(java.time.DayOfWeek.MONDAY, Weekdays.busiest(totals))
        org.junit.Assert.assertNull(Weekdays.busiest(Weekdays.totals(emptyMap())))
    }
}
