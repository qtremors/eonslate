package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class ChartsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var selected by mutableStateOf<Int?>(null)

    @Test
    fun tappingABarPicksItAndTappingAgainLetsGo() {
        compose.setContent {
            EonslateTheme {
                BarChart(
                    values = listOf(1f, 3f, 2f, 0f),
                    color = Color.Red,
                    contentDescription = "Bars",
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.width(400.dp),
                )
            }
        }
        val node = compose.onNodeWithContentDescription("Bars")
        node.performTouchInput { click(Offset(width * 0.6f, height / 2f)) }
        assertEquals(2, selected)
        node.performTouchInput { click(Offset(width * 0.6f, height / 2f)) }
        assertNull(selected)
        node.performTouchInput { click(Offset(width * 0.1f, height / 2f)) }
        assertEquals(0, selected)
    }

    @Test
    fun aLineIgnoresTapsOnMissingDays() {
        compose.setContent {
            EonslateTheme {
                LineChart(
                    values = listOf(70f, null, 71f),
                    color = Color.Red,
                    contentDescription = "Line",
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.width(300.dp),
                )
            }
        }
        val node = compose.onNodeWithContentDescription("Line")
        node.performTouchInput { click(Offset(width * 0.5f, height / 2f)) }
        assertNull(selected)
        node.performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
        assertEquals(2, selected)
    }
}
