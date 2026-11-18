package dev.qtremors.eonslate.feature.settings

import dev.qtremors.eonslate.core.settings.SmsSettings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.settings.AccentChoice
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.settings.ThemeChoice
import dev.qtremors.eonslate.core.settings.Tracking
import dev.qtremors.eonslate.core.settings.WeekStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Every setting applies the moment it changes; there is no Save. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    /** Null until the stored settings are read, so the page never shows defaults first. */
    val appearance: StateFlow<Appearance?> = settings.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun update(change: (Appearance) -> Appearance) {
        viewModelScope.launch { settings.updateAppearance(change) }
    }

    fun setTheme(theme: ThemeChoice) = update { it.copy(theme = theme) }
    fun setPureBlack(on: Boolean) = update { it.copy(pureBlack = on) }
    fun setAccent(accent: AccentChoice) = update { it.copy(accent = accent) }

    /** Picking a preset also switches back from wallpaper colors, since that is what was asked for. */
    fun setPreset(name: String) = update { it.copy(preset = name, accent = AccentChoice.PRESET) }
    fun setHaptics(on: Boolean) = update { it.copy(haptics = on) }
    fun setReduceMotion(on: Boolean) = update { it.copy(reduceMotion = on) }
    fun setBlockScreenshots(on: Boolean) = update { it.copy(blockScreenshots = on) }

    val sms: StateFlow<SmsSettings?> = settings.sms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSms(change: (SmsSettings) -> SmsSettings) {
        viewModelScope.launch { settings.updateSms(change) }
    }

    /** Null until read, like [appearance]. */
    val tracking: StateFlow<Tracking?> = settings.tracking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWeekStart(start: WeekStart) {
        viewModelScope.launch { settings.updateTracking { it.copy(weekStart = start) } }
    }

    /** A known three-letter code, or null to follow the device again. */
    fun setCurrency(code: String?) {
        viewModelScope.launch { settings.updateTracking { it.copy(currency = code?.takeIf(Tracking::isCurrency)) } }
    }
}
