package dev.qtremors.eonslate.core.tracker.domain.model

import java.time.DayOfWeek

/**
 * A nudge to log a tracker at a wall-clock time in the phone's current zone.
 *
 * @property minuteOfDay 0 until 1440; 8:30 is 510.
 * @property skipIfLogged no notification when the tracker already has an entry that day.
 */
data class Reminder(
    val id: String,
    val trackerId: String,
    val minuteOfDay: Int,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val enabled: Boolean = true,
    val skipIfLogged: Boolean = true,
    val createdAt: Long = 0,
) {
    init {
        require(minuteOfDay in 0 until MINUTES_PER_DAY) { "minuteOfDay $minuteOfDay" }
    }

    companion object {
        const val MINUTES_PER_DAY = 1440

        /** Monday is bit 0, Sunday bit 6. */
        fun daysToMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

        fun maskToDays(mask: Int): Set<DayOfWeek> =
            DayOfWeek.entries.filterTo(LinkedHashSet()) { mask and (1 shl (it.value - 1)) != 0 }
    }
}
