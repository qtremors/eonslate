package dev.qtremors.eonslate.feature.home

import dev.qtremors.eonslate.core.settings.Tracking
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.insights.Streaks
import dev.qtremors.eonslate.core.tracker.domain.insights.Today
import dev.qtremors.eonslate.core.tracker.domain.insights.TodayProgress
import dev.qtremors.eonslate.core.tracker.domain.insights.localDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val entries: EntryRepository,
    private val clock: AppClock,
    private val ids: IdGenerator,
    private val settings: SettingsRepository,
) : ViewModel() {
    /** Active trackers in home order; null until the first read, so loading never looks empty. */
    val trackers: StateFlow<List<TrackerSummary>?> = repository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val homeTag = settings.homeTag

    fun setHomeTag(tag: String?) {
        viewModelScope.launch { settings.setHomeTag(tag) }
    }

    // The device's week start until the setting is read, so nothing waits on it.
    private val weekStart = settings.tracking.map { it.firstDayOfWeek() }.onStart { emit(Tracking().firstDayOfWeek()) }.distinctUntilChanged()
    private val schedules = repository.observeSummaries().map { list -> list.associate { it.tracker.id to it.tracker.schedule } }
    private val loggedDays = entries.observeEntryTimesByTracker().map { times -> times.mapValues { (_, list) -> list.map { it.localDay() } } }

    /** Trackers, the days each was logged, and the week start, read together. */
    private val logged = combine(schedules, loggedDays, weekStart, ::Logged)

    /**
     * Each tracker's current streak under its schedule, by id; trackers without one are left out.
     * It does not wait for the tracker list: until it arrives, every tracker counts as daily.
     */
    val streaks: StateFlow<Map<String, Int>> = combine(schedules.onStart { emit(emptyMap()) }, loggedDays, weekStart, ::Logged)
        .map { (schedules, days, weekStart) ->
            val today = LocalDate.now(ZoneId.of(clock.zoneId()))
            days.mapValues { (id, logged) -> Streaks.of(logged, today, schedules[id] ?: Schedule.DAILY, weekStart).current }.filterValues { it > 0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    /** The trackers due today under their schedules. */
    val dueToday: StateFlow<Set<String>?> = logged
        .map { (schedules, days, weekStart) -> Today.due(schedules, days, LocalDate.now(ZoneId.of(clock.zoneId())), weekStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** How many of the trackers due today have an entry; null until the reads arrive. */
    val today: StateFlow<TodayProgress?> = logged
        .map { (schedules, days, weekStart) -> Today.progress(schedules, days, LocalDate.now(ZoneId.of(clock.zoneId())), weekStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private data class Logged(val schedules: Map<String, Schedule>, val days: Map<String, List<LocalDate>>, val weekStart: java.time.DayOfWeek)

    /** Logs "yes" now to a tracker that is a single yes or no, such as a daily habit. */
    fun quickLog(tracker: Tracker) {
        val field = tracker.fields.singleOrNull()?.takeIf { it.type == FieldType.YES_NO } ?: return
        val now = clock.nowMillis()
        viewModelScope.launch {
            entries.saveEntry(
                Entry(
                    id = ids.newId(),
                    trackerId = tracker.id,
                    occurredAt = now,
                    zoneId = clock.zoneId(),
                    values = mapOf(field.id to FieldValue.YesNo(true)),
                    createdAt = now,
                )
            )
        }
    }

    /** Moves [tracker] [by] places in the home order, keeping everyone else's order. */
    fun move(tracker: Tracker, by: Int) {
        val ids = trackers.value?.map { it.tracker.id } ?: return
        val order = moved(ids, tracker.id, by) ?: return
        viewModelScope.launch { repository.reorder(order) }
    }

    /** Saves a tracker built from a template after every other tracker. */
    fun createFrom(build: (currency: String) -> Tracker) {
        viewModelScope.launch {
            val tracker = build(settings.tracking.first().currencyCode())
            val now = clock.nowMillis()
            repository.saveTracker(tracker.copy(position = repository.nextPosition(), createdAt = now, updatedAt = now))
        }
    }

    /** Adds a copy of [tracker] named [name] after every other tracker, without its entries. */
    fun duplicate(tracker: Tracker, name: String) {
        viewModelScope.launch {
            repository.saveTracker(tracker.duplicate(ids::newId, name, clock.nowMillis(), repository.nextPosition()))
        }
    }

    fun setArchived(tracker: Tracker, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(tracker.id, archived) }
    }

    companion object {
        /** [ids] with [id] moved [by] places, clamped to the ends; null when nothing would change. */
        fun moved(ids: List<String>, id: String, by: Int): List<String>? {
            val from = ids.indexOf(id)
            if (from < 0) return null
            val to = (from + by).coerceIn(0, ids.lastIndex)
            if (to == from) return null
            return ids.toMutableList().apply { add(to, removeAt(from)) }
        }

        /** Keeps the query alive across a rotation, which takes well under this. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** True when one tap can log this tracker without opening the log screen. */
        fun isQuickLog(tracker: Tracker): Boolean =
            tracker.fields.singleOrNull()?.type == FieldType.YES_NO
    }
}
