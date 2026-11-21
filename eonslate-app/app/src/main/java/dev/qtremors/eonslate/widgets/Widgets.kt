package dev.qtremors.eonslate.widgets

import dev.qtremors.eonslate.core.tracker.domain.insights.Intervals
import dev.qtremors.eonslate.core.tracker.domain.insights.Goals
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.eonslate.R
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.MoneySummary
import dev.qtremors.eonslate.core.tracker.domain.insights.Today
import dev.qtremors.eonslate.core.tracker.domain.insights.localDay
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.quicklog.QuickLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Runs a widget's drawing off the main thread, holding the broadcast open until it is done. */
private fun AppWidgetProvider.drawLater(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** The trackers due today, unlogged first; a tap on one logs it, a tap on the title opens the app. */
@AndroidEntryPoint
class TodayWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var trackers: TrackerRepository
    @Inject lateinit var entries: EntryRepository
    @Inject lateinit var settings: SettingsRepository

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = drawLater {
        val summaries = trackers.observeSummaries().first()
        val weekStart = settings.tracking.first().firstDayOfWeek()
        val today = LocalDate.now()
        val logged = entries.observeEntryTimesByTracker().first().mapValues { (_, list) -> list.map { it.localDay() } }
        // As on the Trackers tab: with daily goals a day is done once they are met, and an
        // interval that has come round makes a tracker due.
        val valued = summaries.map { it.tracker }.filter { Goals.fields(it).isNotEmpty() || it.intervals.isNotEmpty() }
            .associateWith { entries.observeEntries(it.id).first() }
        val days = logged + valued.map { (tracker, list) -> tracker.id to Goals.doneDays(tracker, list).toList() }
        val intervalDue = valued.filter { (tracker, list) -> Intervals.status(tracker, list, today).any { it.due } }.keys.map { it.id }
        val schedules = summaries.associate { it.tracker.id to it.tracker.schedule }
        val due = Today.due(schedules, days, today, weekStart) + intervalDue
        val progress = Today.progress(due, days, today)
        val rows = summaries.filter { it.tracker.id in due }
            .map { it.tracker to (today in days[it.tracker.id].orEmpty()) }
            .sortedBy { it.second }
        val slots = listOf(R.id.today_row_1, R.id.today_row_2, R.id.today_row_3, R.id.today_row_4)
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            views.setOnClickPendingIntent(R.id.today_title, QuickLog.pending(context, id * 10, QuickLog.log(context)))
            views.setTextViewText(
                R.id.today_progress,
                if (progress.total == 0) context.getString(R.string.widget_today_none)
                else context.getString(R.string.widget_today_progress, progress.logged, progress.total),
            )
            slots.forEachIndexed { index, slot ->
                val row = rows.getOrNull(index)
                if (row == null) {
                    views.setViewVisibility(slot, View.GONE)
                } else {
                    val (tracker, logged) = row
                    views.setViewVisibility(slot, View.VISIBLE)
                    views.setTextViewText(slot, context.getString(if (logged) R.string.widget_today_done else R.string.widget_today_todo, tracker.name))
                    views.setTextColor(slot, if (logged) 0xB3FFFFFF.toInt() else 0xFFFFFFFF.toInt())
                    views.setOnClickPendingIntent(slot, QuickLog.pending(context, id * 10 + index + 1, QuickLog.log(context, tracker.id)))
                }
            }
            val more = rows.size - slots.size
            views.setViewVisibility(R.id.today_more, if (more > 0) View.VISIBLE else View.GONE)
            if (more > 0) views.setTextViewText(R.id.today_more, context.getString(R.string.widget_today_more, more))
            manager.updateAppWidget(id, views)
        }
    }
}

/** The balance and this month's spending, in the currency the app shows; a tap opens the app. */
@AndroidEntryPoint
class MoneyWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var finance: FinanceRepository
    @Inject lateinit var settings: SettingsRepository

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = drawLater {
        val ledger = finance.observeLedger().first()
        val chosen = settings.tracking.first().currencyCode()
        val used = ledger.accounts.map { it.currency }.distinct()
        // The same currency the app's tabs show: the chosen one when an account uses it.
        val currency = if (used.isEmpty() || chosen in used) chosen else used.first()
        val summary = MoneySummary.of(ledger, LocalDate.now(), currency)
        val locale = settings.display.first().numbers.locale()
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_money)
            views.setOnClickPendingIntent(R.id.money_root, QuickLog.pending(context, id * 10, QuickLog.log(context)))
            if (summary != null) {
                views.setTextViewText(R.id.money_balance, ValueFormat.money(summary.balance, currency, locale))
                views.setTextViewText(R.id.money_month, context.getString(R.string.widget_money_month, ValueFormat.money(summary.monthExpense, currency, locale)))
            }
            manager.updateAppWidget(id, views)
        }
    }
}

/** Redraws the widgets whenever trackers, entries or money change, so they never show stale days. */
@Singleton
class WidgetRefresh @Inject constructor(
    private val trackers: TrackerRepository,
    private val entries: EntryRepository,
    private val finance: FinanceRepository,
) {
    @OptIn(FlowPreview::class)
    fun start(context: Context, scope: CoroutineScope) {
        scope.launch {
            // The first reading is what the widgets already show; redrawing then only slows the launch.
            combine(trackers.observeSummaries(), entries.observeEntryTimesByTracker(), finance.observeLedger()) { _, _, _ -> }
                .drop(1)
                .debounce(500)
                .collect { refresh(context) }
        }
    }

    private fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(TodayWidgetProvider::class.java, MoneyWidgetProvider::class.java).forEach { provider ->
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, provider).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }
}
