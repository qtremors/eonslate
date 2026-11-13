package dev.qtremors.eonslate.feature.home

import app.cash.turbine.test
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val summaries = MutableSharedFlow<List<TrackerSummary>>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadingThenEmptyThenTrackers() = runTest {
        val viewModel = HomeViewModel(FakeRepository(summaries))
        val water = TrackerSummary(Tracker(id = "w", name = "Water", createdAt = 0), entryCount = 2, lastEntryAt = 10)

        viewModel.trackers.test {
            assertNull("Loading must not look empty", awaitItem())
            summaries.emit(emptyList())
            assertEquals(emptyList<TrackerSummary>(), awaitItem())
            summaries.emit(listOf(water))
            assertEquals(listOf(water), awaitItem())
        }
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
