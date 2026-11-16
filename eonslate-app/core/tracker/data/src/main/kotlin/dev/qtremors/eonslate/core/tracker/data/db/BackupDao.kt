package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Whole-table reads and writes for backups. Plain inserts that fail on a clash, never REPLACE,
 * so a restore can only add rows into tables it has just emptied.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM trackers ORDER BY position, created_at, id")
    suspend fun allTrackers(): List<TrackerEntity>

    @Query("SELECT * FROM tracker_fields ORDER BY tracker_id, position, id")
    suspend fun allFields(): List<TrackerFieldEntity>

    @Query("SELECT * FROM entries ORDER BY tracker_id, occurred_at, id")
    suspend fun allEntries(): List<EntryEntity>

    @Query("SELECT * FROM entry_values ORDER BY entry_id, field_id")
    suspend fun allValues(): List<EntryValueEntity>

    @Query("SELECT COUNT(*) FROM trackers")
    suspend fun trackerCount(): Int

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun entryCount(): Int

    // Children first, so no step relies on cascades.
    @Query("DELETE FROM entry_values")
    suspend fun clearValues()

    @Query("DELETE FROM entries")
    suspend fun clearEntries()

    @Query("DELETE FROM tracker_fields")
    suspend fun clearFields()

    @Query("DELETE FROM trackers")
    suspend fun clearTrackers()

    @Insert
    suspend fun insertTrackers(rows: List<TrackerEntity>)

    @Insert
    suspend fun insertFields(rows: List<TrackerFieldEntity>)

    @Insert
    suspend fun insertEntries(rows: List<EntryEntity>)

    @Insert
    suspend fun insertValues(rows: List<EntryValueEntity>)
}
