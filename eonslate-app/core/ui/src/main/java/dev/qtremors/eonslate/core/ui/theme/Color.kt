package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot

/**
 * Eonslate's own colors, "Slate and Ember": a deep slate blue carries the app, and a warm ember
 * amber marks what is alive (today's progress, a streak, the main action's accent). These are the
 * default; wallpaper colors are a choice in Settings, never the out-of-the-box look.
 */
val BrandSeed = Color(0xFF34557A)

/** The warm accent every Eonslate scheme uses for its tertiary roles. */
val EmberSeed = Color(0xFFE8873A)

/**
 * Named seed colors, each a mineral or natural material, so every preset stays in the same family
 * as the brand. Each produces a full tonal scheme in light, dark and OLED.
 */
enum class ThemePreset(val seed: Color) {
    SLATE(BrandSeed),
    TIDE(Color(0xFF1F7A8C)),
    MOSS(Color(0xFF5B7F3A)),
    CLAY(Color(0xFFA0583A)),
    DUSK(Color(0xFF5E4B8B)),
    GRAPHITE(Color(0xFF4A4F55))
}

/** Builds a Material 3 scheme from one seed with Material Color Utilities' tonal spot style. */
fun seedColorScheme(seed: Color, isDark: Boolean): ColorScheme =
    SchemeTonalSpot(Hct.fromInt(seed.toArgb()), isDark, 0.0).toColorScheme(isDark)

/**
 * An Eonslate scheme: [seed] for primary, secondary and the tinted neutrals, and always ember for
 * tertiary. Ember's roles are the primary roles of its own tonal scheme, so they keep Material's
 * contrast pairs in light and dark instead of being hand-picked.
 */
fun eonslateColorScheme(seed: Color, isDark: Boolean): ColorScheme {
    val ember = seedColorScheme(EmberSeed, isDark)
    return seedColorScheme(seed, isDark).copy(
        tertiary = ember.primary,
        onTertiary = ember.onPrimary,
        tertiaryContainer = ember.primaryContainer,
        onTertiaryContainer = ember.onPrimaryContainer
    )
}

/**
 * Builds the badge colors for a pigment from HCT tones, so every pigment has the same lightness
 * and contrast whatever its hue: a soft tile with a deep glyph in light themes, a deep tile with
 * a light glyph in dark ones.
 */
internal fun pigmentTones(pigment: Color, isDark: Boolean): Pair<Color, Color> {
    val hct = Hct.fromInt(pigment.toArgb())
    val chroma = hct.chroma.coerceAtMost(48.0)
    fun tone(t: Double, c: Double = chroma) = Color(Hct.from(hct.hue, c, t).toInt())
    return if (isDark) tone(30.0, chroma * 0.8) to tone(88.0, chroma * 0.6)
    else tone(88.0, chroma * 0.55) to tone(32.0)
}

/**
 * Pure black for OLED screens. Pages are drawn on surfaceContainer, so that is black too; rows
 * (surfaceBright) and raised parts step up in small neutral grays so they still read as separate.
 */
fun ColorScheme.toOled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceBright = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF262626),
    surfaceVariant = Color(0xFF1E1E1E)
)

private fun DynamicScheme.toColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest)
    )
}
