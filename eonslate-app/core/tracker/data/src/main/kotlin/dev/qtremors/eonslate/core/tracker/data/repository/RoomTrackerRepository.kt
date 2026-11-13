package dev.qtremors.eonslate.core.tracker.data.repository

import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.EntryDao
import dev.qtremors.eonslate.core.tracker.data.db.EntryValueEntity
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDao
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.ValueColumns
import dev.qtremors.eonslate.core.tracker.data.db.fieldEntities
import dev.qtremors.eonslate.core.tracker.data.db.toDomain
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.validation.TrackerValidator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.tracker.domain.value.FieldValueAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTrackerRepository @Inject constructor(
    private val database: TrackerDatabase,
    private val clock: AppClock,
) : TrackerRepository {
    private val trackers: TrackerDao = database.trackerDao()
    private val entries: EntryDao = database.entryDao()

    override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> =
        trackers.observeTrackers(includeArchived)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeSummaries(): Flow<List<TrackerSummary>> =
        trackers.observeSummaries()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeTracker(id: String): Flow<Tracker?> =
        trackers.observeTracker(id)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun getTracker(id: String): Tracker? = trackers.getTracker(id)?.toDomain()

    override suspend fun saveTracker(tracker: Tracker): ValidationResult {
        val result = TrackerValidator.validate(tracker)
        if (!result.isValid) return result

        database.withTransaction {
            val previous = trackers.getTracker(tracker.id)?.toDomain()?.fields.orEmpty().associateBy { it.id }
            val next = tracker.fields.associateBy { it.id }

            trackers.upsertTracker(tracker.toEntity())

            // Removed fields go first; the cascade takes their values with them.
            val removed = previous.keys - next.keys
            if (removed.isNotEmpty()) trackers.deleteFields(removed.toList())

            // Values of a field whose type changes must be gone before the field row changes,
            // or the integrity trigger refuses the update.
            val changed = next.values.mapNotNull { field ->
                previous[field.id]?.takeIf { FieldValueAdapter.needsAdapting(it.config, field.config) }?.let { it to field }
            }
            changed.filter { (old, new) -> old.type != new.type }
                .forEach { (old, _) -> entries.deleteValuesForField(old.id) }

            trackers.upsertFields(tracker.fieldEntities())

            changed.filter { (old, new) -> old.type == new.type }
                .forEach { (old, new) -> adaptValues(old, new) }
        }
        return result
    }

    /** Rewrites each logged value of [old] for [new], dropping the ones that cannot be kept. */
    private suspend fun adaptValues(old: FieldDefinition, new: FieldDefinition) {
        val rewritten = mutableListOf<EntryValueEntity>()
        entries.getValuesForField(old.id).forEach { row ->
            val adapted = ValueColumns.toDomain(row)?.let { FieldValueAdapter.adapt(old.config, new.config, it) }
            if (adapted == null) {
                entries.deleteValue(row.entryId, row.fieldId)
            } else {
                rewritten += ValueColumns.toEntity(row.entryId, row.fieldId, adapted)
            }
        }
        if (rewritten.isNotEmpty()) entries.upsertValues(rewritten)
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        trackers.setArchived(id, archived, clock.nowMillis())
    }

    override suspend fun reorder(orderedIds: List<String>) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, id -> trackers.setPosition(id, index) }
        }
    }

    override suspend fun deleteTracker(id: String) {
        trackers.deleteTracker(id)
    }
}
