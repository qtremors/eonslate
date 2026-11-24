package dev.qtremors.eonslate.quicklog

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import dev.qtremors.eonslate.R

class LogWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, manager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val edit = preferences(context).edit()
        appWidgetIds.forEach { edit.remove(it.toString()) }
        edit.apply()
    }

    companion object {
        internal fun select(context: Context, manager: AppWidgetManager, id: Int, trackerId: String) {
            preferences(context).edit().putString(id.toString(), trackerId).apply()
            render(context, manager, id)
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val tracker = preferences(context).getString(id.toString(), null)
            val views = RemoteViews(context.packageName, R.layout.widget_quick_log)
            views.setTextViewText(R.id.widget_label, context.getText(if (tracker == null) R.string.quick_log_choose else R.string.quick_log_title))
            val intent = if (tracker == null) QuickLog.configure(context, id) else QuickLog.log(context, tracker)
            views.setOnClickPendingIntent(R.id.widget_root, QuickLog.pending(context, id, intent))
            manager.updateAppWidget(id, views)
        }

        private fun preferences(context: Context) = context.getSharedPreferences("quick_log_widgets", Context.MODE_PRIVATE)
    }
}
