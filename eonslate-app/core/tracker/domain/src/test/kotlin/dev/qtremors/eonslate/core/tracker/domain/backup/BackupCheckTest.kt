package dev.qtremors.eonslate.core.tracker.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCheckTest {
    private fun problems(snapshot: BackupSnapshot) = BackupCheck.problems(snapshot, currentSchema = 1)

    @Test
    fun aSoundSnapshotHasNoProblems() {
        assertEquals(emptyList<String>(), problems(sampleSnapshot()))
    }

    @Test
    fun anEmptySnapshotIsSound() {
        assertEquals(emptyList<String>(), problems(BackupSnapshot(schema = 1, createdAt = 0, appVersion = "2.0.9")))
    }

    @Test
    fun aNewerSchemaIsRefused() {
        val newer = sampleSnapshot().copy(schema = 2)
        assertTrue(problems(newer).isNotEmpty())
        val problem = runCatching { BackupCheck.requireSound(newer, 1) }.exceptionOrNull() as BackupException
        assertEquals(BackupProblem.NEWER_VERSION, problem.problem)
    }

    @Test
    fun orphansAreFound() {
        val s = sampleSnapshot()
        assertTrue(problems(s.copy(trackers = s.trackers.drop(1))).any { "belongs to no tracker" in it })
        assertTrue(problems(s.copy(fields = s.fields.drop(1))).any { "no field or entry" in it })
        assertTrue(problems(s.copy(entries = emptyList())).any { "no field or entry" in it })
    }

    @Test
    fun aValueCannotJoinTwoTrackers() {
        val s = sampleSnapshot()
        val moved = s.copy(entries = s.entries.map { it.copy(trackerId = "t2") })
        assertTrue(problems(moved).any { "joins two trackers" in it })
    }

    @Test
    fun valuesMustFitTheirType() {
        val s = sampleSnapshot()
        fun withValue(v: ValueRow) = s.copy(values = listOf(v, s.values[1]))
        assertTrue(problems(withValue(ValueRow("e1", "f1", "text", textValue = "x"))).any { "in a duration field" in it })
        assertTrue(problems(withValue(ValueRow("e1", "f1", "duration", textValue = "x"))).any { "not a whole number" in it })
        assertTrue(problems(withValue(ValueRow("e1", "f1", "duration", longValue = -1))).any { "negative" in it })
    }

    @Test
    fun aFieldConfigMustMatchItsType() {
        val s = sampleSnapshot()
        val wrong = s.copy(fields = listOf(s.fields[0].copy(configJson = """{"kind":"text"}"""), s.fields[1]))
        assertTrue(problems(wrong).any { "does not fit" in it })
        val broken = s.copy(fields = listOf(s.fields[0].copy(configJson = "{"), s.fields[1]))
        assertTrue(problems(broken).any { "does not fit" in it })
    }

    @Test
    fun duplicatesAndBadTrackersAreFound() {
        val s = sampleSnapshot()
        assertTrue(problems(s.copy(trackers = s.trackers + s.trackers[1])).any { "appears twice" in it })
        assertTrue(problems(s.copy(trackers = listOf(s.trackers[0].copy(colorIndex = 99), s.trackers[1]))).any { "color" in it })
        assertTrue(problems(s.copy(trackers = listOf(s.trackers[0].copy(name = " "), s.trackers[1]))).any { "no name" in it })
    }

    @Test
    fun requireSoundNamesTheProblem() {
        val s = sampleSnapshot()
        val e = runCatching { BackupCheck.requireSound(s.copy(entries = emptyList()), 1) }.exceptionOrNull() as BackupException
        assertEquals(BackupProblem.INCONSISTENT, e.problem)
    }
}
