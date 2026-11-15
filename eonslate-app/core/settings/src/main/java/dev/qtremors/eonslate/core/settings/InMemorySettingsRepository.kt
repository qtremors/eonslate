package dev.qtremors.eonslate.core.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Settings held in memory, for previews and tests; the app itself uses DataStore. */
class InMemorySettingsRepository(
    appearance: Appearance = Appearance(),
    tracking: Tracking = Tracking(),
    welcomeSeen: Boolean = false,
) : SettingsRepository {
    val appearanceState = MutableStateFlow(appearance)
    val trackingState = MutableStateFlow(tracking)
    val welcomeState = MutableStateFlow(welcomeSeen)

    override val welcomeSeen: Flow<Boolean> = welcomeState
    override suspend fun setWelcomeSeen(seen: Boolean) {
        welcomeState.value = seen
    }

    override val appearance: Flow<Appearance> = appearanceState
    override suspend fun updateAppearance(change: (Appearance) -> Appearance) = appearanceState.update(change)

    override val tracking: Flow<Tracking> = trackingState
    override suspend fun updateTracking(change: (Tracking) -> Tracking) = trackingState.update(change)
}
