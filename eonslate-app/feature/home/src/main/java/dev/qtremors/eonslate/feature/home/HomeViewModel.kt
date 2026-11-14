package dev.qtremors.eonslate.feature.home

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: TrackerRepository,
    private val entries: EntryRepository,
    private val clock: AppClock,
    private val ids: IdGenerator,
) : ViewModel() {
    /** Active trackers in home order; null until the first read, so loading never looks empty. */
    val trackers: StateFlow<List<TrackerSummary>?> = repository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

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

    companion object {
        /** Keeps the query alive across a rotation, which takes well under this. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** True when one tap can log this tracker without opening the log screen. */
        fun isQuickLog(tracker: Tracker): Boolean =
            tracker.fields.singleOrNull()?.type == FieldType.YES_NO
    }
}
