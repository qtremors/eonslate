package dev.qtremors.eonslate.feature.welcome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.validation.TrackerValidator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeTest {
    @get:Rule
    val compose = createComposeRule()

    private val repo = Repo()
    private val settings = Settings()
    private val clock = object : AppClock {
        override fun nowMillis() = 1_000L
        override fun zoneId() = "UTC"
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = WelcomeViewModel(handle, repo, settings, clock)

    private fun tracker(id: String) = Tracker(id = id, name = id, fields = listOf(FieldDefinition("$id-f", id, "F", FieldConfig.YesNo)), createdAt = 0)

    @Test
    fun startersArePlacedAfterExistingTrackersAndWelcomeIsSeen() = runTest {
        repo.next = 3
        val vm = vm()
        vm.finish { listOf(tracker("a"), tracker("b")) }
        assertEquals(listOf(3, 4), repo.saved.map { it.position })
        assertEquals(listOf(1_000L, 1_001L), repo.saved.map { it.createdAt })
        assertTrue(settings.seen.value)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun startersUseTheChosenCurrency() = runTest {
        var seen = ""
        vm().finish { currency -> seen = currency; emptyList() }
        assertEquals("JPY", seen)
    }

    @Test
    fun skippingSavesNothingButStillCounts() = runTest {
        vm().finish { emptyList() }
        assertTrue(repo.saved.isEmpty())
        assertTrue(settings.seen.value)
    }

    @Test
    fun picksAndPageSurviveProcessDeath() = runTest {
        val handle = SavedStateHandle()
        val first = vm(handle)
        first.showPage(1)
        first.toggle("water")
        first.toggle("mood")
        first.toggle("water")
        val restored = vm(handle).state.value
        assertEquals(1, restored.page)
        assertEquals(listOf("mood"), restored.picked)
    }

    @Test
    fun thePagesWalkFromIntroToStarters() {
        val vm = vm()
        compose.setContent { EonslateTheme { WelcomeScreen(onDone = {}, viewModel = vm) } }
        compose.onNodeWithText("Private by design").assertIsDisplayed()
        compose.onNodeWithText("Get started").performClick()
        compose.onNodeWithText("Pick a few to start").assertIsDisplayed()
        compose.onNodeWithText("Start with nothing").assertIsDisplayed()
        compose.onNodeWithText("Mood").performClick()
        compose.onNodeWithText("Start with 1 tracker").performClick()
        compose.waitForIdle()
        val saved = repo.saved.single()
        assertEquals("mood", saved.templateId)
        assertEquals("Mood", saved.name)
        assertEquals(ValidationResult.Valid, TrackerValidator.validate(saved))
    }

    @Test
    fun skippingFromTheFirstPageCreatesNothing() {
        val vm = vm()
        var done = 0
        compose.setContent { EonslateTheme { WelcomeScreen(onDone = { done++ }, viewModel = vm) } }
        compose.onNodeWithText("Skip").performClick()
        compose.waitForIdle()
        assertTrue(repo.saved.isEmpty())
        assertTrue(settings.seen.value)
        assertEquals(1, done)
    }

    @Test
    fun backOnTheSecondPageReturnsToTheFirst() = runTest {
        val vm = vm()
        vm.showPage(1)
        vm.showPage(0)
        assertEquals(0, vm.state.value.page)
        vm.showPage(5)
        assertEquals(1, vm.state.value.page)
    }

    private class Repo : TrackerRepository {
        val saved = mutableListOf<Tracker>()
        var next = 0
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
        override fun observeSummaries(): Flow<List<TrackerSummary>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = emptyFlow()
        override suspend fun getTracker(id: String): Tracker? = null
        override suspend fun nextPosition() = next
        override suspend fun saveTracker(tracker: Tracker): ValidationResult = ValidationResult.Valid.also { saved += tracker }
        override suspend fun setArchived(id: String, archived: Boolean) = Unit
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) = Unit
    }

    private class Settings : SettingsRepository by dev.qtremors.eonslate.core.settings.InMemorySettingsRepository() {
        override val homeTag: Flow<String?> = MutableStateFlow(null)
        override suspend fun setHomeTag(tag: String?) = Unit

        val seen = MutableStateFlow(false)
        override val welcomeSeen: Flow<Boolean> = seen
        override suspend fun setWelcomeSeen(seen: Boolean) {
            this.seen.value = seen
        }
        override val appearance: Flow<dev.qtremors.eonslate.core.settings.Appearance> = MutableStateFlow(dev.qtremors.eonslate.core.settings.Appearance())
        override suspend fun updateAppearance(change: (dev.qtremors.eonslate.core.settings.Appearance) -> dev.qtremors.eonslate.core.settings.Appearance) = Unit
        override val tracking: Flow<dev.qtremors.eonslate.core.settings.Tracking> = MutableStateFlow(dev.qtremors.eonslate.core.settings.Tracking(currency = "JPY"))
        override suspend fun updateTracking(change: (dev.qtremors.eonslate.core.settings.Tracking) -> dev.qtremors.eonslate.core.settings.Tracking) = Unit
    }
}
