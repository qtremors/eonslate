package dev.qtremors.eonslate.feature.reminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class RemindersUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val saved = mutableListOf<Triple<String?, Int, Set<DayOfWeek>>>()
    private val toggled = mutableListOf<Boolean>()
    private var turnedOn = 0

    private fun show(reminders: List<Reminder>, notificationsOn: Boolean = true) = compose.setContent {
        EonslateTheme {
            RemindersContent(
                state = RemindersUiState("Water", reminders),
                notificationsOn = notificationsOn,
                onBack = {},
                onSave = { id, minute, days, _ -> saved += Triple(id, minute, days) },
                onToggle = { _, on -> toggled += on },
                onDelete = {},
                onTurnOn = { turnedOn++ },
            )
        }
    }

    @Test
    fun noRemindersSaysSo() {
        show(emptyList())
        compose.onNodeWithText("No reminders").assertIsDisplayed()
        compose.onNodeWithText("Get a notification to log Water.").assertIsDisplayed()
    }

    @Test
    fun aReminderShowsItsTimeAndDays() {
        show(listOf(Reminder("r", "t", 20 * 60 + 30, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))))
        // The JDK puts a narrow no-break space before PM.
        compose.onNodeWithText("8:30", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Mon, Fri", substring = true).assertIsDisplayed()
    }

    @Test
    fun everyDayReadsAsOne() {
        show(listOf(Reminder("r", "t", 8 * 60)))
        compose.onNodeWithText("Every day").assertIsDisplayed()
    }

    @Test
    fun aNewReminderDefaultsToEveryEvening() {
        show(emptyList())
        compose.onNodeWithContentDescription("Add reminder").performClick()
        compose.onNode(hasText("Save") and hasClickAction()).performClick()
        assertEquals(listOf(Triple(null, 20 * 60, DayOfWeek.entries.toSet())), saved)
    }

    @Test
    fun aReminderNeedsADay() {
        show(emptyList())
        compose.onNodeWithContentDescription("Add reminder").performClick()
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("Pick at least one day").assertIsDisplayed()
        compose.onNode(hasText("Save") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun blockedNotificationsShowAWayToTurnThemOn() {
        show(listOf(Reminder("r", "t", 8 * 60)), notificationsOn = false)
        compose.onNodeWithText("Notifications are off").assertIsDisplayed()
        compose.onNodeWithText("Turn on").performClick()
        assertEquals(1, turnedOn)
        assertEquals(0, compose.onAllNodesWithText("No reminders").fetchSemanticsNodes().size)
    }
}
