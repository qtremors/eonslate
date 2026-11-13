package dev.qtremors.eonslate.core.navigation.api

import kotlinx.serialization.Serializable

/** Top-level places in the app. Each feature owns the screen for its destination. */
@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination
}
