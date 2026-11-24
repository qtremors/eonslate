package dev.qtremors.eonslate.core.tracker.data.repository

import dev.qtremors.eonslate.core.tracker.data.db.ReminderEntity
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomReminderRepository @Inject constructor(database: TrackerDatabase) : ReminderRepository {
    private val dao = database.reminderDao()

    override fun observeReminders(trackerId: String): Flow<List<Reminder>> =
        dao.observeForTracker(trackerId).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeActive(): Flow<List<Reminder>> =
        dao.observeActive().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun getActive(): List<Reminder> = dao.getActive().map { it.toDomain() }

    override suspend fun getReminder(id: String): Reminder? = dao.get(id)?.toDomain()

    override suspend fun saveReminder(reminder: Reminder) = dao.upsert(reminder.toEntity())

    override suspend fun deleteReminder(id: String) = dao.delete(id)
}

internal fun ReminderEntity.toDomain() = Reminder(
    id = id,
    trackerId = trackerId,
    minuteOfDay = minuteOfDay,
    days = Reminder.maskToDays(daysMask),
    enabled = enabled,
    skipIfLogged = skipIfLogged,
    createdAt = createdAt,
)

internal fun Reminder.toEntity() = ReminderEntity(
    id = id,
    trackerId = trackerId,
    minuteOfDay = minuteOfDay,
    daysMask = Reminder.daysToMask(days),
    enabled = enabled,
    skipIfLogged = skipIfLogged,
    createdAt = createdAt,
)
