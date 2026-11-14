package dev.qtremors.eonslate.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalCoroutinesApi::class)
class ArchivedTest {
    @get:Rule
    val compose = createComposeRule()

    private val active = Tracker(id = "a", name = "Water", createdAt = 0)
    private val old = Tracker(id = "o", name = "Old habit", archived = true, createdAt = 0)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun viewModelKeepsOnlyArchivedTrackers() = runTest {
        val all = MutableStateFlow(listOf(active, old))
        val vm = ArchivedViewModel(Repo(all))
        val shown = vm.trackers.filterNotNull().first()
        assertEquals(listOf("o"), shown.map { it.id })
    }

    @Test
    fun archivedSlatesOpenTheirPage() {
        val opened = mutableListOf<String>()
        var back = 0
        compose.setContent {
            EonslateTheme { ArchivedContent(trackers = listOf(old), onBack = { back++ }, onOpenTracker = { opened += it }) }
        }
        compose.onNodeWithText("Archived. Open it to bring it back.").assertIsDisplayed()
        compose.onNodeWithText("Old habit").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("o"), opened)
        assertEquals(1, back)
    }

    @Test
    fun nothingArchivedSaysSo() {
        compose.setContent { EonslateTheme { ArchivedContent(trackers = emptyList(), onBack = {}, onOpenTracker = {}) } }
        compose.onNodeWithText("Nothing archived").assertIsDisplayed()
    }

    private class Repo(private val all: Flow<List<Tracker>>) : TrackerRepository {
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> =
            if (includeArchived) all else all.map { list -> list.filterNot { it.archived } }
        override fun observeSummaries(): Flow<List<TrackerSummary>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = emptyFlow()
        override suspend fun getTracker(id: String): Tracker? = null
        override suspend fun nextPosition() = 0
        override suspend fun saveTracker(tracker: Tracker) = ValidationResult.Valid
        override suspend fun setArchived(id: String, archived: Boolean) = Unit
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) = Unit
    }
}
