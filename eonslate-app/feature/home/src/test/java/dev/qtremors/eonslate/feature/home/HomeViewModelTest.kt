package dev.qtremors.eonslate.feature.home

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
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val summaries = MutableSharedFlow<List<TrackerSummary>>()
    private val entries = FakeEntries()
    private val clock = object : AppClock {
        override fun nowMillis() = 5_000L
        override fun zoneId() = "Asia/Kolkata"
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadingThenEmptyThenTrackers() = runTest {
        val viewModel = HomeViewModel(FakeRepository(summaries), entries, clock, IdGenerator { "e1" })
        val water = TrackerSummary(Tracker(id = "w", name = "Water", createdAt = 0), entryCount = 2, lastEntryAt = 10)

        viewModel.trackers.test {
            assertNull("Loading must not look empty", awaitItem())
            summaries.emit(emptyList())
            assertEquals(emptyList<TrackerSummary>(), awaitItem())
            summaries.emit(listOf(water))
            assertEquals(listOf(water), awaitItem())
        }
    }

    @Test
    fun quickLogTicksASingleYesNoTracker() = runTest {
        val viewModel = HomeViewModel(FakeRepository(summaries), entries, clock, IdGenerator { "e1" })
        val habit = Tracker(id = "h", name = "Stretch", fields = listOf(FieldDefinition("d", "h", "Done", FieldConfig.YesNo)), createdAt = 0)
        val weight = Tracker(id = "w", name = "Weight", fields = listOf(FieldDefinition("k", "w", "Kg", FieldConfig.Number())), createdAt = 0)

        assertTrue(HomeViewModel.isQuickLog(habit))
        assertFalse(HomeViewModel.isQuickLog(weight))
        viewModel.quickLog(weight)
        viewModel.quickLog(habit)

        val logged = entries.saved.single()
        assertEquals("h", logged.trackerId)
        assertEquals(mapOf("d" to FieldValue.YesNo(true)), logged.values)
        assertEquals(5_000L, logged.occurredAt)
        assertEquals("Asia/Kolkata", logged.zoneId)
    }

    @Test
    fun movingKeepsEveryoneElsesOrder() {
        val ids = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", "c", "b", "d"), HomeViewModel.moved(ids, "c", -1))
        assertEquals(listOf("b", "c", "d", "a"), HomeViewModel.moved(ids, "a", 10))
        assertEquals(null, HomeViewModel.moved(ids, "a", -1))
        assertEquals(null, HomeViewModel.moved(ids, "ghost", 1))
    }

    private class FakeEntries : EntryRepository {
        val saved = mutableListOf<Entry>()
        override fun observeEntries(trackerId: String): Flow<List<Entry>> = emptyFlow()
        override fun observeEntriesBetween(trackerId: String, fromMillis: Long, untilMillis: Long): Flow<List<Entry>> = emptyFlow()
        override fun observeRecentEntries(trackerId: String, limit: Int): Flow<List<Entry>> = emptyFlow()
        override fun observeEntryTimes(trackerId: String): Flow<List<EntryTime>> = emptyFlow()
        override suspend fun getEntry(id: String): Entry? = null
        override suspend fun getLatestEntry(trackerId: String): Entry? = null
        override suspend fun saveEntry(entry: Entry): ValidationResult = ValidationResult.Valid.also { saved += entry }
        override suspend fun deleteEntry(id: String) = Unit
    }

    private class FakeRepository(private val summaries: Flow<List<TrackerSummary>>) : TrackerRepository {
        override fun observeSummaries() = summaries
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = emptyFlow()
        override suspend fun getTracker(id: String): Tracker? = null
        override suspend fun nextPosition() = 0
        override suspend fun saveTracker(tracker: Tracker) = ValidationResult.Valid
        override suspend fun setArchived(id: String, archived: Boolean) = Unit
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) = Unit
    }
}
