package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Transaction
    @Query(
        """SELECT * FROM trackers
           WHERE :includeArchived OR archived = 0
           ORDER BY position, created_at, id"""
    )
    fun observeTrackers(includeArchived: Boolean): Flow<List<TrackerWithFields>>

    @Transaction
    @Query(
        """SELECT t.*, COUNT(e.id) AS entry_count, MAX(e.occurred_at) AS last_entry_at
           FROM trackers t
           LEFT JOIN entries e ON e.tracker_id = t.id
           WHERE t.archived = 0
           GROUP BY t.id
           ORDER BY t.position, t.created_at, t.id"""
    )
    fun observeSummaries(): Flow<List<TrackerSummaryRow>>

    @Transaction
    @Query("SELECT * FROM trackers WHERE id = :id")
    fun observeTracker(id: String): Flow<TrackerWithFields?>

    @Transaction
    @Query("SELECT * FROM trackers WHERE id = :id")
    suspend fun getTracker(id: String): TrackerWithFields?

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM trackers")
    suspend fun nextPosition(): Int

    @Query("SELECT * FROM tracker_fields WHERE tracker_id = :trackerId ORDER BY position")
    suspend fun getFields(trackerId: String): List<TrackerFieldEntity>

    /** Never REPLACE: a replace deletes the row first, and the cascade would take its entries. */
    @Upsert
    suspend fun upsertTracker(tracker: TrackerEntity)

    @Upsert
    suspend fun upsertFields(fields: List<TrackerFieldEntity>)

    @Query("DELETE FROM tracker_fields WHERE id IN (:ids)")
    suspend fun deleteFields(ids: List<String>)

    @Query("UPDATE trackers SET archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("UPDATE trackers SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteTracker(id: String)
}

@Dao
interface EntryDao {
    @Transaction
    @Query(
        """SELECT * FROM entries WHERE tracker_id = :trackerId
           ORDER BY occurred_at DESC, created_at DESC, id DESC"""
    )
    fun observeEntries(trackerId: String): Flow<List<EntryWithValues>>

    @Transaction
    @Query(
        """SELECT * FROM entries WHERE tracker_id = :trackerId
           ORDER BY occurred_at DESC, created_at DESC, id DESC LIMIT :limit"""
    )
    fun observeRecentEntries(trackerId: String, limit: Int): Flow<List<EntryWithValues>>

    /** Only two columns, so a history of thousands of entries stays cheap to read. */
    @Query("SELECT occurred_at, zone_id FROM entries WHERE tracker_id = :trackerId ORDER BY occurred_at DESC")
    fun observeEntryTimes(trackerId: String): Flow<List<EntryTimeRow>>

    @Transaction
    @Query(
        """SELECT * FROM entries
           WHERE tracker_id = :trackerId AND occurred_at >= :fromMillis AND occurred_at < :untilMillis
           ORDER BY occurred_at DESC, created_at DESC, id DESC"""
    )
    fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long): Flow<List<EntryWithValues>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getEntry(id: String): EntryWithValues?

    @Transaction
    @Query(
        """SELECT * FROM entries WHERE tracker_id = :trackerId
           ORDER BY occurred_at DESC, created_at DESC, id DESC LIMIT 1"""
    )
    suspend fun getLatestEntry(trackerId: String): EntryWithValues?

    @Upsert
    suspend fun upsertEntry(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("SELECT * FROM entry_values WHERE field_id = :fieldId")
    suspend fun getValuesForField(fieldId: String): List<EntryValueEntity>

    @Upsert
    suspend fun upsertValues(values: List<EntryValueEntity>)

    @Query("DELETE FROM entry_values WHERE entry_id = :entryId")
    suspend fun deleteValuesForEntry(entryId: String)

    @Query("DELETE FROM entry_values WHERE field_id = :fieldId")
    suspend fun deleteValuesForField(fieldId: String)

    @Query("DELETE FROM entry_values WHERE entry_id = :entryId AND field_id = :fieldId")
    suspend fun deleteValue(entryId: String, fieldId: String)
}
