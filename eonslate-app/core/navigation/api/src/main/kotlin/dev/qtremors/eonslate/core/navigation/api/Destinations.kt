package dev.qtremors.eonslate.core.navigation.api

import kotlinx.serialization.Serializable

/** Top-level places in the app. Each feature owns the screen for its destination. */
@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination

    /** Every tracker as a slate, with search and tags. */
    @Serializable
    data object Trackers : Destination

    /** Money in and out, with search and filters. */
    @Serializable
    data object Transactions : Destination

    /** Subscriptions and other recurring payments. */
    @Serializable
    data object Subscriptions : Destination

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

    /** The first pages, shown once. */
    @Serializable
    data object Welcome : Destination

    /** Everyone payments are with, who owes whom first. */
    @Serializable
    data object People : Destination

    /** One person or business's payments, as a chat. */
    @Serializable
    data class Party(val partyId: String) : Destination

    /** Payments found in SMS, payments to confirm, budgets over, and things due. */
    @Serializable
    data object Notifications : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object DisplaySettings : Destination

    @Serializable
    data object Categories : Destination

    /** The reminders of one tracker. */
    @Serializable
    data class Reminders(val trackerId: String) : Destination

    /** Making a backup file, restoring one, and undoing the last restore. */
    @Serializable
    data object Backup : Destination

    @Serializable
    data object About : Destination

    @Serializable
    data object Licenses : Destination

    @Serializable
    data object Privacy : Destination

    /**
     * Logs a new entry to [trackerId], or edits the entry [entryId] when it is set. [optionId]
     * starts a new entry on that part of the tracker.
     */
    @Serializable
    data class LogEntry(val trackerId: String, val entryId: String? = null, val optionId: String? = null) : Destination
}
