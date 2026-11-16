package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE tracker_id = :trackerId ORDER BY minute_of_day, created_at, id")
    fun observeForTracker(trackerId: String): Flow<List<ReminderEntity>>

    @Query(
        """SELECT r.* FROM reminders r JOIN trackers t ON t.id = r.tracker_id
           WHERE r.enabled = 1 AND t.archived = 0 ORDER BY r.id"""
    )
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query(
        """SELECT r.* FROM reminders r JOIN trackers t ON t.id = r.tracker_id
           WHERE r.enabled = 1 AND t.archived = 0 ORDER BY r.id"""
    )
    suspend fun getActive(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: String): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}
