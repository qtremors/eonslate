package dev.qtremors.eonslate.core.reminders

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

/** Reminders in memory; every reminder here counts as active unless disabled. */
private class FakeReminders : ReminderRepository {
    val all = MutableStateFlow<List<Reminder>>(emptyList())
    override fun observeReminders(trackerId: String): Flow<List<Reminder>> = all.map { list -> list.filter { it.trackerId == trackerId } }
    override fun observeActive(): Flow<List<Reminder>> = all.map { list -> list.filter { it.enabled } }
    override suspend fun getActive() = all.value.filter { it.enabled }
    override suspend fun getReminder(id: String) = all.value.firstOrNull { it.id == id }
    override suspend fun saveReminder(reminder: Reminder) {
        all.value = all.value.filterNot { it.id == reminder.id } + reminder
    }
    override suspend fun deleteReminder(id: String) {
        all.value = all.value.filterNot { it.id == id }
    }
}

/** No entries: these tests are about alarms, not clearing shown notifications. */
private object NoEntries : dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository {
    override fun observeEntries(trackerId: String) = kotlinx.coroutines.flow.emptyFlow<List<dev.qtremors.eonslate.core.tracker.domain.model.Entry>>()
    override fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long) = kotlinx.coroutines.flow.emptyFlow<List<dev.qtremors.eonslate.core.tracker.domain.model.Entry>>()
    override fun observeRecentEntries(trackerId: String, limit: Int) = kotlinx.coroutines.flow.emptyFlow<List<dev.qtremors.eonslate.core.tracker.domain.model.Entry>>()
    override fun observeEntryTimes(trackerId: String) = kotlinx.coroutines.flow.emptyFlow<List<dev.qtremors.eonslate.core.tracker.domain.model.EntryTime>>()
    override suspend fun getEntry(id: String): dev.qtremors.eonslate.core.tracker.domain.model.Entry? = null
    override fun observeEntryTimesByTracker() = kotlinx.coroutines.flow.emptyFlow<Map<String, List<dev.qtremors.eonslate.core.tracker.domain.model.EntryTime>>>()
    override suspend fun getLatestEntry(trackerId: String): dev.qtremors.eonslate.core.tracker.domain.model.Entry? = null
    override suspend fun saveEntry(entry: dev.qtremors.eonslate.core.tracker.domain.model.Entry) = dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult.Valid
    override suspend fun deleteEntry(id: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
class ReminderSyncTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val repo = FakeReminders()
    private val sync = ReminderSync(context, repo, NoEntries)
    private val now = ZonedDateTime.of(2026, 11, 11, 9, 0, 0, 0, ZoneId.of("Asia/Kolkata"))

    private fun reminder(id: String, minute: Int = 20 * 60, enabled: Boolean = true, days: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
        Reminder(id, "t1", minute, days, enabled)

    @Test
    fun everyActiveReminderGetsOneAlarm() = runTest {
        repo.saveReminder(reminder("a"))
        repo.saveReminder(reminder("b", minute = 7 * 60))
        repo.saveReminder(reminder("off", enabled = false))
        sync.sync(now)
        assertEquals(2, alarms.scheduledAlarms.size)
    }

    @Test
    fun syncingAgainNeverDuplicates() = runTest {
        repo.saveReminder(reminder("a"))
        sync.sync(now)
        sync.sync(now.plusHours(12))
        sync.sync(now.plusHours(13))
        assertEquals(1, alarms.scheduledAlarms.size)
    }

    @Test
    fun aRemovedOrDisabledReminderLosesItsAlarm() = runTest {
        repo.saveReminder(reminder("a"))
        repo.saveReminder(reminder("b"))
        sync.sync(now)
        repo.deleteReminder("a")
        repo.saveReminder(reminder("b", enabled = false))
        sync.sync(now)
        assertEquals(0, alarms.scheduledAlarms.size)
    }

    @Test
    fun theAlarmIsAtTheNextDueTime() = runTest {
        repo.saveReminder(reminder("a", minute = 20 * 60))
        sync.sync(now)
        assertEquals(now.withHour(20).toInstant().toEpochMilli(), alarms.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun aReminderWithNoDaysGetsNoAlarm() = runTest {
        repo.saveReminder(reminder("a", days = emptySet()))
        sync.sync(now)
        assertEquals(0, alarms.scheduledAlarms.size)
    }
}
