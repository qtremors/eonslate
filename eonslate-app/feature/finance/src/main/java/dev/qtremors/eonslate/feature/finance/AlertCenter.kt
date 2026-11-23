package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.domain.finance.OverallFloor
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.insights.Alert
import dev.qtremors.eonslate.core.tracker.domain.insights.Alerts
import dev.qtremors.eonslate.core.tracker.domain.insights.IntervalStatus
import dev.qtremors.eonslate.core.tracker.domain.insights.Intervals
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the bell shows, read once for the whole app: payments found in SMS, payments waiting,
 * budgets over, intervals come round, and payments coming up.
 */
@Singleton
class AlertCenter @Inject constructor(
    finance: FinanceRepository,
    trackers: TrackerRepository,
    entries: EntryRepository,
    detection: SmsDetection,
    private val clock: AppClock,
    settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun today(): LocalDate = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.of(clock.zoneId())).toLocalDate()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val intervals: Flow<List<Pair<Tracker, List<IntervalStatus>>>> = trackers.observeSummaries()
        .map { list -> list.map { it.tracker }.filter { it.intervals.isNotEmpty() } }
        .distinctUntilChanged()
        .flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(list.map { tracker -> entries.observeEntries(tracker.id).map { tracker to Intervals.status(tracker, it, today()) } }) { it.toList() }
        }

    val alerts: StateFlow<List<Alert>?> = combine(finance.observeLedger(), detection.pending, intervals, settings.tracking) { ledger, pending, statuses, tracking ->
        Alerts.of(ledger, today(), pending.size, statuses, tracking.overallFloor?.let { OverallFloor(tracking.currencyCode(), it) })
    }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    /** The badge: everything that wants doing, plus an SMS batch result not yet seen. */
    val count: Flow<Int> = combine(alerts, detection.result) { list, result ->
        (list?.count { it.counts } ?: 0) + if (result != null) 1 else 0
    }.distinctUntilChanged()

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
