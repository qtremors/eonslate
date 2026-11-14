package dev.qtremors.eonslate.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE)
class HomeContentUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val now = 10_000_000L
    private val stretch = Tracker(
        id = "s",
        name = "Stretch",
        fields = listOf(FieldDefinition("d", "s", "Done", FieldConfig.YesNo)),
        createdAt = 0,
    )
    private val run = Tracker(
        id = "r",
        name = "Run",
        fields = listOf(FieldDefinition("k", "r", "Distance", FieldConfig.Number("km", 2))),
        createdAt = 0,
    )

    private val moves = mutableListOf<Pair<String, Int>>()
    private val archived = mutableListOf<String>()
    private val edited = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private val logged = mutableListOf<String>()
    private var created = 0

    private fun show(trackers: List<TrackerSummary>?) {
        compose.setContent {
            EonslateTheme {
                HomeContent(
                    trackers = trackers,
                    onNewTracker = { created++ },
                    onOpenTracker = { opened += it },
                    onLog = { logged += it.id },
                    actions = SlateActions(
                        onMove = { t, by -> moves += t.id to by },
                        onEdit = { edited += it.id },
                        onArchive = { archived += it.id },
                    ),
                    nowMillis = now,
                )
            }
        }
    }

    @Test
    fun loadingNeverLooksEmpty() {
        show(null)
        assertTrue(compose.onAllNodesWithTextCount("Nothing tracked yet") == 0)
    }

    @Test
    fun emptyHomeOffersToCreate() {
        show(emptyList())
        compose.onNodeWithText("Nothing tracked yet").assertIsDisplayed()
        compose.onNodeWithText("Create a tracker").performClick()
        assertEquals(1, created)
    }

    @Test
    fun slatesShowWhatWasLoggedAndOpenOnTap() {
        show(
            listOf(
                TrackerSummary(stretch, entryCount = 0, lastEntryAt = null),
                TrackerSummary(run, entryCount = 3, lastEntryAt = now - 30_000),
            )
        )
        compose.onNodeWithText("Nothing logged yet").assertIsDisplayed()
        compose.onNodeWithText("3 entries, latest just now").assertIsDisplayed()
        compose.onNodeWithText("Run").performClick()
        assertEquals(listOf("r"), opened)
    }

    @Test
    fun eachSlateHasItsOwnLogButton() {
        show(listOf(TrackerSummary(stretch, 0, null), TrackerSummary(run, 0, null)))
        compose.onNodeWithContentDescription("Log Stretch").performClick()
        compose.onNodeWithContentDescription("Log Run").performClick()
        assertEquals(listOf("s", "r"), logged)
        // Screen readers meet the main action by its name.
        compose.onNodeWithContentDescription("New tracker").assertExists().performClick()
        assertEquals(1, created)
    }

    @Test
    fun longPressOffersMoveEditAndArchive() {
        show(listOf(TrackerSummary(stretch, 0, null), TrackerSummary(run, 0, null)))
        compose.onNodeWithText("Stretch").performTouchInput { longClick() }
        compose.onNodeWithText("Move earlier").assertIsNotEnabled()
        compose.onNodeWithText("Move later").performClick()
        assertEquals(listOf("s" to 1), moves)

        compose.onNodeWithText("Run").performTouchInput { longClick() }
        compose.onNodeWithText("Move later").assertIsNotEnabled()
        compose.onNodeWithText("Archive").performClick()
        assertEquals(listOf("r"), archived)

        compose.onNodeWithText("Run").performTouchInput { longClick() }
        compose.onNodeWithText("Edit").performClick()
        assertEquals(listOf("r"), edited)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String): Int =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
}

/** A typical phone, so screens lay out as they do on a device. */
private const val PHONE = "en-rUS-w411dp-h891dp-xxhdpi"
