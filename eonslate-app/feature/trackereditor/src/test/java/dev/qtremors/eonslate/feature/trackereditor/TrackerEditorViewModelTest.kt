package dev.qtremors.eonslate.feature.trackereditor

import androidx.lifecycle.SavedStateHandle
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.TrackerValidator
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerEditorViewModelTest {
    private val repo = FakeRepository()
    private val clock = object : AppClock {
        override fun nowMillis() = 1_000L
        override fun zoneId() = "UTC"
    }
    private var counter = 0
    private val ids = IdGenerator { "id${++counter}" }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newEditor(handle: SavedStateHandle = SavedStateHandle()) = TrackerEditorViewModel(handle, repo, clock, ids)

    private fun TrackerEditorViewModel.addYesNo(name: String) {
        addField()
        updateSheet { it.copy(name = name) }
        setFieldType(FieldType.YES_NO)
        commitSheet()
    }

    @Test
    fun newTrackerSavesAtTheEnd() = runTest {
        repo.next = 7
        val vm = newEditor()
        vm.setName("Vitamins")
        vm.addYesNo("Taken")
        vm.save()

        val saved = repo.saved.single()
        assertEquals("Vitamins", saved.name)
        assertEquals(7, saved.position)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(FieldConfig.YesNo, saved.fields.single().config)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun saveReportsEveryProblemWithoutLeaving() = runTest {
        val vm = newEditor()
        vm.save()
        val errors = vm.state.value.errors
        assertTrue(ValidationError.TrackerNameBlank in errors)
        assertTrue(ValidationError.NoFields in errors)
        assertFalse(vm.state.value.done)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun sheetRefusesABadFieldAndKeepsIt() = runTest {
        val vm = newEditor()
        vm.addField()
        vm.commitSheet()
        assertTrue(vm.state.value.sheetErrors.any { it is ValidationError.FieldNameBlank })
        assertNotNull(vm.state.value.sheet)
        assertTrue(vm.state.value.draft!!.fields.isEmpty())
    }

    @Test
    fun sheetRefusesADuplicateName() = runTest {
        val vm = newEditor()
        vm.addYesNo("Done")
        vm.addField()
        vm.updateSheet { it.copy(name = " done ") }
        vm.commitSheet()
        assertTrue(vm.state.value.sheetErrors.any { it is ValidationError.DuplicateFieldName })
    }

    @Test
    fun sheetReportsUnreadableLimits() = runTest {
        val vm = newEditor()
        vm.addField()
        vm.updateSheet { it.copy(name = "Weight", decimals = 0, max = "1.5") }
        vm.commitSheet()
        assertTrue(vm.state.value.sheetLimits.maxInvalid)
    }

    @Test
    fun choiceFieldStartsWithTwoOptions() = runTest {
        val vm = newEditor()
        vm.addField()
        vm.setFieldType(FieldType.MULTI_CHOICE)
        assertEquals(2, vm.state.value.sheet!!.draft.options.size)
        vm.addOption()
        val third = vm.state.value.sheet!!.draft.options[2].id
        vm.removeOption(third)
        assertEquals(2, vm.state.value.sheet!!.draft.options.size)
    }

    @Test
    fun fieldsMoveAndClampAtTheEnds() = runTest {
        val vm = newEditor()
        listOf("A", "B", "C").forEach { vm.addYesNo(it) }
        val ids = vm.state.value.draft!!.fields.map { it.id }
        vm.moveField(ids[2], -1)
        assertEquals(listOf("A", "C", "B"), vm.state.value.draft!!.fields.map { it.name })
        vm.moveField(ids[0], -5)
        assertEquals(listOf("A", "C", "B"), vm.state.value.draft!!.fields.map { it.name })
        vm.removeField(ids[1])
        assertEquals(listOf("A", "C"), vm.state.value.draft!!.fields.map { it.name })
    }

    @Test
    fun editingKeepsCreationAndRemembersTypes() = runTest {
        val stored = Tracker(
            id = "t",
            name = "Mood",
            fields = listOf(FieldDefinition("r", "t", "Mood", FieldConfig.Rating(5))),
            position = 2,
            createdAt = 10,
        )
        repo.trackers["t"] = stored
        val vm = newEditor(SavedStateHandle(mapOf(TrackerEditorViewModel.ARG_TRACKER_ID to "t")))
        assertEquals(mapOf("r" to FieldType.RATING), vm.state.value.originalTypes)
        vm.setName("Daily mood")
        vm.save()
        val saved = repo.saved.single()
        assertEquals(10L, saved.createdAt)
        assertEquals(2, saved.position)
        assertEquals("Daily mood", saved.name)
    }

    @Test
    fun missingTrackerShowsMissing() = runTest {
        val vm = newEditor(SavedStateHandle(mapOf(TrackerEditorViewModel.ARG_TRACKER_ID to "gone")))
        assertTrue(vm.state.value.missing)
    }

    @Test
    fun draftAndOpenSheetSurviveProcessDeath() = runTest {
        val handle = SavedStateHandle()
        val vm = newEditor(handle)
        vm.setName("Half done")
        vm.addField()
        vm.updateSheet { it.copy(name = "Typing") }

        val restored = newEditor(handle)
        assertEquals("Half done", restored.state.value.draft!!.name)
        assertEquals("Typing", restored.state.value.sheet!!.draft.name)
        assertTrue(restored.state.value.dirty)
    }

    @Test
    fun deletingANewTrackerTouchesNothing() = runTest {
        val vm = newEditor()
        vm.delete()
        assertTrue(vm.state.value.done)
        assertTrue(repo.deleted.isEmpty())
        assertNull(vm.state.value.sheet)
    }

    private class FakeRepository : TrackerRepository {
        val trackers = mutableMapOf<String, Tracker>()
        val saved = mutableListOf<Tracker>()
        val deleted = mutableListOf<String>()
        var next = 0

        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
        override fun observeSummaries(): Flow<List<TrackerSummary>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = emptyFlow()
        override suspend fun getTracker(id: String) = trackers[id]
        override suspend fun nextPosition() = next
        override suspend fun saveTracker(tracker: Tracker): ValidationResult =
            TrackerValidator.validate(tracker).also { if (it.isValid) saved += tracker }
        override suspend fun setArchived(id: String, archived: Boolean) = Unit
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) {
            deleted += id
        }
    }
}
