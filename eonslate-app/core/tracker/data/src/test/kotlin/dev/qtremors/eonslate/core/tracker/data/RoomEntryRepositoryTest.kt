package dev.qtremors.eonslate.core.tracker.data

import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomEntryRepositoryTest {
    private lateinit var db: TrackerDatabase
    private lateinit var trackers: RoomTrackerRepository
    private lateinit var entries: RoomEntryRepository

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        trackers = RoomTrackerRepository(db, FakeClock())
        entries = RoomEntryRepository(db)
        trackers.saveTracker(everyTypeTracker())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun everyValueTypeRoundTripsExactly() = runTest {
        val saved = entry(note = "  Long day  ")
        assertEquals(ValidationResult.Valid, entries.saveEntry(saved))

        assertEquals(saved.copy(note = "Long day"), entries.getEntry("e1"))
    }

    @Test
    fun extremeWholeNumbersSurvive() = runTest {
        val values = mapOf(
            "t1-weight" to FieldValue.Number(Long.MIN_VALUE),
            "t1-spent" to FieldValue.Money(Long.MAX_VALUE),
            "t1-day" to FieldValue.Date(-719_528),
            "t1-tags" to FieldValue.MultiChoice(emptySet()),
        )
        entries.saveEntry(entry(values = values))

        assertEquals(values, entries.getEntry("e1")!!.values)
    }

    @Test
    fun savingAgainReplacesTheValues() = runTest {
        entries.saveEntry(entry())
        val fewer = mapOf("t1-done" to FieldValue.YesNo(true))

        entries.saveEntry(entry(values = fewer).copy(updatedAt = NOW + 1))

        val read = entries.getEntry("e1")!!
        assertEquals(fewer, read.values)
        assertEquals(NOW + 1, read.updatedAt)
    }

    @Test
    fun entryForMissingTrackerIsRefused() = runTest {
        val result = entries.saveEntry(entry(trackerId = "ghost", values = emptyMap(), note = "x"))
        assertEquals(listOf(ValidationError.TrackerNotFound), result.errorList)
    }

    @Test
    fun invalidValuesAreRefusedAndNothingIsWritten() = runTest {
        val result = entries.saveEntry(entry(values = mapOf("t1-stars" to FieldValue.Rating(9))))

        assertTrue(ValidationError.RatingOutOfRange("t1-stars", 5) in result.errorList)
        assertNull(entries.getEntry("e1"))
    }

    @Test
    fun entryCannotMoveToAnotherTracker() = runTest {
        trackers.saveTracker(everyTypeTracker("t2"))
        entries.saveEntry(entry())

        val result = entries.saveEntry(entry(trackerId = "t2"))

        assertEquals(listOf(ValidationError.EntryTrackerMismatch), result.errorList)
        assertEquals("t1", entries.getEntry("e1")!!.trackerId)
    }

    @Test
    fun entriesAreNewestFirstAndRangeIsHalfOpen() = runTest {
        entries.saveEntry(entry("old", occurredAt = 1_000))
        entries.saveEntry(entry("mid", occurredAt = 2_000))
        entries.saveEntry(entry("new", occurredAt = 3_000))

        assertEquals(listOf("new", "mid", "old"), entries.observeEntries("t1").first().map { it.id })
        assertEquals(listOf("mid", "old"), entries.observeEntriesBetween("t1", 1_000, 3_000).first().map { it.id })
    }

    @Test
    fun latestEntryIsTheOneThatHappenedLast() = runTest {
        assertNull(entries.getLatestEntry("t1"))
        entries.saveEntry(entry("logged-late", occurredAt = 1_000))
        entries.saveEntry(entry("happened-late", occurredAt = 5_000))
        entries.saveEntry(entry("early", occurredAt = 2_000))

        assertEquals("happened-late", entries.getLatestEntry("t1")!!.id)
        assertEquals(everyValue(), entries.getLatestEntry("t1")!!.values)
        assertNull(entries.getLatestEntry("ghost"))
    }

    @Test
    fun deletingEntryDeletesItsValues() = runTest {
        entries.saveEntry(entry())
        entries.deleteEntry("e1")

        assertNull(entries.getEntry("e1"))
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM entry_values").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }
}
