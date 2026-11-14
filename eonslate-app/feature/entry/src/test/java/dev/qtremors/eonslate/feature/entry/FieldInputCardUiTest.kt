package dev.qtremors.eonslate.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The log screen's inputs, driven the way a person would: typing, tapping, clearing. */
@RunWith(RobolectricTestRunner::class)
class FieldInputCardUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var input: FieldInput? by mutableStateOf(null)

    private fun show(field: FieldDefinition, error: String? = null) {
        compose.setContent {
            EonslateTheme {
                FieldInputCard(
                    field = field,
                    input = input,
                    error = error,
                    onChange = { change -> input = change(input ?: FieldInput()) },
                    onClear = { input = null },
                )
            }
        }
    }

    @Test
    fun numberFieldTakesTypedText() {
        show(FieldDefinition("w", "t", "Weight", FieldConfig.Number("kg", 1)))
        compose.onNodeWithText("Weight").assertIsDisplayed()
        compose.onNodeWithText("Value").performTextInput("72.5")
        assertEquals("72.5", input?.text)
    }

    @Test
    fun durationSplitsHoursAndMinutesAndDropsLetters() {
        show(FieldDefinition("d", "t", "Slept", FieldConfig.Duration()))
        compose.onNodeWithText("Hours").performTextInput("7a")
        compose.onNodeWithText("Minutes").performTextInput("15")
        assertEquals("7", input?.text)
        assertEquals("15", input?.text2)
    }

    @Test
    fun ratingIsPickedAndCleared() {
        show(FieldDefinition("r", "t", "Mood", FieldConfig.Rating(5)))
        compose.onNodeWithText("4").performClick()
        assertEquals(4, input?.number)
        compose.onNodeWithText("Clear").performClick()
        assertNull(input)
    }

    @Test
    fun yesNoUsesWords() {
        show(FieldDefinition("y", "t", "Took vitamins", FieldConfig.YesNo))
        compose.onNodeWithText("No").performClick()
        assertEquals(0, input?.number)
        compose.onNodeWithText("Yes").performClick()
        assertEquals(1, input?.number)
    }

    @Test
    fun multiChoiceTogglesEachOption() {
        val options = listOf(ChoiceOption("a", "Calm"), ChoiceOption("b", "Tired"))
        show(FieldDefinition("c", "t", "Feelings", FieldConfig.MultiChoice(options)))
        compose.onNodeWithText("Calm").performClick()
        compose.onNodeWithText("Tired").performClick()
        compose.onNodeWithText("Calm").performClick()
        assertEquals(setOf("b"), input?.choices)
        assertEquals(true, input?.touched)
    }

    @Test
    fun requiredFieldsAndProblemsAreShown() {
        show(FieldDefinition("w", "t", "Weight", FieldConfig.Number(), required = true), error = "Fill this in")
        compose.onNodeWithText("Weight").assertIsDisplayed()
        compose.onNodeWithText("Required").assertIsDisplayed()
        compose.onNodeWithText("Fill this in").assertIsDisplayed()
    }
}
