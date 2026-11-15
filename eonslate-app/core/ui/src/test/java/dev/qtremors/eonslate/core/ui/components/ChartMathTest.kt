package dev.qtremors.eonslate.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartMathTest {
    @Test
    fun tapsLandOnTheirColumn() {
        assertEquals(0, chartIndexAt(0f, 300f, 30))
        assertEquals(29, chartIndexAt(300f, 300f, 30))
        assertEquals(14, chartIndexAt(149f, 300f, 30))
        assertNull(chartIndexAt(-1f, 300f, 30))
        assertNull(chartIndexAt(10f, 300f, 0))
    }

    @Test
    fun theValueRangeAlwaysHoldsZero() {
        assertEquals(0f..5f, chartRange(listOf(2f, 5f)))
        assertEquals(-3f..4f, chartRange(listOf(-3f, 4f)))
        assertEquals(-6f..0f, chartRange(listOf(-6f, -1f)))
        // A flat chart still has room to draw.
        assertEquals(0f..1f, chartRange(listOf(0f, 0f)))
        assertEquals(0f..1f, chartRange(emptyList()))
    }
}
