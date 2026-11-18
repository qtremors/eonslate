package dev.qtremors.eonslate.feature.backup

import dev.qtremors.eonslate.core.tracker.domain.backup.BackupArchive
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val backups = FakeBackups()
    private val documents = FakeDocuments()
    private lateinit var vm: BackupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = BackupViewModel(backups, documents)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val state get() = vm.state.value

    @Test
    fun thePageShowsWhatIsOnThePhone() {
        assertEquals(DataCounts(2, 5), state.counts)
        assertNull(state.restorePoint)
    }

    @Test
    fun anEncryptedBackupIsWrittenAndThePasswordWiped() = runTest {
        val password = "long enough".toCharArray()
        vm.choosePassword(password)
        vm.writeBackup("content://out", "2.0.9")
        assertEquals(BackupMessage.SAVED, state.message)
        assertTrue(BackupArchive.inspect(documents.files.getValue("content://out")).encrypted)
        assertArrayEquals(CharArray(password.size), password)
    }

    @Test
    fun closingThePickerWipesThePassword() {
        val password = "long enough".toCharArray()
        vm.choosePassword(password)
        vm.cancelCreate()
        assertTrue(password.all { it == '\u0000' })
    }

    @Test
    fun aPlaceThatCannotBeWrittenSaysSo() {
        documents.refuseWrites = true
        vm.choosePassword(CharArray(0))
        vm.writeBackup("content://out", "2.0.9")
        assertEquals(BackupMessage.WRITE_FAILED, state.message)
    }

    @Test
    fun aPlainBackupGoesStraightToReview() = runTest {
        documents.files["content://in"] = BackupArchive.encode(snapshotOf(4, 9), null)
        vm.openBackup("content://in")
        assertNull(state.unlock)
        assertEquals(4, state.review!!.backup.trackers)
        assertEquals(9, state.review!!.backup.entries)
        assertEquals(DataCounts(2, 5), state.review!!.current)
    }

    @Test
    fun anEncryptedBackupAsksForItsPasswordUntilItIsRight() = runTest {
        documents.files["content://in"] = BackupArchive.encode(snapshotOf(1, 1), "right one".toCharArray(), 10_000)
        vm.openBackup("content://in")
        assertEquals(false, state.unlock)

        val wrong = "wrong one".toCharArray()
        vm.unlock(wrong)
        assertEquals(true, state.unlock)
        assertNull(state.review)
        assertTrue(wrong.all { it == '\u0000' })

        vm.unlock("right one".toCharArray())
        assertNull(state.unlock)
        assertNotNull(state.review)
    }

    @Test
    fun otherFilesAreRefusedWithAReason() {
        documents.files["content://photo"] = "a photo".toByteArray()
        vm.openBackup("content://photo")
        assertEquals(BackupMessage.NOT_A_BACKUP, state.message)
        assertNull(state.review)

        vm.messageShown()
        vm.openBackup("content://gone")
        assertEquals(BackupMessage.READ_FAILED, state.message)
    }

    @Test
    fun restoringReplacesTheDataAndOffersUndo() = runTest {
        documents.files["content://in"] = BackupArchive.encode(snapshotOf(4, 9), null)
        vm.openBackup("content://in")
        vm.restore("2.0.9")
        assertEquals(BackupMessage.RESTORED, state.message)
        assertNull(state.review)
        assertEquals(DataCounts(4, 9), state.counts)
        assertEquals(42L, state.restorePoint)

        vm.undoRestore()
        assertEquals(BackupMessage.UNDONE, state.message)
        assertEquals(DataCounts(2, 5), state.counts)
        assertNull(state.restorePoint)
    }

    @Test
    fun aRestoreTheDatabaseRefusesChangesNothing() = runTest {
        documents.files["content://in"] = BackupArchive.encode(snapshotOf(4, 9), null)
        vm.openBackup("content://in")
        backups.failNextRestore = true
        vm.restore("2.0.9")
        assertEquals(BackupMessage.RESTORE_FAILED, state.message)
        assertEquals(DataCounts(2, 5), state.counts)
        assertNull(state.busy)
    }

    @Test
    fun puttingAwayForgetsTheOpenedFile() {
        documents.files["content://in"] = BackupArchive.encode(snapshotOf(4, 9), null)
        vm.openBackup("content://in")
        vm.putAway()
        assertNull(state.review)
        vm.restore("2.0.9")
        assertEquals(DataCounts(2, 5), state.counts)
        assertFalse(state.message == BackupMessage.RESTORED)
    }

    @Test
    fun clearingDeletesEverythingAndCanBeUndone() = runTest {
        vm.clearAll("2.1.7")
        assertEquals(BackupMessage.CLEARED, state.message)
        assertEquals(DataCounts(0, 0), state.counts)
        assertEquals(42L, state.restorePoint)
        vm.undoRestore()
        assertEquals(DataCounts(2, 5), state.counts)
    }
}
