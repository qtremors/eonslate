package dev.qtremors.eonslate.feature.trackerdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class InsightsViewUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 10)
    private val tracker = Tracker(
        id = "t",
        name = "Habit",
        fields = listOf(
            FieldDefinition("y", "t", "Done", FieldConfig.YesNo),
            FieldDefinition("c", "t", "Where", FieldConfig.SingleChoice(listOf(ChoiceOption("h", "Home"), ChoiceOption("g", "Gym")))),
            FieldDefinition("w", "t", "Weight", FieldConfig.Number("kg", 1)),
        ),
        createdAt = 0,
    )

    private fun entry(back: Long, values: Map<String, FieldValue>) = Entry(
        id = "e$back",
        trackerId = "t",
        occurredAt = today.minusDays(back).atTime(9, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        zoneId = "UTC",
        values = values,
        createdAt = 0,
    )

    private val entries = listOf(
        entry(0, mapOf("y" to FieldValue.YesNo(true), "c" to FieldValue.SingleChoice("g"), "w" to FieldValue.Number(725))),
        entry(1, mapOf("y" to FieldValue.YesNo(true), "c" to FieldValue.SingleChoice("g"), "w" to FieldValue.Number(730))),
        entry(2, mapOf("y" to FieldValue.YesNo(false), "c" to FieldValue.SingleChoice("h"))),
    )

    private fun show() {
        val state = DetailState(
            loading = false,
            tracker = tracker,
            entries = entries,
            times = entries.map { EntryTime(it.occurredAt, it.zoneId) },
            allEntries = entries,
            view = DetailView.INSIGHTS,
        )
        compose.setContent { EonslateTheme { InsightsView(state = state, tracker = tracker, today = today) } }
    }

    @Test
    fun streaksAndActivityAreRead() {
        show()
        compose.onAllNodesWithText("3 days")[0].assertIsDisplayed()
        compose.onNodeWithText("Current streak").assertIsDisplayed()
        compose.onNodeWithText("Still going").assertIsDisplayed()
        compose.onNodeWithText("3 entries in the last 30 days").assertIsDisplayed()
        compose.onNodeWithText("The same as yesterday").assertExists()
    }

    @Test
    fun eachFieldSaysWhatItAddsUpTo() {
        show()
        compose.onNodeWithText("67% yes, 2 of 3").assertExists()
        compose.onNodeWithText("Answered 3 times").assertExists()
        compose.onNodeWithText("Average 72.75 kg, from 72.5 kg to 73.0 kg").assertExists()
        compose.onNodeWithText("Record 73.0 kg, on Nov 9, 2026").assertExists()
    }

    @Test
    fun aBrokenStreakSaysWhenItEnded() {
        val old = listOf(entry(5, emptyMap()), entry(6, emptyMap()), entry(7, emptyMap()), entry(1, emptyMap()))
        val state = DetailState(
            loading = false,
            tracker = tracker,
            entries = old,
            times = old.map { EntryTime(it.occurredAt, it.zoneId) },
            allEntries = old,
            view = DetailView.INSIGHTS,
        )
        compose.setContent { EonslateTheme { InsightsView(state = state, tracker = tracker, today = today) } }
        compose.onNodeWithText("Ended Nov 5").assertIsDisplayed()
        compose.onNodeWithText("1 day").assertIsDisplayed()
    }

    @Test
    fun weeksGroupTheHistory() {
        show()
        compose.onNodeWithText("12 weeks").performClick()
        compose.onNodeWithText("3 entries in the last 12 weeks").assertIsDisplayed()
        compose.onNodeWithText("This week").assertExists()
        compose.onNodeWithText("Logged per week").assertExists()
    }

    @Test
    fun everyChartHasATableView() {
        show()
        compose.onAllNodesWithText("Table")[0].performClick()
        // The activity table lists the logged days, newest first.
        compose.onNodeWithText("Chart").assertIsDisplayed()
        compose.onAllNodesWithText("1")[0].assertExists()
    }
}
