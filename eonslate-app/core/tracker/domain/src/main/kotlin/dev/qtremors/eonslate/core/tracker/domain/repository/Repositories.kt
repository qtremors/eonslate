package dev.qtremors.eonslate.core.tracker.domain.repository

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Trackers and their fields. Every observe function emits again whenever the stored data
 * changes, so screens never refresh by hand.
 */
interface TrackerRepository {
    /** Trackers in home order (position, then creation), with or without archived ones. */
    fun observeTrackers(includeArchived: Boolean = false): Flow<List<Tracker>>

    /** Active trackers in home order with their entry count and latest entry time. */
    fun observeSummaries(): Flow<List<TrackerSummary>>

    fun observeTracker(id: String): Flow<Tracker?>

    suspend fun getTracker(id: String): Tracker?

    /** The position that puts a new tracker after every existing one, archived ones included. */
    suspend fun nextPosition(): Int

    /**
     * Validates [tracker] and, when valid, inserts or updates it with exactly its fields in one
     * transaction. Fields removed from the tracker are deleted with their logged values; fields
     * that stay keep their values. Returns the validation result either way.
     */
    suspend fun saveTracker(tracker: Tracker): ValidationResult

    suspend fun setArchived(id: String, archived: Boolean)

    /** Gives each id in [orderedIds] its index as position. Ids not listed are left alone. */
    suspend fun reorder(orderedIds: List<String>)

    /** Deletes the tracker with all of its fields, entries and values. */
    suspend fun deleteTracker(id: String)
}

/** Entries logged to trackers, with their values. */
interface EntryRepository {
    /** Entries of one tracker, newest first. */
    fun observeEntries(trackerId: String): Flow<List<Entry>>

    /** Entries of one tracker that occurred in [fromMillis, untilMillis), newest first. */
    fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long): Flow<List<Entry>>

    suspend fun getEntry(id: String): Entry?

    /**
     * Validates [entry] against its tracker and, when valid, inserts or replaces it and its
     * values in one transaction. An entry for a tracker that does not exist is invalid.
     */
    suspend fun saveEntry(entry: Entry): ValidationResult

    suspend fun deleteEntry(id: String)
}
