package dev.qtremors.eonslate.core.navigation.api

import kotlinx.serialization.Serializable

/** Top-level places in the app. Each feature owns the screen for its destination. */
@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination

    /** Trackers put away from Home. */
    @Serializable
    data object Archived : Destination

    /** One tracker's page: its entries by day or on a calendar. */
    @Serializable
    data class TrackerDetail(val trackerId: String) : Destination

    /**
     * Creates a tracker when [trackerId] is null, filled in from [templateId] when that is set, or
     * edits the tracker with that id.
     */
    @Serializable
    data class TrackerEditor(val trackerId: String? = null, val templateId: String? = null) : Destination

    /** A blank tracker and every template, where a new tracker starts. */
    @Serializable
    data object Templates : Destination

    /** The first pages, shown once. */
    @Serializable
    data object Welcome : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object About : Destination

    @Serializable
    data object Licenses : Destination

    @Serializable
    data object Privacy : Destination

    /** Logs a new entry to [trackerId], or edits the entry [entryId] when it is set. */
    @Serializable
    data class LogEntry(val trackerId: String, val entryId: String? = null) : Destination
}
