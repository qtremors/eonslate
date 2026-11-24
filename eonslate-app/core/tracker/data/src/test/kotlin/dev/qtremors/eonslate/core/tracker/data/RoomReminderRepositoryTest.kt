package dev.qtremors.eonslate.core.tracker.data

import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.repository.RoomBackupRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomReminderRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
class RoomReminderRepositoryTest {
    private lateinit var db: TrackerDatabase
    private lateinit var trackers: RoomTrackerRepository
    private lateinit var reminders: RoomReminderRepository
    private val clock = FakeClock()

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        trackers = RoomTrackerRepository(db, clock)
        reminders = RoomReminderRepository(db)
        trackers.saveTracker(everyTypeTracker("t1", "One"))
        trackers.saveTracker(everyTypeTracker("t2", "Two", position = 1))
    }

    @After
    fun tearDown() = db.close()

    private fun reminder(id: String, trackerId: String = "t1", minute: Int = 9 * 60, enabled: Boolean = true) =
        Reminder(id, trackerId, minute, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), enabled, skipIfLogged = false, createdAt = 1)

    @Test
    fun aReminderReadsBackExactly() = runTest {
        reminders.saveReminder(reminder("r1"))
        assertEquals(listOf(reminder("r1")), reminders.observeReminders("t1").first())
        assertEquals(reminder("r1"), reminders.getReminder("r1"))
    }

    @Test
    fun onlyEnabledRemindersOfActiveTrackersAreActive() = runTest {
        reminders.saveReminder(reminder("on"))
        reminders.saveReminder(reminder("off", enabled = false))
        reminders.saveReminder(reminder("archived", trackerId = "t2"))
        trackers.setArchived("t2", true)
        assertEquals(listOf("on"), reminders.getActive().map { it.id })
        assertEquals(listOf("on"), reminders.observeActive().first().map { it.id })
    }

    @Test
    fun deletingATrackerDeletesItsReminders() = runTest {
        reminders.saveReminder(reminder("r1"))
        trackers.deleteTracker("t1")
        assertNull(reminders.getReminder("r1"))
    }

    @Test
    fun theDatabaseRefusesAReminderWithNoDays() = runTest {
        try {
            reminders.saveReminder(reminder("r1").copy(days = emptySet()))
            fail("no days should be refused")
        } catch (_: Exception) {
        }
        assertNull(reminders.getReminder("r1"))
    }

    @Test
    fun remindersTravelWithBackups() = runTest {
        reminders.saveReminder(reminder("r1"))
        val snapshot = RoomBackupRepository(db, clock).snapshot("2.1.0")
        assertEquals(1, snapshot.reminders.size)

        val other = inMemoryDatabase()
        RoomBackupRepository(other, clock).replaceAll(snapshot)
        assertEquals(reminder("r1"), RoomReminderRepository(other).getReminder("r1"))
        other.close()
    }
}
