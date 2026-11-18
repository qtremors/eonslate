package dev.qtremors.eonslate.core.tracker.data.repository

import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.toDomain
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.data.db.valueEntities
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvException
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvProblem
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerCsvRepository
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerHistory
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.validation.EntryValidator
import javax.inject.Inject

class RoomTrackerCsvRepository @Inject constructor(private val db: TrackerDatabase) : TrackerCsvRepository {
    private val trackers = db.trackerDao()
    private val entries = db.entryDao()

    override suspend fun readHistory(trackerId: String): TrackerHistory? = db.withTransaction {
        val tracker = trackers.getTracker(trackerId)?.toDomain() ?: return@withTransaction null
        TrackerHistory(tracker, entries.getEntries(trackerId).map { it.toDomain() })
    }

    override suspend fun append(expected: Tracker, entries: List<Entry>) {
        db.withTransaction {
            val tracker = trackers.getTracker(expected.id)?.toDomain()
            if (tracker == null || tracker.fields != expected.fields) throw CsvException(CsvProblem.TRACKER_CHANGED)
            entries.forEachIndexed { index, entry ->
                if (!EntryValidator.validate(entry, tracker).isValid) throw CsvException(CsvProblem.ENTRY, index + 2)
            }
            this.entries.insertEntries(entries.map { it.toEntity() })
            entries.chunked(100).forEach { chunk ->
                this.entries.insertValues(chunk.flatMap { it.valueEntities() })
            }
        }
    }
}
