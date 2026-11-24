package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The builder's pickers and field sheet, driven through the real view model. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE)
class TrackerEditorUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pigmentPickerSelectsByName() {
        var selected by mutableIntStateOf(0)
        compose.setContent { EonslateTheme { PigmentPicker(selected = selected, onSelect = { selected = it }) } }
        compose.onNodeWithContentDescription("Tide").performClick()
        assertEquals(3, selected)
        compose.onNodeWithContentDescription("Tide").assertIsSelected()
    }

    @Test
    fun iconPickerSelectsByName() {
        var selected by mutableStateOf(Tracker.DEFAULT_ICON)
        compose.setContent { EonslateTheme { IconPicker(selected = selected, colorIndex = 0, onSelect = { selected = it }) } }
        compose.onNodeWithContentDescription("Water").performClick()
        assertEquals("LocalDrink", selected)
        compose.onNodeWithContentDescription("Water").assertIsSelected()
    }

    @Test
    fun choiceFieldIsBuiltInTheSheet() {
        var counter = 0
        val vm = TrackerEditorViewModel(
            SavedStateHandle(),
            NoTrackers,
            object : AppClock {
                override fun nowMillis() = 0L
                override fun zoneId() = "UTC"
            },
            IdGenerator { "id${++counter}" },
            dev.qtremors.eonslate.core.settings.InMemorySettingsRepository(),
        )
        vm.addField()
        compose.setContent {
            EonslateTheme {
                val state by vm.state.collectAsState()
                state.sheet?.let { sheet ->
                    FieldSheetContent(
                        sheet = sheet,
                        originalType = null,
                        errors = state.sheetErrors,
                        limits = state.sheetLimits,
                        viewModel = vm,
                    )
                }
            }
        }
        compose.onNodeWithText("Field name").performTextInput("Mood")
        compose.onNodeWithText("One choice").performClick()
        compose.onNodeWithText("Choice 1").performTextInput("Good")
        compose.onNodeWithText("Choice 2").performTextInput("Low")
        compose.onNodeWithText("Add").performClick()

        val field = vm.state.value.draft!!.fields.single()
        assertEquals("Mood", field.name)
        assertEquals(FieldType.SINGLE_CHOICE, field.type)
        val built = field.build("t").field!!.config as FieldConfig.SingleChoice
        assertEquals(listOf("Good", "Low"), built.options.map { it.label })
    }

    private object NoTrackers : TrackerRepository {
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
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

/** A typical phone, so screens lay out as they do on a device. */
private const val PHONE = "en-rUS-w411dp-h891dp-xxhdpi"
