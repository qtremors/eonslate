package dev.qtremors.eonslate.core.tracker.data

import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.repository.BackupManager
import dev.qtremors.eonslate.core.tracker.data.repository.RoomBackupRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupException
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupProblem
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.tracker.domain.backup.ValueRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: TrackerDatabase
    private lateinit var other: TrackerDatabase
    private lateinit var clock: FakeClock

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        other = inMemoryDatabase()
        clock = FakeClock()
    }

    @After
    fun tearDown() {
        db.close()
        other.close()
    }

    /** Two trackers, one archived, with every kind of value. */
    private suspend fun fill(database: TrackerDatabase) {
        val trackers = RoomTrackerRepository(database, clock)
        val entries = RoomEntryRepository(database)
        trackers.saveTracker(everyTypeTracker("t1", "Everything"))
        trackers.saveTracker(everyTypeTracker("t2", "Second", position = 1))
        trackers.setArchived("t2", true)
        entries.saveEntry(entry("e1", "t1", note = "First"))
        entries.saveEntry(entry("e2", "t1", occurredAt = NOW - 86_400_000))
        entries.saveEntry(entry("e3", "t2", values = everyValue("t2")))
    }

    private fun manager(database: TrackerDatabase) =
        BackupManager(RoomBackupRepository(database, clock), folder.root, Dispatchers.Unconfined, iterations = 10_000)

    @Test
    fun tagsSurviveSavingAndRestoringABackup() = runTest {
        val tags = listOf("Health", "Après work")
        val trackers = RoomTrackerRepository(db, clock)
        assertTrue(trackers.saveTracker(everyTypeTracker().copy(tags = tags)).isValid)
        assertEquals(tags, trackers.getTracker("t1")!!.tags)

        val bytes = manager(db).create(null, "2.1.1")
        val backups = manager(other)
        val snapshot = backups.open(bytes, null)
        assertEquals(tags, snapshot.trackers.single().tags)
        backups.restore(snapshot, "2.1.1")
        assertEquals(tags, RoomTrackerRepository(other, clock).getTracker("t1")!!.tags)
    }

    @Test
    fun aSnapshotRestoresIntoAnotherDatabaseExactly() = runTest {
        fill(db)
        val snapshot = RoomBackupRepository(db, clock).snapshot("2.0.9")
        assertEquals(2, snapshot.trackers.size)
        assertEquals(3, snapshot.entries.size)
        assertEquals(27, snapshot.values.size)

        val target = RoomBackupRepository(other, clock)
        target.replaceAll(snapshot)
        assertEquals(snapshot, target.snapshot("2.0.9"))
        // The restored rows read back through the normal repositories too.
        assertEquals(everyValue(), RoomEntryRepository(other).getEntry("e1")!!.values)
        assertTrue(RoomTrackerRepository(other, clock).getTracker("t2")!!.archived)
    }

    @Test
    fun aRestoreReplacesWhatWasThere() = runTest {
        fill(db)
        RoomTrackerRepository(other, clock).saveTracker(everyTypeTracker("stale", "Stale"))
        RoomBackupRepository(other, clock).replaceAll(RoomBackupRepository(db, clock).snapshot("2.0.9"))
        assertNull(RoomTrackerRepository(other, clock).getTracker("stale"))
        assertEquals(DataCounts(2, 3), RoomBackupRepository(other, clock).counts())
    }

    @Test
    fun aRestoreThatFailsHalfwayChangesNothing() = runTest {
        fill(other)
        val before = RoomBackupRepository(other, clock).snapshot("2.0.9")
        fill(db)
        val good = RoomBackupRepository(db, clock).snapshot("2.0.9")
        // A yes/no of 7 slips past nothing but the trigger: the transaction must roll back.
        val bad = good.copy(values = good.values.map { if (it.type == "yes_no") it.copy(longValue = 7) else it })
        try {
            RoomBackupRepository(other, clock).replaceAll(bad)
            fail("the trigger should refuse the restore")
        } catch (_: Exception) {
        }
        assertEquals(before, RoomBackupRepository(other, clock).snapshot("2.0.9"))
    }

    @Test
    fun anEncryptedBackupOpensOnlyWithItsPassword() = runTest {
        fill(db)
        val backups = manager(db)
        val bytes = backups.create("open sesame".toCharArray(), "2.0.9")
        assertTrue(backups.inspect(bytes).encrypted)
        assertEquals(3, backups.open(bytes, "open sesame".toCharArray()).entries.size)
        val problem = runCatching { backups.open(bytes, "wrong".toCharArray()) }.exceptionOrNull() as BackupException
        assertEquals(BackupProblem.WRONG_PASSWORD, problem.problem)
    }

    @Test
    fun aRestoreKeepsARestorePointThatUndoBringsBack() = runTest {
        fill(db)
        val file = manager(db).create(null, "2.0.9")

        RoomTrackerRepository(other, clock).saveTracker(everyTypeTracker("mine", "Mine"))
        val backups = manager(other)
        assertNull(backups.restorePoint.value)

        backups.restore(backups.open(file, null), "2.0.9")
        assertEquals(DataCounts(2, 3), backups.counts())
        assertNotNull(backups.restorePoint.value)
        // A new manager over the same folder still finds the point, as after an app restart.
        assertNotNull(manager(other).restorePoint.value)

        backups.undoRestore()
        assertEquals(DataCounts(1, 0), backups.counts())
        assertNotNull(RoomTrackerRepository(other, clock).getTracker("mine"))
        assertNull(backups.restorePoint.value)
    }

    @Test
    fun aRefusedRestoreKeepsDataAndTheOldRestorePoint() = runTest {
        fill(db)
        val backups = manager(other)
        RoomTrackerRepository(other, clock).saveTracker(everyTypeTracker("mine", "Mine"))
        backups.restore(manager(db).open(manager(db).create(null, "2.0.9"), null), "2.0.9")
        val point = backups.restorePoint.value

        val good = RoomBackupRepository(db, clock).snapshot("2.0.9")
        val orphan = good.copy(values = good.values + ValueRow("missing", "t1-done", "yes_no", longValue = 1))
        val problem = runCatching { backups.restore(orphan, "2.0.9") }.exceptionOrNull() as BackupException
        assertEquals(BackupProblem.INCONSISTENT, problem.problem)
        assertEquals(DataCounts(2, 3), backups.counts())
        assertEquals(point, backups.restorePoint.value)
        assertTrue(folder.root.listFiles()!!.none { it.name.endsWith(".tmp") })
    }
}
