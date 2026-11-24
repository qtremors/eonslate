package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeTonalSpot

/**
 * The seed of the Slate palette, one of the accents a person can pick instead of the default
 * monochrome look (see [monoColorScheme]).
 */
val BrandSeed = Color(0xFF34557A)

/** Ember: the contextual warm color every scheme uses for its tertiary roles (streaks, records). */
val EmberSeed = Color(0xFFE8873A)

/**
 * The accents a person can pick. Mono, the default, is black and white; the rest are named seed
 * colors, each a mineral or natural material, producing a full tonal scheme in light, dark and
 * OLED. The order is shown in Settings; presets are stored by name, never by position.
 */
enum class ThemePreset(val seed: Color) {
    MONO(Color.White),
    SLATE(BrandSeed),
    TIDE(Color(0xFF1F7A8C)),
    MOSS(Color(0xFF5B7F3A)),
    OCHRE(Color(0xFFB08A2E)),
    CLAY(Color(0xFFA0583A)),
    ROSE(Color(0xFFB04A6A)),
    DUSK(Color(0xFF5E4B8B)),
    GRAPHITE(Color(0xFF4A4F55))
}

/**
 * Builds a Material 3 scheme from one seed with Material Color Utilities' tonal spot style. A
 * near-grey seed (Graphite) uses the neutral style instead, which keeps it grey; tonal spot would
 * lift any seed to the same chroma and turn grey into blue.
 */
fun seedColorScheme(seed: Color, isDark: Boolean): ColorScheme {
    val hct = Hct.fromInt(seed.toArgb())
    return if (hct.chroma < NEUTRAL_CHROMA) SchemeNeutral(hct, isDark, 0.0).toColorScheme(isDark)
    else SchemeTonalSpot(hct, isDark, 0.0).toColorScheme(isDark)
}

/** Seeds below this chroma read as grey and get the neutral scheme. */
internal const val NEUTRAL_CHROMA = 12.0

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
/**
 * The pigment for thin chart marks: its full chroma at a middle tone, deep enough to read on a
 * light card and light enough on a dark one. Badge tones are too soft for a 2 dp line.
 */
internal fun pigmentMark(pigment: Color, isDark: Boolean): Color {
    val hct = Hct.fromInt(pigment.toArgb())
    return Color(Hct.from(hct.hue, maxOf(hct.chroma, 36.0), if (isDark) 75.0 else 45.0).toInt())
}

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
