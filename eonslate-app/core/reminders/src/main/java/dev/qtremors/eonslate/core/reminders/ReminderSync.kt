package dev.qtremors.eonslate.core.reminders

import java.time.LocalDate
import dev.qtremors.eonslate.core.tracker.domain.insights.localDay
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import androidx.core.app.NotificationManagerCompat
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderRepository
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderTimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps exactly one alarm per active reminder, at its next due time. Idempotent: every run sets
 * each alarm again and cancels the ones whose reminder is gone, disabled or archived, so it is
 * safe after any change, an alarm, a reboot or a clock change.
 */
@Singleton
class ReminderSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminders: ReminderRepository,
    private val entries: EntryRepository,
) {
    private val lock = Mutex()
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("reminder_alarms", Context.MODE_PRIVATE)

    /** Follows the database for the life of the process, so every edit reschedules. */
    fun start(scope: CoroutineScope) {
        scope.launch { reminders.observeActive().collect { sync() } }
        // A reminder that already went off is cleared once its tracker is logged today, from
        // anywhere: the app, the widget or the tile.
        scope.launch {
            entries.observeEntryTimesByTracker().collect { times ->
                val today = LocalDate.now()
                val logged = times.filterValues { list -> list.any { it.localDay() == today } }.keys
                if (logged.isEmpty()) return@collect
                val shade = NotificationManagerCompat.from(context)
                reminders.getActive().filter { it.trackerId in logged }.forEach { shade.cancel(it.id.hashCode()) }
            }
        }
    }

    suspend fun sync(now: ZonedDateTime = ZonedDateTime.now()) = lock.withLock {
        val active = reminders.getActive()
        val scheduled = HashSet<String>()
        active.forEach { reminder ->
            val due = ReminderTimes.next(reminder, now) ?: return@forEach
            val at = due.toInstant().toEpochMilli()
            // Plain inexact alarms may drift up to an hour on a day-ahead time, so: exact when
            // allowed, else a 10 minute window (the shortest Android grants).
            if (exactAllowed(context)) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(reminder.id))
            } else {
                alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, intent(reminder.id))
            }
            scheduled += reminder.id
        }
        prefs.getStringSet(KEY, emptySet()).orEmpty().filterNot { it in scheduled }.forEach { alarms.cancel(intent(it)) }
        prefs.edit { putStringSet(KEY, scheduled) }
    }

    private fun intent(id: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        // The data makes each reminder's intent distinct, so alarms never replace each other.
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).setData("eonslate://reminder/$id".toUri()).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /** True when reminders can fire on the minute; Android 12 and up let the user turn it off. */
        fun exactAllowed(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

        internal const val WINDOW_MILLIS = 10 * 60_000L
        internal const val KEY = "scheduled"
        internal const val ACTION_FIRE = "dev.qtremors.eonslate.REMINDER"
        internal const val EXTRA_ID = "reminder_id"
    }
}
