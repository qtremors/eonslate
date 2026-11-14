package dev.qtremors.eonslate.core.navigation.api

import kotlinx.serialization.Serializable

/** Top-level places in the app. Each feature owns the screen for its destination. */
@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination

    /** Creates a tracker when [trackerId] is null, or edits the tracker with that id. */
    @Serializable
    data class TrackerEditor(val trackerId: String? = null) : Destination

    /** Logs a new entry to [trackerId], or edits the entry [entryId] when it is set. */
    @Serializable
    data class LogEntry(val trackerId: String, val entryId: String? = null) : Destination
}
