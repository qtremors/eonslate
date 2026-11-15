package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import dev.qtremors.eonslate.core.tracker.ui.Templates
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class TemplateGalleryUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val picks = mutableListOf<String?>()
    private var back = 0

    private fun show() = compose.setContent {
        EonslateTheme {
            TemplateGalleryScreen(
                onPick = { picks += it },
                onBack = { back++ },
                templates = Templates.all.take(3),
            )
        }
    }

    @Test
    fun thePreviewListsEveryField() {
        show()
        compose.onNodeWithText("Expenses").performClick()
        compose.onNodeWithText("Amount").assertExists()
        compose.onNodeWithText("Category").assertExists()
        compose.onNodeWithText("One choice · Food, Transport, Shopping, Bills, Fun, Other").assertExists()
    }

    @Test
    fun templatesSitOnTheirShelves() {
        show()
        compose.onNodeWithText("Money and time").assertExists()
        compose.onNodeWithText("Mind and habits").assertExists()
    }

    @Test
    fun searchNarrowsTheGallery() {
        show()
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("streak")
        compose.onNodeWithText("Daily habit").assertExists()
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText("Expenses")).fetchSemanticsNodes().size)
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextReplacement("zzz")
        compose.onNodeWithText("No template matches").assertExists()
        // Blank is always there to start from.
        compose.onNodeWithText("Blank").assertExists()
    }

    @Test
    fun blankComesFirstAndPicksNoTemplate() {
        show()
        compose.onNodeWithText("Blank").performClick()
        assertEquals(listOf<String?>(null), picks)
    }

    @Test
    fun aTemplateSaysWhatItHoldsAndPicksItsId() {
        show()
        compose.onNodeWithText("What you spend, by category\n2 fields").assertIsDisplayed()
        compose.onNodeWithText("Daily habit").performClick()
        // A template opens its preview first; it is used from there.
        assertEquals(emptyList<String?>(), picks)
        compose.onNodeWithText("Use this template").performClick()
        assertEquals(listOf<String?>("habit"), picks)
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
    }
}
