package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Eonslate's default look: monochrome. In the dark it is a near-black page with a white accent
 * (true black with Pure black on, the default); in the light it is the mirror, a white page with
 * a black accent. Every other color in the app
 * is contextual and never decoration: tracker pigments on slates and charts, red for errors and
 * deletes, and ember (tertiary) for streaks and records.
 *
 * Built by hand rather than from a seed: a seeded scheme always carries some hue, and these
 * greys are meant to be exactly neutral. Surfaces step up in small grey increments so rows still
 * read as separate from the page on black.
 */
fun monoColorScheme(isDark: Boolean): ColorScheme {
    val ember = seedColorScheme(EmberSeed, isDark)
    return if (isDark) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFFE6E6E6),
            onPrimaryContainer = Color.Black,
            inversePrimary = Color.Black,
            secondary = Color(0xFFC6C6C6),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF2E2E2E),
            onSecondaryContainer = Color.White,
            tertiary = ember.primary,
            onTertiary = ember.onPrimary,
            tertiaryContainer = ember.primaryContainer,
            onTertiaryContainer = ember.onPrimaryContainer,
            background = Color(0xFF101010),
            onBackground = Color.White,
            surface = Color(0xFF101010),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF262626),
            onSurfaceVariant = Color(0xFFABABAB),
            surfaceTint = Color.White,
            inverseSurface = Color(0xFFEDEDED),
            inverseOnSurface = Color.Black,
            outline = Color(0xFF6E6E6E),
            outlineVariant = Color(0xFF333333),
            scrim = Color.Black,
            surfaceBright = Color(0xFF1E1E1E),
            surfaceDim = Color(0xFF0C0C0C),
            surfaceContainerLowest = Color(0xFF0B0B0B),
            surfaceContainerLow = Color(0xFF141414),
            surfaceContainer = Color(0xFF181818),
            surfaceContainerHigh = Color(0xFF222222),
            surfaceContainerHighest = Color(0xFF2C2C2C),
        )
    } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1F1F1F),
            onPrimaryContainer = Color.White,
            inversePrimary = Color.White,
            secondary = Color(0xFF4A4A4A),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE4E4E4),
            onSecondaryContainer = Color.Black,
            tertiary = ember.primary,
            onTertiary = ember.onPrimary,
            tertiaryContainer = ember.primaryContainer,
            onTertiaryContainer = ember.onPrimaryContainer,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFEBEBEB),
            onSurfaceVariant = Color(0xFF555555),
            surfaceTint = Color.Black,
            inverseSurface = Color(0xFF1C1C1C),
            inverseOnSurface = Color.White,
            outline = Color(0xFF8A8A8A),
            outlineVariant = Color(0xFFD6D6D6),
            scrim = Color.Black,
            surfaceBright = Color.White,
            surfaceDim = Color(0xFFE0E0E0),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF7F7F7),
            surfaceContainer = Color(0xFFF2F2F2),
            surfaceContainerHigh = Color(0xFFEAEAEA),
            surfaceContainerHighest = Color(0xFFE2E2E2),
        )
    }
}

/** The scheme for a preset: Mono is hand-built, every other preset grows from its seed. */
fun presetColorScheme(preset: ThemePreset, isDark: Boolean): ColorScheme =
    if (preset == ThemePreset.MONO) monoColorScheme(isDark) else eonslateColorScheme(preset.seed, isDark)
