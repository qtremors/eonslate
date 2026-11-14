package dev.qtremors.eonslate.core.tracker.data.repository

import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.EntryDao
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDao
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.toDomain
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.data.db.valueEntities
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.validation.EntryValidator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryRepository @Inject constructor(
    private val database: TrackerDatabase,
) : EntryRepository {
    private val trackers: TrackerDao = database.trackerDao()
    private val entries: EntryDao = database.entryDao()

    override fun observeEntries(trackerId: String): Flow<List<Entry>> =
        entries.observeEntries(trackerId)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeRecentEntries(trackerId: String, limit: Int): Flow<List<Entry>> =
        entries.observeRecentEntries(trackerId, limit)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeEntryTimes(trackerId: String): Flow<List<EntryTime>> =
        entries.observeEntryTimes(trackerId)
            .map { rows -> rows.map { EntryTime(it.occurredAt, it.zoneId) } }
            .distinctUntilChanged()

    override fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long): Flow<List<Entry>> =
        entries.observeEntriesBetween(trackerId, fromMillis, untilMillis)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun getEntry(id: String): Entry? = entries.getEntry(id)?.toDomain()

    override suspend fun getLatestEntry(trackerId: String): Entry? = entries.getLatestEntry(trackerId)?.toDomain()

    override suspend fun saveEntry(entry: Entry): ValidationResult =
        // Validating inside the transaction means the tracker cannot change in between.
        database.withTransaction {
            val tracker = trackers.getTracker(entry.trackerId)?.toDomain()
                ?: return@withTransaction ValidationResult.Invalid(listOf(ValidationError.TrackerNotFound))
            val existing = entries.getEntry(entry.id)?.entry
            if (existing != null && existing.trackerId != entry.trackerId) {
                return@withTransaction ValidationResult.Invalid(listOf(ValidationError.EntryTrackerMismatch))
            }
            val result = EntryValidator.validate(entry, tracker)
            if (result.isValid) {
                entries.upsertEntry(entry.toEntity())
                entries.deleteValuesForEntry(entry.id)
                entries.upsertValues(entry.valueEntities())
            }
            result
        }

    override suspend fun deleteEntry(id: String) {
        entries.deleteEntry(id)
    }
}
