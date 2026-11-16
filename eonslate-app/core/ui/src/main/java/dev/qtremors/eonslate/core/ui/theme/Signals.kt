package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize

/**
 * Colors that mean something, beside the accent: [positive] for gains and things that went well
 * (income, a goal met), [warning] for a heads-up that is not an error. Negative uses the scheme's
 * error roles and streaks use ember (tertiary), so neither is repeated here.
 *
 * Each grows from its own seed into a tonal scheme, so the four roles keep Material's contrast
 * pairs in light and dark. Under a colored accent the seeds lean toward it, so they sit in the
 * same palette; under Mono they keep their own hue, since there is nothing to lean toward.
 */
@Immutable
data class Signals(
    val positive: Color,
    val onPositive: Color,
    val positiveContainer: Color,
    val onPositiveContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val PositiveSeed = Color(0xFF2E8B57)
private val WarningSeed = Color(0xFFC9A000)

/** The signals for a scheme whose primary is [accent]. A grey accent leaves the seeds as they are. */
fun signalsFor(accent: Color, isDark: Boolean): Signals {
    val colored = Hct.fromInt(accent.toArgb()).chroma >= NEUTRAL_CHROMA
    fun scheme(seed: Color) = seedColorScheme(if (colored) seed.harmonize(accent) else seed, isDark)
    val positive = scheme(PositiveSeed)
    val warning = scheme(WarningSeed)
    return Signals(
        positive = positive.primary,
        onPositive = positive.onPrimary,
        positiveContainer = positive.primaryContainer,
        onPositiveContainer = positive.onPrimaryContainer,
        warning = warning.primary,
        onWarning = warning.onPrimary,
        warningContainer = warning.primaryContainer,
        onWarningContainer = warning.onPrimaryContainer,
    )
}

/** The current theme's [Signals]; provided by EonslateTheme. */
val LocalSignals = staticCompositionLocalOf { signalsFor(Color.Black, isDark = false) }
