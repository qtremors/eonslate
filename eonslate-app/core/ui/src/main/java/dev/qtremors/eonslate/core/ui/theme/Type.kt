package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.qtremors.eonslate.core.ui.R

/**
 * Roboto Flex is one variable font file. Each weight is an instance of it, so the APK carries one
 * file instead of four.
 */
@OptIn(ExperimentalTextApi::class)
private fun flex(weight: FontWeight) = Font(
    R.font.roboto_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val RobotoFlex = FontFamily(
    flex(FontWeight.Normal),
    flex(FontWeight.Medium),
    flex(FontWeight.SemiBold),
    flex(FontWeight.Bold)
)

/**
 * Eonslate's display voice: the same Roboto Flex file narrowed on its width axis and set heavy,
 * like lettering cut into a slate. Used for page titles, headlines and big numbers only; body
 * text stays at normal width for reading.
 */
@OptIn(ExperimentalTextApi::class)
private fun slate(weight: FontWeight) = Font(
    R.font.roboto_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.width(SLATE_WIDTH)
    )
)

/** The narrowest end of Roboto Flex's 25 to 151 width axis. At heavy weights the font compresses
 * gently, so this reads as condensed lettering rather than squeezed text. */
private const val SLATE_WIDTH = 25f

val EonslateDisplay = FontFamily(
    slate(FontWeight.Medium),
    slate(FontWeight.SemiBold),
    slate(FontWeight.Bold),
    slate(FontWeight.ExtraBold)
)

private fun display(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = EonslateDisplay,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )

val EonslateTypography = Typography(
    displayLarge = display(60, 64, FontWeight.ExtraBold, -1.0),
    displayMedium = display(48, 52, FontWeight.ExtraBold, -0.75),
    displaySmall = display(38, 44, FontWeight.Bold, -0.5),
    headlineLarge = display(34, 40, FontWeight.Bold, -0.5),
    headlineMedium = display(30, 36, FontWeight.Bold, -0.25),
    headlineSmall = display(26, 32, FontWeight.Bold),
    titleLarge = display(24, 28, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold, 0.15),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = style(16, 24, tracking = 0.4),
    bodyMedium = style(14, 20, tracking = 0.2),
    bodySmall = style(12, 16, tracking = 0.4),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = style(11, 16, FontWeight.SemiBold, 0.5)
)

/** A logged value shown large, such as "72.5 kg" or "6 h 30 m". Digits stay aligned. */
val MetricTextStyle: TextStyle = display(30, 34, FontWeight.Bold).copy(
    fontFeatureSettings = "tnum"
)

/** Values in rows, such as amounts and times, with aligned digits. */
val ValueTextStyle: TextStyle = style(14, 20, tracking = 0.1).copy(fontFeatureSettings = "tnum")
