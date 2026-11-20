package dev.qtremors.eonslate.core.tracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * When a tracker is due: every day, on chosen weekdays, or a number of days a week on any days.
 *
 * @property days the weekdays it is due on; all seven means every day. Ignored when
 *   [timesPerWeek] is set.
 * @property timesPerWeek how many days a week it should be logged, on any days; 0 means it
 *   follows [days] instead.
 */
data class Schedule(
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val timesPerWeek: Int = 0,
) {
    val isDaily: Boolean get() = timesPerWeek == 0 && days.size == DayOfWeek.entries.size
    val isWeekly: Boolean get() = timesPerWeek > 0

    fun isValid(): Boolean = timesPerWeek in 0..MAX_TIMES_PER_WEEK && (timesPerWeek > 0 || days.isNotEmpty())

    /**
     * Whether it is due on [day]. A weekly target is due any day until the week's target is met;
     * [loggedDays] are the days with an entry, and a day already logged stays due so it counts.
     */
    fun isDue(day: LocalDate, loggedDays: Set<LocalDate>, weekStart: DayOfWeek = DayOfWeek.MONDAY): Boolean {
        if (!isWeekly) return day.dayOfWeek in days
        if (day in loggedDays) return true
        val start = day.with(TemporalAdjusters.previousOrSame(weekStart))
        val done = loggedDays.count { !it.isBefore(start) && !it.isAfter(start.plusDays(6)) }
        return done < timesPerWeek
    }

    /** The weekdays as a bit mask, Monday the lowest bit, as reminders store them. */
    val daysMask: Int get() = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    companion object {
        val DAILY = Schedule()
        const val MAX_TIMES_PER_WEEK = 6
        const val ALL_DAYS_MASK = 127

        fun daysOf(mask: Int): Set<DayOfWeek> =
            DayOfWeek.entries.filterTo(LinkedHashSet()) { mask and (1 shl (it.value - 1)) != 0 }

        /** From the stored columns; anything unreadable falls back to every day. */
        fun of(daysMask: Int, timesPerWeek: Int): Schedule {
            val schedule = Schedule(daysOf(daysMask), timesPerWeek)
            return if (schedule.isValid()) schedule else DAILY
        }
    }
}
