package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MonoAndSignalsTest {
    @Test
    fun monoIsWhiteOnNearBlackAndPureBlackTakesItAllTheWay() {
        val dark = monoColorScheme(isDark = true)
        assertEquals(Color.White, dark.primary)
        assertNotEquals(Color.Black, dark.surface)
        assertEquals(Color.Black, dark.toOled().surface)
    }

    @Test
    fun monoIsBlackOnWhiteInTheLight() {
        val light = monoColorScheme(isDark = false)
        assertEquals(Color.Black, light.primary)
        assertEquals(Color.White, light.surface)
    }

    @Test
    fun streaksKeepEmberInMono() {
        assertEquals(seedColorScheme(EmberSeed, true).primary, monoColorScheme(isDark = true).tertiary)
    }

    @Test
    fun signalsLeanTowardAColoredAccentButNotTowardGrey() {
        val mono = signalsFor(Color.White, isDark = true)
        assertEquals(mono, signalsFor(Color.Black, isDark = true))
        val dusk = signalsFor(presetColorScheme(ThemePreset.DUSK, true).primary, isDark = true)
        assertNotEquals(mono.positive, dusk.positive)
    }
}
