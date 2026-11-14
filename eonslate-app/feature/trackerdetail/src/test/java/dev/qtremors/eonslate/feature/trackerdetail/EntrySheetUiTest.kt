package dev.qtremors.eonslate.feature.trackerdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class EntrySheetUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val tracker = Tracker(
        id = "t",
        name = "Run",
        fields = listOf(
            FieldDefinition("k", "t", "Distance", FieldConfig.Number("km", 2)),
            FieldDefinition("d", "t", "Time", FieldConfig.Duration()),
            FieldDefinition("e", "t", "Effort", FieldConfig.Rating(5)),
            FieldDefinition("s", "t", "Shoes", FieldConfig.Text()),
            FieldDefinition("w", "t", "Weather", FieldConfig.Text()),
        ),
        createdAt = 0,
    )
    private val paris = ZonedDateTime.of(2026, 11, 9, 7, 15, 0, 0, ZoneId.of("Europe/Paris"))
    private val entry = Entry(
        id = "e",
        trackerId = "t",
        occurredAt = paris.toInstant().toEpochMilli(),
        zoneId = "Europe/Paris",
        values = mapOf(
            "k" to FieldValue.Number(1_002),
            "d" to FieldValue.Duration(3_000),
            "e" to FieldValue.Rating(5),
            "s" to FieldValue.Text("Trail pair"),
        ),
        note = "Felt strong on the hills",
        createdAt = 1,
    )
    private var edits = 0
    private var deletes = 0
    private var dismissed = false

    private fun show(deviceZone: ZoneId) {
        compose.setContent {
            EonslateTheme {
                EntrySheet(
                    entry = entry,
                    tracker = tracker,
                    onEdit = { edits++ },
                    onDelete = { deletes++ },
                    onDismiss = { dismissed = true },
                    deviceZone = deviceZone,
                )
            }
        }
    }

    @Test
    fun showsEveryFilledFieldAndTheNote() {
        show(ZoneId.of("Europe/Paris"))
        compose.onNodeWithText("10.02 km").assertIsDisplayed()
        compose.onNodeWithText("50 m").assertIsDisplayed()
        compose.onNodeWithText("5/5").assertIsDisplayed()
        compose.onNodeWithText("Trail pair").assertIsDisplayed()
        compose.onNodeWithText("Felt strong on the hills").assertExists()
        // An empty field is left out rather than shown blank.
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText("Weather")).fetchSemanticsNodes().size)
        // The same zone as the phone needs no zone line.
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText("Logged in", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun anotherZoneIsNamed() {
        show(ZoneId.of("Asia/Kolkata"))
        compose.onNodeWithText("Logged in Europe/Paris").assertIsDisplayed()
    }

    @Test
    fun editAndDeleteAreOffered() {
        show(ZoneId.of("Europe/Paris"))
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(1, edits)
        assertEquals(1, deletes)
        assertTrue(!dismissed)
    }
}
