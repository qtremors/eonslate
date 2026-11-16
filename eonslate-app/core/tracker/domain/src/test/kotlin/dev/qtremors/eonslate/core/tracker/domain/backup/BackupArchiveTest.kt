package dev.qtremors.eonslate.core.tracker.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** A small but complete snapshot: one tracker, two fields, one entry with both values. */
fun sampleSnapshot(note: String = "Felt good 🌙") = BackupSnapshot(
    schema = 1,
    createdAt = 1_762_740_000_000L,
    appVersion = "2.0.9",
    trackers = listOf(
        TrackerRow("t1", "Sleep", "", "Bedtime", 4, false, 0, "sleep", 1_000, 2_000),
        TrackerRow("t2", "Old", "", "Archive", 0, true, 1, null, 1_000, 1_000),
    ),
    fields = listOf(
        FieldRow("f1", "t1", "Hours", "duration", """{"kind":"duration"}""", true, 0),
        FieldRow("f2", "t1", "Mood", "text", """{"kind":"text"}""", false, 1),
    ),
    entries = listOf(EntryRow("e1", "t1", 1_762_700_000_000L, "Asia/Kolkata", note, 3_000, 3_000)),
    values = listOf(
        ValueRow("e1", "f1", "duration", longValue = 27_000),
        ValueRow("e1", "f2", "text", textValue = "Calm"),
    ),
    reminders = listOf(ReminderRow("r1", "t1", 21 * 60, 127)),
)

class BackupArchiveTest {
    private val iterations = 10_000

    private fun problemOf(block: () -> Unit): BackupProblem? = try {
        block()
        null
    } catch (e: BackupException) {
        e.problem
    }

    @Test
    fun aPlainBackupReadsBackExactly() {
        val bytes = BackupArchive.encode(sampleSnapshot(), password = null)
        assertFalse(BackupArchive.inspect(bytes).encrypted)
        assertEquals(sampleSnapshot(), BackupArchive.decode(bytes, password = null))
    }

    @Test
    fun anEncryptedBackupNeedsItsPassword() {
        val bytes = BackupArchive.encode(sampleSnapshot(), "correct horse".toCharArray(), iterations)
        assertTrue(BackupArchive.inspect(bytes).encrypted)
        assertEquals(sampleSnapshot(), BackupArchive.decode(bytes, "correct horse".toCharArray()))
        assertEquals(BackupProblem.PASSWORD_NEEDED, problemOf { BackupArchive.decode(bytes, null) })
        assertEquals(BackupProblem.WRONG_PASSWORD, problemOf { BackupArchive.decode(bytes, "Correct horse".toCharArray()) })
    }

    @Test
    fun anEncryptedBackupHidesItsContents() {
        val bytes = BackupArchive.encode(sampleSnapshot(note = "secret-note"), "pw".toCharArray(), iterations)
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("secret-note"))
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("Sleep"))
    }

    @Test
    fun anEmptyPasswordMeansNoEncryption() {
        val bytes = BackupArchive.encode(sampleSnapshot(), CharArray(0), iterations)
        assertFalse(BackupArchive.inspect(bytes).encrypted)
    }

    @Test
    fun everyBackupGetsItsOwnSaltAndNonce() {
        val a = BackupArchive.encode(sampleSnapshot(), "pw".toCharArray(), iterations)
        val b = BackupArchive.encode(sampleSnapshot(), "pw".toCharArray(), iterations)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun aChangeAnywhereInAnEncryptedFileIsCaught() {
        val bytes = BackupArchive.encode(sampleSnapshot(), "pw".toCharArray(), iterations)
        // A flipped flag byte would turn it into a "plain" file; every other header byte is
        // authenticated, and so is every body byte.
        listOf(14, 30, bytes.size / 2, bytes.size - 1).forEach { index ->
            val changed = bytes.copyOf().also { it[index] = (it[index].toInt() xor 0x01).toByte() }
            val problem = problemOf { BackupArchive.decode(changed, "pw".toCharArray()) }
            assertTrue("byte $index gave $problem", problem == BackupProblem.WRONG_PASSWORD || problem == BackupProblem.DAMAGED)
        }
    }

    @Test
    fun aCutShortFileIsDamaged() {
        val plain = BackupArchive.encode(sampleSnapshot(), null)
        assertEquals(BackupProblem.DAMAGED, problemOf { BackupArchive.decode(plain.copyOf(plain.size - 8), null) })
        val sealed = BackupArchive.encode(sampleSnapshot(), "pw".toCharArray(), iterations)
        assertEquals(BackupProblem.DAMAGED, problemOf { BackupArchive.decode(sealed.copyOf(30), "pw".toCharArray()) })
    }

    @Test
    fun otherFilesAreNotBackups() {
        assertEquals(BackupProblem.NOT_A_BACKUP, problemOf { BackupArchive.inspect(ByteArray(0)) })
        assertEquals(BackupProblem.NOT_A_BACKUP, problemOf { BackupArchive.inspect("PK\u0003\u0004 a zip file".toByteArray()) })
    }

    @Test
    fun aNewerFormatIsRefusedPolitely() {
        val bytes = BackupArchive.encode(sampleSnapshot(), null).also { it[8] = (BackupArchive.FORMAT_VERSION + 1).toByte() }
        assertEquals(BackupProblem.NEWER_VERSION, problemOf { BackupArchive.inspect(bytes) })
    }

    @Test
    fun aCraftedLowIterationCountIsRefused() {
        val bytes = BackupArchive.encode(sampleSnapshot(), "pw".toCharArray(), iterations)
        // Iterations sit after the 10 byte header and 16 byte salt.
        bytes[26] = 0; bytes[27] = 0; bytes[28] = 0; bytes[29] = 1
        assertEquals(BackupProblem.DAMAGED, problemOf { BackupArchive.decode(bytes, "pw".toCharArray()) })
    }

    @Test
    fun unknownJsonFromANewerAppIsIgnored() {
        val bytes = BackupArchive.encode(sampleSnapshot(), null)
        val decoded = BackupArchive.decode(bytes, null)
        assertEquals(2, decoded.summary.trackers)
        assertEquals(1, decoded.summary.archived)
        assertEquals(1, decoded.summary.entries)
        try {
            BackupArchive.decode(bytes.copyOf(12), null)
            fail("a cut short body must not read")
        } catch (e: BackupException) {
            assertEquals(BackupProblem.DAMAGED, e.problem)
        }
    }
}
