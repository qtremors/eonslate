package dev.qtremors.eonslate.feature.welcome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeState(
    /** 0 shows what Eonslate is; 1 offers trackers to start with. */
    val page: Int = 0,
    /** Template ids picked to start with, in the order they were picked. */
    val picked: List<String> = emptyList(),
    val saving: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val trackers: TrackerRepository,
    private val settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WelcomeState(
            page = savedState[KEY_PAGE] ?: 0,
            picked = savedState.get<ArrayList<String>>(KEY_PICKED).orEmpty(),
        )
    )
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    fun showPage(page: Int) {
        _state.update { it.copy(page = page.coerceIn(0, 1)) }
        savedState[KEY_PAGE] = _state.value.page
    }

    fun toggle(templateId: String) {
        _state.update { s -> s.copy(picked = if (templateId in s.picked) s.picked - templateId else s.picked + templateId) }
        savedState[KEY_PICKED] = ArrayList(_state.value.picked)
    }

    /**
     * Saves the starters [build] makes for the chosen currency (the picked templates, built by the
     * screen in the device's words) after every existing tracker, then marks the welcome as seen.
     * Skipping builds an empty list.
     */
    fun finish(build: (currency: String) -> List<Tracker>) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val starters = build(settings.tracking.first().currencyCode())
            val first = trackers.nextPosition()
            val now = clock.nowMillis()
            starters.forEachIndexed { index, tracker ->
                trackers.saveTracker(tracker.copy(position = first + index, createdAt = now + index, updatedAt = now + index))
            }
            settings.setWelcomeSeen(true)
            _state.update { it.copy(saving = false, done = true) }
        }
    }

    private companion object {
        const val KEY_PAGE = "welcome.page"
        const val KEY_PICKED = "welcome.picked"
    }
}
