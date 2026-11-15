package dev.qtremors.eonslate.feature.trackerdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.qtremors.eonslate.core.settings.SettingsRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.YearMonth
import javax.inject.Inject

enum class DetailView { LIST, CALENDAR, INSIGHTS }

data class DetailState(
    val loading: Boolean = true,
    /** True when the tracker no longer exists, such as after it was deleted elsewhere. */
    val missing: Boolean = false,
    val tracker: Tracker? = null,
    /** The newest page of entries; [hasMore] says whether older ones exist. */
    val entries: List<Entry> = emptyList(),
    /** When every entry happened, for totals and the calendar. */
    val times: List<EntryTime> = emptyList(),
    /** Every entry of [selectedDay], however far back; null when no day is picked. */
    val dayEntries: List<Entry>? = null,
    /** Every entry with its values, read only while insights are shown; null until then. */
    val allEntries: List<Entry>? = null,
    /** The day weeks start on, from settings or the device's locale. */
    val firstDay: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
    val view: DetailView = DetailView.LIST,
    val month: YearMonth = YearMonth.now(),
    /** A day picked on the calendar; the list below it shows only that day. */
    val selectedDay: LocalDate? = null,
) {
    val hasMore: Boolean get() = selectedDay == null && times.size > entries.size

    val groups: List<DayGroup>
        get() = DetailModels.groupByDay(if (selectedDay == null) entries else dayEntries.orEmpty())

    /** The entry with [id] among those shown, for its detail sheet. */
    fun find(id: String): Entry? = entries.firstOrNull { it.id == id } ?: dayEntries?.firstOrNull { it.id == id }
}

/** Something to say once, such as "Entry deleted" with a way to take it back. */
sealed interface DetailEvent {
    data class Deleted(val entry: Entry) : DetailEvent
}

@HiltViewModel
class TrackerDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val trackers: TrackerRepository,
    private val entries: EntryRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val trackerId: String = checkNotNull(savedState[ARG_TRACKER_ID]) { "TrackerDetail needs a tracker" }

    private data class Ui(val view: DetailView, val month: YearMonth, val day: LocalDate?)

    private val ui = MutableStateFlow(
        Ui(
            view = savedState.get<String>(KEY_VIEW)?.let { runCatching { DetailView.valueOf(it) }.getOrNull() } ?: DetailView.LIST,
            month = YearMonth.now(),
            day = null,
        )
    )
    private val handle = savedState

    private val events = Channel<DetailEvent>(Channel.BUFFERED)

    /** One-off messages for the screen; each is delivered once. */
    val eventFlow: Flow<DetailEvent> = events.receiveAsFlow()

    private val limit = MutableStateFlow(PAGE)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recent: Flow<List<Entry>> = limit.flatMapLatest { entries.observeRecentEntries(trackerId, it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayEntries: Flow<List<Entry>?> = ui.map { it.day }.distinctUntilChanged().flatMapLatest { day ->
        if (day == null) flowOf(null)
        else dayWindow(day).let { window ->
            entries.observeEntriesBetween(trackerId, window.first, window.last + 1).map { list -> list.filter { it.day() == day } }
        }
    }

    /** The whole history, only while insights need it, so the list and calendar stay light. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val insightEntries: Flow<List<Entry>?> = ui.map { it.view }.distinctUntilChanged().flatMapLatest { view ->
        if (view == DetailView.INSIGHTS) entries.observeEntries(trackerId) else flowOf(null)
    }

    private val extra: Flow<Triple<List<Entry>?, List<Entry>?, DayOfWeek>> =
        combine(dayEntries, insightEntries, settings.tracking) { day, all, tracking -> Triple(day, all, tracking.firstDayOfWeek()) }

    val state: StateFlow<DetailState> = combine(
        trackers.observeTracker(trackerId),
        entries.observeEntryTimes(trackerId),
        recent,
        extra,
        ui,
    ) { tracker, times, list, (day, all, firstDay), u ->
        if (tracker == null) DetailState(loading = false, missing = true)
        else DetailState(
            loading = false,
            tracker = tracker,
            entries = list,
            times = times,
            dayEntries = day,
            allEntries = all,
            firstDay = firstDay,
            view = u.view,
            month = u.month,
            selectedDay = u.day,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    /** Loads one more page of older entries. */
    fun showOlder() = limit.update { it + PAGE }

    fun setView(view: DetailView) {
        ui.update { it.copy(view = view, day = if (view == DetailView.LIST) null else it.day) }
        handle[KEY_VIEW] = view.name
    }

    fun showMonth(month: YearMonth) = ui.update { it.copy(month = month, day = it.day?.takeIf { d -> YearMonth.from(d) == month }) }

    /** Picks a day on the calendar, or clears the pick when the same day is tapped again. */
    fun selectDay(day: LocalDate) = ui.update { it.copy(day = if (it.day == day) null else day) }

    /** Deletes [entry] at once and offers to take it back. */
    fun delete(entry: Entry) {
        viewModelScope.launch {
            entries.deleteEntry(entry.id)
            events.send(DetailEvent.Deleted(entry))
        }
    }

    /** Puts a deleted entry back exactly as it was, values, time and all. */
    fun undoDelete(entry: Entry) {
        viewModelScope.launch { entries.saveEntry(entry) }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { trackers.setArchived(trackerId, archived) }
    }

    companion object {
        const val ARG_TRACKER_ID = "trackerId"

        /** Entries per page: a few weeks of daily logging, enough to fill the screen many times over. */
        const val PAGE = 60
        private const val KEY_VIEW = "detail.view"
    }
}
