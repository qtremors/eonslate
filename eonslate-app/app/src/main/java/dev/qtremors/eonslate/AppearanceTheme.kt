package dev.qtremors.eonslate

import dev.qtremors.eonslate.core.settings.AccentChoice
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.ThemeChoice
import dev.qtremors.eonslate.core.ui.theme.AccentSource
import dev.qtremors.eonslate.core.ui.theme.ThemeMode
import dev.qtremors.eonslate.core.ui.theme.ThemeOptions
import dev.qtremors.eonslate.core.ui.theme.ThemePreset

/**
 * The stored appearance as the theme's options. The settings store knows nothing of the theme,
 * so this mapping lives where both meet; a preset name this version lacks falls back to Mono.
 */
fun Appearance.toThemeOptions(): ThemeOptions = ThemeOptions(
    mode = when (theme) {
        ThemeChoice.SYSTEM -> ThemeMode.SYSTEM
        ThemeChoice.LIGHT -> ThemeMode.LIGHT
        ThemeChoice.DARK -> ThemeMode.DARK
    },
    oled = pureBlack,
    accentSource = when (accent) {
        AccentChoice.PRESET -> AccentSource.PRESET
        AccentChoice.WALLPAPER -> AccentSource.WALLPAPER
    },
    preset = ThemePreset.entries.firstOrNull { it.name == preset } ?: ThemePreset.MONO,
    hapticsEnabled = haptics,
    reduceMotion = reduceMotion,
)
