package dev.qtremors.eonslate.core.tracker.data

import app.cash.turbine.test
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomTrackerRepositoryTest {
    private lateinit var db: TrackerDatabase
    private lateinit var clock: FakeClock
    private lateinit var trackers: RoomTrackerRepository
    private lateinit var entries: RoomEntryRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        clock = FakeClock()
        trackers = RoomTrackerRepository(db, clock)
        entries = RoomEntryRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savedTrackerReadsBackWithFieldsInOrder() = runTest {
        val tracker = everyTypeTracker()
        assertEquals(ValidationResult.Valid, trackers.saveTracker(tracker))

        val read = trackers.getTracker("t1")!!
        assertEquals(tracker.fields.map { it.id }, read.fields.map { it.id })
        assertEquals(tracker.fields.map { it.config }, read.fields.map { it.config })
        assertEquals(tracker.fields.indices.toList(), read.fields.map { it.position })
        assertEquals(tracker.copy(fields = read.fields), read)
    }

    @Test
    fun namesAreStoredTrimmed() = runTest {
        trackers.saveTracker(everyTypeTracker(name = "  Mood  "))
        assertEquals("Mood", trackers.getTracker("t1")!!.name)
    }

    @Test
    fun invalidTrackerIsNotSaved() = runTest {
        val result = trackers.saveTracker(everyTypeTracker(name = " "))
        assertTrue(ValidationError.TrackerNameBlank in result.errorList)
        assertNull(trackers.getTracker("t1"))
    }

    @Test
    fun reorderingFieldsKeepsTheirValues() = runTest {
        val tracker = everyTypeTracker()
        trackers.saveTracker(tracker)
        entries.saveEntry(entry())

        trackers.saveTracker(tracker.copy(fields = tracker.fields.reversed()))

        assertEquals(tracker.fields.reversed().map { it.id }, trackers.getTracker("t1")!!.fields.map { it.id })
        assertEquals(everyValue(), entries.getEntry("e1")!!.values)
    }

    @Test
    fun removingFieldDeletesOnlyItsValues() = runTest {
        val tracker = everyTypeTracker()
        trackers.saveTracker(tracker)
        entries.saveEntry(entry())

        trackers.saveTracker(tracker.copy(fields = tracker.fields.filterNot { it.id == "t1-note" }))

        assertEquals(everyValue() - "t1-note", entries.getEntry("e1")!!.values)
    }

    @Test
    fun changingDecimalsRescalesLoggedNumbers() = runTest {
        val tracker = everyTypeTracker()
        trackers.saveTracker(tracker)
        entries.saveEntry(entry())

        val finer = tracker.copy(
            fields = tracker.fields.map {
                if (it.id == "t1-weight") it.copy(config = FieldConfig.Number(unit = "kg", decimals = 3)) else it
            }
        )
        trackers.saveTracker(finer)

        assertEquals(FieldValue.Number(72_500), entries.getEntry("e1")!!.values["t1-weight"])
    }

    @Test
    fun changingTypeDropsOldValues() = runTest {
        val tracker = everyTypeTracker()
        trackers.saveTracker(tracker)
        entries.saveEntry(entry())

        val retyped = tracker.copy(
            fields = tracker.fields.map { if (it.id == "t1-done") it.copy(config = FieldConfig.Rating()) else it }
        )
        assertTrue(trackers.saveTracker(retyped).isValid)

        assertFalse("t1-done" in entries.getEntry("e1")!!.values)
        assertEquals(FieldConfig.Rating(), trackers.getTracker("t1")!!.field("t1-done")!!.config)
    }

    @Test
    fun removingAnOptionDropsSelectionsOfIt() = runTest {
        val tracker = everyTypeTracker()
        trackers.saveTracker(tracker)
        entries.saveEntry(entry())

        val fewer = moodOptions.filterNot { it.id == "bad" || it.id == "okay" }
        trackers.saveTracker(
            tracker.copy(
                fields = tracker.fields.map {
                    when (it.id) {
                        "t1-mood" -> it.copy(config = FieldConfig.SingleChoice(fewer))
                        "t1-tags" -> it.copy(config = FieldConfig.MultiChoice(fewer))
                        else -> it
                    }
                }
            )
        )

        val values = entries.getEntry("e1")!!.values
        assertFalse("t1-mood" in values)
        assertEquals(FieldValue.MultiChoice(setOf("good")), values["t1-tags"])
    }

    @Test
    fun updatingTrackerKeepsItsEntries() = runTest {
        trackers.saveTracker(everyTypeTracker())
        entries.saveEntry(entry())

        trackers.saveTracker(everyTypeTracker(name = "Renamed").copy(updatedAt = NOW + 1))

        assertEquals("Renamed", trackers.getTracker("t1")!!.name)
        assertEquals(1, entries.observeEntries("t1").first().size)
    }

    @Test
    fun deletingTrackerCascadesToEverything() = runTest {
        trackers.saveTracker(everyTypeTracker())
        entries.saveEntry(entry())

        trackers.deleteTracker("t1")

        assertNull(trackers.getTracker("t1"))
        assertNull(entries.getEntry("e1"))
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM entry_values").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun archivedTrackersLeaveTheHomeList() = runTest {
        trackers.saveTracker(everyTypeTracker("a"))
        trackers.saveTracker(everyTypeTracker("b", position = 1))
        clock.now = NOW + 500

        trackers.setArchived("a", true)

        assertEquals(listOf("b"), trackers.observeTrackers().first().map { it.id })
        assertEquals(listOf("a", "b"), trackers.observeTrackers(includeArchived = true).first().map { it.id })
        assertEquals(listOf("b"), trackers.observeSummaries().first().map { it.tracker.id })
        assertEquals(NOW + 500, trackers.getTracker("a")!!.updatedAt)
    }

    @Test
    fun reorderSetsPositionsByIndex() = runTest {
        listOf("a", "b", "c").forEachIndexed { i, id -> trackers.saveTracker(everyTypeTracker(id, position = i)) }

        trackers.reorder(listOf("c", "a", "b"))

        assertEquals(listOf("c", "a", "b"), trackers.observeTrackers().first().map { it.id })
    }

    @Test
    fun equalPositionsFallBackToCreationOrder() = runTest {
        trackers.saveTracker(everyTypeTracker("late", createdAt = NOW + 10))
        trackers.saveTracker(everyTypeTracker("early", createdAt = NOW))

        assertEquals(listOf("early", "late"), trackers.observeTrackers().first().map { it.id })
    }

    @Test
    fun summariesCountEntriesAndFindTheLatest() = runTest {
        trackers.saveTracker(everyTypeTracker("a"))
        trackers.saveTracker(everyTypeTracker("b", position = 1))
        entries.saveEntry(entry("e1", "a", occurredAt = NOW))
        entries.saveEntry(entry("e2", "a", occurredAt = NOW + 9_000))
        entries.saveEntry(entry("e3", "a", occurredAt = NOW - 9_000))

        val summaries = trackers.observeSummaries().first()

        assertEquals(3, summaries[0].entryCount)
        assertEquals(NOW + 9_000, summaries[0].lastEntryAt)
        assertEquals(0, summaries[1].entryCount)
        assertNull(summaries[1].lastEntryAt)
        assertEquals(9, summaries[0].tracker.fields.size)
    }

    @Test
    fun summariesEmitAgainWhenAnEntryIsLogged() = runTest {
        trackers.saveTracker(everyTypeTracker())

        trackers.observeSummaries().test {
            assertEquals(0, awaitItem().single().entryCount)
            entries.saveEntry(entry())
            assertEquals(1, awaitItem().single().entryCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unreadableFieldConfigIsSkippedNotFatal() = runTest {
        trackers.saveTracker(everyTypeTracker())
        db.openHelper.writableDatabase.execSQL(
            "UPDATE tracker_fields SET config_json = '{\"kind\":\"number\",\"decimals\":\"many\"}' WHERE id = 't1-weight'"
        )

        val read = trackers.getTracker("t1")!!
        assertNull(read.field("t1-weight"))
        assertEquals(8, read.fields.size)
    }
}
