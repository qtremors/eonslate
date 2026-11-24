package dev.qtremors.eonslate.core.tracker.data.repository

import dev.qtremors.eonslate.core.tracker.data.db.IntervalColumn
import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.EntryEntity
import dev.qtremors.eonslate.core.tracker.data.db.EntryValueEntity
import dev.qtremors.eonslate.core.tracker.data.db.ReminderEntity
import dev.qtremors.eonslate.core.tracker.data.db.TagColumn
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.TrackerEntity
import dev.qtremors.eonslate.core.tracker.data.db.TrackerFieldEntity
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.tracker.domain.backup.EntryRow
import dev.qtremors.eonslate.core.tracker.domain.backup.FieldRow
import dev.qtremors.eonslate.core.tracker.domain.backup.ReminderRow
import dev.qtremors.eonslate.core.tracker.domain.backup.TrackerRow
import dev.qtremors.eonslate.core.tracker.domain.backup.ValueRow
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import javax.inject.Inject

/**
 * Backups straight from and into the tables. The integrity triggers stay on during a restore,
 * so even a snapshot that slipped past the check cannot store a row the app would misread; the
 * transaction then rolls back and the phone keeps what it had.
 */
class RoomBackupRepository @Inject constructor(
    private val database: TrackerDatabase,
    private val clock: AppClock,
) : BackupRepository {
    private val dao = database.backupDao()
    private val finance = database.financeDao()

    override val schema: Int = TrackerDatabase.VERSION

    override suspend fun snapshot(appVersion: String): BackupSnapshot = database.withTransaction {
        BackupSnapshot(
            schema = schema,
            createdAt = clock.nowMillis(),
            appVersion = appVersion,
            trackers = dao.allTrackers().map { it.toRow() },
            fields = dao.allFields().map { it.toRow() },
            entries = dao.allEntries().map { it.toRow() },
            values = dao.allValues().map { it.toRow() },
            reminders = dao.allReminders().map { it.toRow() },
            accounts = finance.accounts().map { it.toDomain() },
            transactions = finance.transactions().map { it.toDomain() },
            recurring = finance.recurring().map { it.toDomain() },
            budgets = finance.budgets().map { it.toDomain() },
            categories = finance.categories().map { it.toDomain() },
            parties = finance.parties().map { it.toDomain() },
            piggyBanks = finance.piggies().map { it.toDomain() },
            piggyMoves = finance.piggyMoves().map { it.toDomain() },
            attachments = finance.attachments().mapNotNull { it.toDomain() },
        )
    }

    override suspend fun replaceAll(snapshot: BackupSnapshot) {
        database.withTransaction {
            finance.clearAttachments()
            finance.clearTransactions()
            finance.clearParties()
            finance.clearPiggyMoves()
            finance.clearPiggies()
            finance.clearRecurring()
            finance.clearBudgets()
            finance.clearCategories()
            finance.clearAccounts()
            dao.clearReminders()
            dao.clearValues()
            dao.clearEntries()
            dao.clearFields()
            dao.clearTrackers()
            // Parents first: each trigger looks up the rows its new row points at.
            dao.insertTrackers(snapshot.trackers.map { it.toEntity() })
            dao.insertFields(snapshot.fields.map { it.toEntity() })
            dao.insertEntries(snapshot.entries.map { it.toEntity() })
            dao.insertValues(snapshot.values.map { it.toEntity() })
            dao.insertReminders(snapshot.reminders.map { it.toEntity() })
            finance.insertAccounts(snapshot.accounts.map { it.toEntity() })
            finance.insertParties(snapshot.parties.map { it.toEntity() })
            finance.insertPiggies(snapshot.piggyBanks.map { it.toEntity() })
            finance.insertPiggyMoves(snapshot.piggyMoves.map { it.toEntity() })
            finance.insertTransactions(snapshot.transactions.map { it.toEntity() })
            finance.insertAttachments(snapshot.attachments.map { it.toEntity() })
            finance.insertRecurring(snapshot.recurring.map { it.toEntity() })
            finance.insertBudgets(snapshot.budgets.map { it.toEntity() })
            finance.insertCategories(snapshot.categories.map { it.toEntity() })
        }
    }

    override suspend fun counts(): DataCounts = database.withTransaction {
        DataCounts(
            dao.trackerCount(), dao.entryCount(), finance.accountCount(), finance.transactionCount(),
            finance.recurringCount(), finance.budgetCount(),
        )
    }
}

private fun TrackerEntity.toRow() =
    TrackerRow(id, name, description, iconName, colorIndex, archived, position, templateId, createdAt, updatedAt, TagColumn.decode(tags), scheduleDays, timesPerWeek, IntervalColumn.decode(intervals))

private fun TrackerRow.toEntity() =
    TrackerEntity(id, name, description, iconName, colorIndex, archived, position, templateId, createdAt, updatedAt, TagColumn.encode(tags), scheduleDays, timesPerWeek, IntervalColumn.encode(intervals))

private fun TrackerFieldEntity.toRow() = FieldRow(id, trackerId, name, type, configJson, required, position)

private fun FieldRow.toEntity() = TrackerFieldEntity(id, trackerId, name, type, configJson, required, position)

private fun EntryEntity.toRow() = EntryRow(id, trackerId, occurredAt, zoneId, note, createdAt, updatedAt)

private fun EntryRow.toEntity() = EntryEntity(id, trackerId, occurredAt, zoneId, note, createdAt, updatedAt)

private fun EntryValueEntity.toRow() = ValueRow(entryId, fieldId, type, longValue, textValue)

private fun ReminderEntity.toRow() = ReminderRow(id, trackerId, minuteOfDay, daysMask, enabled, skipIfLogged, createdAt)

private fun ReminderRow.toEntity() = ReminderEntity(id, trackerId, minuteOfDay, daysMask, enabled, skipIfLogged, createdAt)

private fun ValueRow.toEntity() = EntryValueEntity(entryId, fieldId, type, longValue, textValue)
