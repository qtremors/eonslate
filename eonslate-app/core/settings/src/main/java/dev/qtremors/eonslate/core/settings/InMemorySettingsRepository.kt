package dev.qtremors.eonslate.core.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Settings held in memory, for previews and tests; the app itself uses DataStore. */
class InMemorySettingsRepository(
    appearance: Appearance = Appearance(),
    tracking: Tracking = Tracking(),
    welcomeSeen: Boolean = false,
    display: DisplayPreferences = DisplayPreferences(),
) : SettingsRepository {
    val appearanceState = MutableStateFlow(appearance)
    val trackingState = MutableStateFlow(tracking)
    val welcomeState = MutableStateFlow(welcomeSeen)
    private val homeTagState = MutableStateFlow<String?>(null)

    override val welcomeSeen: Flow<Boolean> = welcomeState
    override suspend fun setWelcomeSeen(seen: Boolean) {
        welcomeState.value = seen
    }

    override val appearance: Flow<Appearance> = appearanceState
    override suspend fun updateAppearance(change: (Appearance) -> Appearance) = appearanceState.update(change)

    override val homeTag: Flow<String?> = homeTagState
    override suspend fun setHomeTag(tag: String?) {
        homeTagState.value = tag
    }

    override val tracking: Flow<Tracking> = trackingState
    override suspend fun updateTracking(change: (Tracking) -> Tracking) = trackingState.update(change)

    private val displayState = MutableStateFlow(display)
    override val display: Flow<DisplayPreferences> = displayState
    override suspend fun updateDisplay(change: (DisplayPreferences) -> DisplayPreferences) = displayState.update {
        val next = change(it)
        next.copy(name = next.name.trim().take(DisplayPreferences.NAME_LIMIT))
    }

    private val smsState = kotlinx.coroutines.flow.MutableStateFlow(SmsSettings())
    override val sms: kotlinx.coroutines.flow.Flow<SmsSettings> = smsState

    override suspend fun updateSms(change: (SmsSettings) -> SmsSettings) {
        smsState.value = change(smsState.value)
    }
}
