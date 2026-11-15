package dev.qtremors.eonslate.feature.trackerdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerDetailViewModelTest {
    private val tracker = Tracker(
        id = "t",
        name = "Water",
        fields = listOf(FieldDefinition("g", "t", "Glasses", FieldConfig.Number())),
        createdAt = 0,
    )
    private val trackers = FakeTrackers(tracker)
    private val entries = FakeEntries()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = TrackerDetailViewModel(SavedStateHandle(mapOf(TrackerDetailViewModel.ARG_TRACKER_ID to "t")), trackers, entries)

    private fun entryOn(id: String, day: String) = Entry(
        id = id,
        trackerId = "t",
        occurredAt = LocalDate.parse(day).atTime(9, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        zoneId = "UTC",
        values = mapOf("g" to FieldValue.Number(2)),
        createdAt = 1,
    )

    @Test
    fun showsTheTrackerAndItsEntries() = runTest {
        entries.list.value = listOf(entryOn("a", "2026-11-10"), entryOn("b", "2026-11-09"))
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val state = vm.state.first { !it.loading }
        assertEquals("Water", state.tracker!!.name)
        assertEquals(2, state.groups.size)
    }

    @Test
    fun aDeletedTrackerShowsMissing() = runTest {
        trackers.current.value = null
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertTrue(vm.state.first { !it.loading }.missing)
    }

    @Test
    fun pickingADayFiltersAndPickingAgainClears() = runTest {
        entries.list.value = listOf(entryOn("a", "2026-11-10"), entryOn("b", "2026-11-09"))
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.setView(DetailView.CALENDAR)
        vm.selectDay(LocalDate.parse("2026-11-09"))
        assertEquals(listOf("b"), vm.state.value.groups.flatMap { g -> g.entries.map { it.id } })
        vm.selectDay(LocalDate.parse("2026-11-09"))
        assertNull(vm.state.value.selectedDay)
        assertEquals(2, vm.state.value.groups.size)
    }

    @Test
    fun movingToAnotherMonthDropsThePickedDay() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.showMonth(YearMonth.of(2026, 11))
        vm.selectDay(LocalDate.parse("2026-11-09"))
        vm.showMonth(YearMonth.of(2026, 10))
        assertNull(vm.state.value.selectedDay)
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
    }

    @Test
    fun deleteOffersUndoAndUndoRestoresTheSameEntry() = runTest {
        val gone = entryOn("a", "2026-11-10")
        entries.list.value = listOf(gone)
        val vm = vm()
        vm.eventFlow.test {
            vm.delete(gone)
            assertEquals(DetailEvent.Deleted(gone), awaitItem())
        }
        assertTrue(entries.list.value.isEmpty())
        vm.undoDelete(gone)
        assertEquals(listOf(gone), entries.list.value)
    }

    @Test
    fun longHistoriesArePagedButCountedInFull() = runTest {
        val start = LocalDate.parse("2026-01-01")
        entries.list.value = (0 until 100).map { entryOn("e$it", start.plusDays(it.toLong()).toString()) }
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(TrackerDetailViewModel.PAGE, vm.state.value.entries.size)
        assertEquals(100, vm.state.value.times.size)
        assertTrue(vm.state.value.hasMore)
        vm.showOlder()
        assertEquals(100, vm.state.value.entries.size)
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun aPickedDayFindsEntriesBeyondThePage() = runTest {
        val start = LocalDate.parse("2026-01-01")
        entries.list.value = (0 until 100).map { entryOn("e$it", start.plusDays(it.toLong()).toString()) }
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.selectDay(start)
        assertEquals(listOf("e0"), vm.state.value.groups.flatMap { g -> g.entries.map { it.id } })
        assertEquals("e0", vm.state.value.find("e0")!!.id)
    }

    @Test
    fun theWholeHistoryIsReadOnlyForInsights() = runTest {
        entries.list.value = listOf(entryOn("a", "2026-11-10"))
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertNull(vm.state.value.allEntries)
        vm.setView(DetailView.INSIGHTS)
        assertEquals(listOf("a"), vm.state.value.allEntries!!.map { it.id })
        vm.setView(DetailView.LIST)
        assertNull(vm.state.value.allEntries)
    }

    @Test
    fun archiveIsPassedThrough() = runTest {
        val vm = vm()
        vm.setArchived(true)
        assertTrue(trackers.current.value!!.archived)
        vm.setArchived(false)
        assertFalse(trackers.current.value!!.archived)
    }

    private class FakeTrackers(tracker: Tracker) : TrackerRepository {
        val current = MutableStateFlow<Tracker?>(tracker)
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
        override fun observeSummaries(): Flow<List<TrackerSummary>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = current
        override suspend fun getTracker(id: String) = current.value
        override suspend fun nextPosition() = 0
        override suspend fun saveTracker(tracker: Tracker) = ValidationResult.Valid
        override suspend fun setArchived(id: String, archived: Boolean) {
            current.value = current.value?.copy(archived = archived)
        }
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) = Unit
    }

    private class FakeEntries : EntryRepository {
        val list = MutableStateFlow<List<Entry>>(emptyList())
        override fun observeEntries(trackerId: String): Flow<List<Entry>> = list.map { all -> all.sortedByDescending { it.occurredAt } }
        override fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long): Flow<List<Entry>> =
            list.map { all -> all.filter { it.occurredAt in fromMillis until untilMillis }.sortedByDescending { it.occurredAt } }
        override fun observeRecentEntries(trackerId: String, limit: Int): Flow<List<Entry>> =
            list.map { all -> all.sortedByDescending { it.occurredAt }.take(limit) }
        override fun observeEntryTimes(trackerId: String): Flow<List<EntryTime>> =
            list.map { all -> all.sortedByDescending { it.occurredAt }.map { EntryTime(it.occurredAt, it.zoneId) } }
        override fun observeEntryTimesByTracker(): Flow<Map<String, List<EntryTime>>> = emptyFlow()
        override suspend fun getEntry(id: String) = list.value.firstOrNull { it.id == id }
        override suspend fun getLatestEntry(trackerId: String) = list.value.maxByOrNull { it.occurredAt }
        override suspend fun saveEntry(entry: Entry): ValidationResult {
            list.value = list.value.filterNot { it.id == entry.id } + entry
            return ValidationResult.Valid
        }
        override suspend fun deleteEntry(id: String) {
            list.value = list.value.filterNot { it.id == id }
        }
    }
}
