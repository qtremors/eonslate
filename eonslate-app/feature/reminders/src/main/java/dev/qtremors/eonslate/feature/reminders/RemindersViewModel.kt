package dev.qtremors.eonslate.feature.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/** Null while loading. */
data class RemindersUiState(val trackerName: String, val reminders: List<Reminder>)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    savedState: SavedStateHandle,
    trackers: TrackerRepository,
    private val repository: ReminderRepository,
    private val clock: AppClock,
    private val ids: IdGenerator,
) : ViewModel() {
    private val trackerId: String = checkNotNull(savedState[ARG_TRACKER_ID]) { "Reminders need a tracker" }

    val state: StateFlow<RemindersUiState?> = combine(
        trackers.observeTracker(trackerId).map { it?.name.orEmpty() },
        repository.observeReminders(trackerId),
    ) { name, list -> RemindersUiState(name, list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves a new reminder when [id] is null; alarms follow through the database. */
    fun save(id: String?, minuteOfDay: Int, days: Set<DayOfWeek>, skipIfLogged: Boolean) {
        if (days.isEmpty()) return
        viewModelScope.launch {
            val existing = id?.let { repository.getReminder(it) }
            repository.saveReminder(
                existing?.copy(minuteOfDay = minuteOfDay, days = days, skipIfLogged = skipIfLogged)
                    ?: Reminder(ids.newId(), trackerId, minuteOfDay, days, enabled = true, skipIfLogged = skipIfLogged, createdAt = clock.nowMillis())
            )
        }
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch { repository.saveReminder(reminder.copy(enabled = enabled)) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }

    companion object {
        const val ARG_TRACKER_ID = "trackerId"
    }
}
