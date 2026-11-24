package dev.qtremors.eonslate.core.tracker.domain.reminders

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderTimes {
    /**
     * The first time strictly after [now] that [reminder] is due, or null when it has no days.
     * Strictly after, so an alarm firing on the minute schedules the next one, not itself.
     * A time inside a DST gap moves forward by the gap, as [ZonedDateTime.of] does.
     */
    fun next(reminder: Reminder, now: ZonedDateTime): ZonedDateTime? {
        if (reminder.days.isEmpty()) return null
        val time = LocalTime.of(reminder.minuteOfDay / 60, reminder.minuteOfDay % 60)
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek !in reminder.days) continue
            val due = ZonedDateTime.of(date, time, now.zone)
            if (due.isAfter(now)) return due
        }
        return null
    }

    /** True when [latest] happened on [day], counted in the zone it was logged in. */
    fun loggedOn(latest: Entry?, day: LocalDate): Boolean =
        latest != null && Instant.ofEpochMilli(latest.occurredAt).atZone(ZoneId.of(latest.zoneId)).toLocalDate() == day
}

/** Reminders, kept apart from trackers so a reminder change never rewrites a tracker. */
interface ReminderRepository {
    fun observeReminders(trackerId: String): Flow<List<Reminder>>

    /** Enabled reminders of trackers that are not archived: exactly what should have an alarm. */
    fun observeActive(): Flow<List<Reminder>>

    suspend fun getActive(): List<Reminder>

    suspend fun getReminder(id: String): Reminder?

    suspend fun saveReminder(reminder: Reminder)

    suspend fun deleteReminder(id: String)
}
