package dev.qtremors.eonslate.quicklog

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import dev.qtremors.eonslate.MainActivity
import dev.qtremors.eonslate.core.reminders.EXTRA_LOG_TRACKER

internal object QuickLog {
    private const val PICK_TRACKER = "dev.qtremors.eonslate.action.PICK_TRACKER"

    fun picker(context: Context): Intent =
        Intent(context, TrackerPickerActivity::class.java).setAction(PICK_TRACKER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isPicker(intent: Intent) = intent.action == PICK_TRACKER

    fun configure(context: Context, widgetId: Int): Intent =
        Intent(context, TrackerPickerActivity::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun log(context: Context, trackerId: String? = null): Intent =
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (trackerId != null) putExtra(EXTRA_LOG_TRACKER, trackerId) }

    fun pending(context: Context, request: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
