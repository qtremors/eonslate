package dev.qtremors.eonslate.feature.entry

import androidx.lifecycle.SavedStateHandle
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.EntryValidator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class LogEntryViewModelTest {
    // 2026-11-10 08:30 in Kolkata.
    private val now = ZonedDateTime.of(2026, 11, 10, 8, 30, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
    private val clock = object : AppClock {
        override fun nowMillis() = now
        override fun zoneId() = "Asia/Kolkata"
    }
    private val tracker = Tracker(
        id = "t",
        name = "Water",
        fields = listOf(
            FieldDefinition("g", "t", "Glasses", FieldConfig.Number(), required = true),
            FieldDefinition("c", "t", "Cold", FieldConfig.YesNo),
        ),
        createdAt = 0,
    )
    private val trackers = FakeTrackers(tracker)
    private val entries = FakeEntries(trackers)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(handle: SavedStateHandle = SavedStateHandle(mapOf(LogEntryViewModel.ARG_TRACKER_ID to "t"))) =
        LogEntryViewModel(handle, trackers, entries, clock, IdGenerator { "e1" })

    @Test
    fun newEntryStartsNowInTheDevicesZone() = runTest {
        val draft = vm().state.value.draft!!
        assertEquals(now, draft.occurredAt)
        assertEquals("Asia/Kolkata", draft.zoneId)
        assertTrue(draft.isNew)
    }

    @Test
    fun logsTypedValues() = runTest {
        val vm = vm()
        vm.setInput("g") { it.copy(text = "3") }
        vm.setInput("c") { it.copy(number = 1) }
        vm.save()
        val saved = entries.saved.single()
        assertEquals(mapOf("g" to FieldValue.Number(3), "c" to FieldValue.YesNo(true)), saved.values)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun missingRequiredFieldIsShownAndNothingSaved() = runTest {
        val vm = vm()
        vm.setInput("c") { it.copy(number = 0) }
        vm.save()
        assertEquals(ValidationError.RequiredFieldMissing("g"), vm.state.value.errorFor("g"))
        assertTrue(entries.saved.isEmpty())
        vm.setInput("g") { it.copy(text = "1") }
        assertEquals(null, vm.state.value.errorFor("g"))
    }

    @Test
    fun unreadableInputBlocksSaving() = runTest {
        val vm = vm()
        vm.setInput("g") { it.copy(text = "2.5") }
        vm.save()
        assertEquals(setOf("g"), vm.state.value.unreadable)
        assertFalse(vm.state.value.done)
    }

    @Test
    fun dateAndTimeMoveWithinTheEntrysZone() = runTest {
        val vm = vm()
        vm.setDate(LocalDate.of(2026, 11, 8))
        vm.setTime(LocalTime.of(23, 45))
        val at = vm.state.value.occurred!!
        assertEquals(LocalDate.of(2026, 11, 8), at.toLocalDate())
        assertEquals(LocalTime.of(23, 45), at.toLocalTime())
        assertEquals(ZoneId.of("Asia/Kolkata"), at.zone)
    }

    @Test
    fun editingLoadsTheEntryAndKeepsItsZone() = runTest {
        val logged = Entry(
            id = "old", trackerId = "t", occurredAt = 1_000, zoneId = "Europe/Paris",
            values = mapOf("g" to FieldValue.Number(8)), note = "n", createdAt = 1_000,
        )
        entries.saved += logged
        val vm = vm(SavedStateHandle(mapOf(LogEntryViewModel.ARG_TRACKER_ID to "t", LogEntryViewModel.ARG_ENTRY_ID to "old")))
        val draft = vm.state.value.draft!!
        assertEquals("8", draft.inputs["g"]!!.text)
        assertEquals("Europe/Paris", draft.zoneId)
        vm.setInput("g") { it.copy(text = "9") }
        vm.save()
        val updated = entries.saved.last()
        assertEquals(FieldValue.Number(9), updated.values["g"])
        assertEquals(1_000L, updated.createdAt)
        assertEquals(now, updated.updatedAt)
    }

    @Test
    fun missingTrackerOrEntryShowsMissing() = runTest {
        assertTrue(vm(SavedStateHandle(mapOf(LogEntryViewModel.ARG_TRACKER_ID to "gone"))).state.value.missing)
        assertTrue(
            vm(SavedStateHandle(mapOf(LogEntryViewModel.ARG_TRACKER_ID to "t", LogEntryViewModel.ARG_ENTRY_ID to "gone")))
                .state.value.missing
        )
    }

    @Test
    fun sameAsLastTimeCopiesValuesButNotTimeOrNote() = runTest {
        entries.saved += Entry(
            id = "prev", trackerId = "t", occurredAt = 1_000, zoneId = "UTC",
            values = mapOf("g" to FieldValue.Number(6), "c" to FieldValue.YesNo(true)), note = "old", createdAt = 1_000,
        )
        val vm = vm()
        assertTrue(vm.state.value.canUseLast)
        vm.useLastValues()
        vm.save()
        val saved = entries.saved.last()
        assertEquals(mapOf("g" to FieldValue.Number(6), "c" to FieldValue.YesNo(true)), saved.values)
        assertEquals(now, saved.occurredAt)
        assertEquals("", saved.note)
        assertEquals("e1", saved.id)
    }

    @Test
    fun sameAsLastTimeHidesOnceSomethingIsEntered() = runTest {
        entries.saved += Entry(
            id = "prev", trackerId = "t", occurredAt = 1, zoneId = "UTC",
            values = mapOf("g" to FieldValue.Number(1)), createdAt = 1,
        )
        val vm = vm()
        vm.setInput("c") { it.copy(number = 0) }
        assertFalse(vm.state.value.canUseLast)
    }

    @Test
    fun draftSurvivesProcessDeath() = runTest {
        val handle = SavedStateHandle(mapOf(LogEntryViewModel.ARG_TRACKER_ID to "t"))
        vm(handle).setInput("g") { it.copy(text = "4") }
        val restored = vm(handle).state.value
        assertEquals("4", restored.draft!!.inputs["g"]!!.text)
        assertTrue(restored.dirty)
    }

    private class FakeTrackers(private val tracker: Tracker) : TrackerRepository {
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
        override fun observeSummaries(): Flow<List<TrackerSummary>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = emptyFlow()
        override suspend fun getTracker(id: String) = tracker.takeIf { it.id == id }
        override suspend fun nextPosition() = 0
        override suspend fun saveTracker(tracker: Tracker) = ValidationResult.Valid
        override suspend fun setArchived(id: String, archived: Boolean) = Unit
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) = Unit
    }

    private class FakeEntries(private val trackers: FakeTrackers) : EntryRepository {
        val saved = mutableListOf<Entry>()
        override fun observeEntries(trackerId: String): Flow<List<Entry>> = emptyFlow()
        override fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long): Flow<List<Entry>> = emptyFlow()
        override fun observeRecentEntries(trackerId: String, limit: Int): Flow<List<Entry>> = emptyFlow()
        override fun observeEntryTimes(trackerId: String): Flow<List<EntryTime>> = emptyFlow()
        override suspend fun getEntry(id: String) = saved.lastOrNull { it.id == id }
        override fun observeEntryTimesByTracker(): Flow<Map<String, List<EntryTime>>> = emptyFlow()
        override suspend fun getLatestEntry(trackerId: String) =
            saved.filter { it.trackerId == trackerId }.maxByOrNull { it.occurredAt }
        override suspend fun saveEntry(entry: Entry): ValidationResult {
            val tracker = trackers.getTracker(entry.trackerId) ?: return ValidationResult.Invalid(listOf(ValidationError.TrackerNotFound))
            return EntryValidator.validate(entry, tracker).also { if (it.isValid) saved += entry }
        }
        override suspend fun deleteEntry(id: String) {
            saved.removeAll { it.id == id }
        }
    }
}
