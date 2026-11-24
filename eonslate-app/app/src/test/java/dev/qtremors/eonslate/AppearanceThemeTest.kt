package dev.qtremors.eonslate

import dev.qtremors.eonslate.core.settings.AccentChoice
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.ThemeChoice
import dev.qtremors.eonslate.core.ui.theme.AccentSource
import dev.qtremors.eonslate.core.ui.theme.ThemeMode
import dev.qtremors.eonslate.core.ui.theme.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeTest {
    @Test
    fun theDefaultIsEonslatesOwnLook() {
        val options = Appearance().toThemeOptions()
        assertEquals(ThemeMode.SYSTEM, options.mode)
        assertEquals(AccentSource.PRESET, options.accentSource)
        assertEquals(ThemePreset.MONO, options.preset)
        assertEquals(true, options.oled)
        assertEquals(true, options.hapticsEnabled)
    }

    @Test
    fun everyChoiceCarriesOver() {
        val options = Appearance(
            theme = ThemeChoice.DARK,
            pureBlack = true,
            accent = AccentChoice.WALLPAPER,
            preset = "MOSS",
            haptics = false,
            reduceMotion = true,
        ).toThemeOptions()
        assertEquals(ThemeMode.DARK, options.mode)
        assertEquals(true, options.oled)
        assertEquals(AccentSource.WALLPAPER, options.accentSource)
        assertEquals(ThemePreset.MOSS, options.preset)
        assertEquals(false, options.hapticsEnabled)
        assertEquals(true, options.reduceMotion)
    }

    @Test
    fun anUnknownPresetFallsBackToMono() {
        assertEquals(ThemePreset.MONO, Appearance(preset = "AURORA").toThemeOptions().preset)
    }
}
