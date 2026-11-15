package dev.qtremors.eonslate.feature.trackerdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class MonthCalendarUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 10)
    private var month by mutableStateOf(YearMonth.of(2026, 11))
    private var selected by mutableStateOf<LocalDate?>(null)

    private fun show(counts: Map<LocalDate, Int>) {
        compose.setContent {
            EonslateTheme {
                MonthCalendar(
                    month = month,
                    counts = counts,
                    colorIndex = 3,
                    selected = selected,
                    today = today,
                    firstDay = java.time.DayOfWeek.SUNDAY,
                    onMonth = { month = it },
                    onDay = { selected = if (selected == it) null else it },
                )
            }
        }
    }

    @Test
    fun daysWithEntriesSayHowMany() {
        show(mapOf(LocalDate.of(2026, 11, 9) to 2))
        compose.onNodeWithText("November 2026").assertExists()
        compose.onNodeWithContentDescription("Monday, November 9, 2026, 2 entries").performClick()
        assertEquals(LocalDate.of(2026, 11, 9), selected)
        compose.onNodeWithContentDescription("Monday, November 9, 2026, 2 entries").assertIsSelected()
    }

    @Test
    fun futureDaysCannotBePicked() {
        show(emptyMap())
        compose.onNodeWithContentDescription("Wednesday, November 11, 2026").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Tuesday, November 10, 2026").assertIsEnabled()
    }

    @Test
    fun monthsMoveBackButNotPastThisMonth() {
        show(emptyMap())
        compose.onNodeWithContentDescription("Next month").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous month").performClick()
        assertEquals(YearMonth.of(2026, 10), month)
        compose.onNodeWithText("October 2026").assertExists()
        compose.onNodeWithContentDescription("Next month").assertIsEnabled()
    }

    @Test
    fun aFarMonthOffersTheWayBack() {
        month = YearMonth.of(2025, 3)
        show(emptyMap())
        compose.onNodeWithText("Back to this month").performClick()
        assertEquals(YearMonth.of(2026, 11), month)
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText("Back to this month")).fetchSemanticsNodes().size)
    }
}
