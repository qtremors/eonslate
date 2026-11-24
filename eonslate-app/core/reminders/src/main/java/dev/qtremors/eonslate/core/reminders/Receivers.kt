package dev.qtremors.eonslate.core.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderRepository
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderTimes
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The extra MainActivity reads to open the log screen of a tracker. */
const val EXTRA_LOG_TRACKER = "log_tracker"

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Runs [block] off the main thread while keeping the broadcast alive until it ends. */
private fun BroadcastReceiver.work(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/**
 * A reminder is due: post it unless it no longer applies, then always schedule the next one, so
 * a reminder never stops after its first alert.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var reminders: ReminderRepository
    @Inject lateinit var trackers: TrackerRepository
    @Inject lateinit var entries: EntryRepository
    @Inject lateinit var sync: ReminderSync

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderSync.EXTRA_ID) ?: return
        work {
            try {
                val reminder = reminders.getReminder(id)?.takeIf { it.enabled }
                val tracker = reminder?.let { trackers.getTracker(it.trackerId) }?.takeIf { !it.archived }
                val logged = reminder != null && reminder.skipIfLogged &&
                    ReminderTimes.loggedOn(entries.getLatestEntry(reminder.trackerId), LocalDate.now())
                if (tracker != null && !logged) Notifier.post(context, id, tracker.id, tracker.name)
            } finally {
                sync.sync()
            }
        }
    }
}

/** The clock, the zone or the app changed, or the phone restarted: alarms are set again. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var sync: ReminderSync

    override fun onReceive(context: Context, intent: Intent) {
        work { sync.sync() }
    }
}

internal object Notifier {
    private const val CHANNEL = "reminders"

    fun post(context: Context, reminderId: String, trackerId: String, trackerName: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(EXTRA_LOG_TRACKER, trackerId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } ?: return
        val tap = PendingIntent.getActivity(
            context, reminderId.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(trackerName)
            .setContentText(context.getString(R.string.reminders_time_to_log))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
        } catch (_: SecurityException) {
            // The permission was revoked between the check and the post.
        }
    }
}
