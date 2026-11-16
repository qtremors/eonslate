package dev.qtremors.eonslate.core.settings

/** Light, dark, or whatever the system uses. */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** Where accent colors come from: one of Eonslate's presets, or the wallpaper (Android 12 and up). */
enum class AccentChoice { PRESET, WALLPAPER }

/**
 * How the app looks and feels. Stored by name, so reordering enums never changes a saved choice;
 * a name this version does not know falls back to the default.
 *
 * @property preset the name of a theme preset in `:core:ui`, such as "MONO" (the default) or "SLATE".
 */
data class Appearance(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val pureBlack: Boolean = true,
    val accent: AccentChoice = AccentChoice.PRESET,
    val preset: String = DEFAULT_PRESET,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
) {
    companion object {
        const val DEFAULT_PRESET = "MONO"
    }
}

/** Reads a stored enum name, falling back when it is missing or from a newer version. */
internal inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default
