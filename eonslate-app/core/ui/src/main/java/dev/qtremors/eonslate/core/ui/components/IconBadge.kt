package dev.qtremors.eonslate.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape
import dev.qtremors.eonslate.core.ui.theme.pigmentMark
import dev.qtremors.eonslate.core.ui.theme.pigmentTones
import kotlin.math.abs

/** A tile color and the glyph color that reads on it, in every theme. */
@Immutable
data class BadgeColors(val container: Color, val content: Color)

/**
 * Eonslate's pigments: seventeen mineral and natural tones a tracker can wear. Each becomes a
 * tile and a glyph color through fixed HCT tones, so every pigment has the same lightness and
 * contrast whatever its hue, in light, dark and pure black themes alike.
 *
 * The order is stored (a tracker keeps its pigment by index), so new pigments go at the end.
 */
object BadgePalette {
    private val pigments = listOf(
        0xFF34557A, // Slate
        0xFFE8873A, // Ember
        0xFF5B7F3A, // Moss
        0xFF1F7A8C, // Tide
        0xFFA0583A, // Clay
        0xFFD9A62E, // Saffron
        0xFF7A4B7E, // Plum
        0xFF8A9A5B, // Lichen
        0xFFB5452F, // Rust
        0xFF2E8B7A, // Lagoon
        0xFF8E7CB8, // Heather
        0xFFC2A27A, // Sand
        0xFF2F5D4A, // Pine
        0xFFD9665B, // Coral
        0xFF4F5FB0, // Iris
        0xFFB8862F, // Ochre
        0xFF7D7A73  // Stone
    ).map { Color(it) }

    /** The number of pigments, which is the number of colors a tracker can pick from. */
    val size: Int get() = pigments.size

    /** The raw pigment at [index], for swatches. Out-of-range indices wrap instead of failing. */
    fun pigment(index: Int): Color = pigments[Math.floorMod(index, pigments.size)]

    /** A stable pigment for a key (usually a row's id), for rows the user never colored. */
    fun forKey(key: String, dark: Boolean = false): BadgeColors =
        forColor(pigments[abs(key.hashCode()) % pigments.size], dark)

    /** The pigment the user picked by [index]. */
    fun forIndex(index: Int, dark: Boolean = false): BadgeColors = forColor(pigment(index), dark)

    /** The pigment at [index] as a chart mark color, for lines and bars on cards. */
    fun mark(index: Int, dark: Boolean = false): Color = pigmentMark(pigment(index), dark)

    /** Tile and glyph colors for any color, such as a status color. */
    fun forColor(color: Color, dark: Boolean = false): BadgeColors {
        val (container, content) = pigmentTones(color, dark)
        return BadgeColors(container, content)
    }
}

/** The pigmented slate tile with an icon that leads tracker and navigational rows. */
@Composable
fun IconBadge(
    icon: ImageVector,
    key: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    colors: BadgeColors? = null,
    /** A picked palette color; when set it wins over the color derived from [key]. */
    colorIndex: Int? = null
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE
    val resolved = colors ?: remember(key, colorIndex, dark) {
        if (colorIndex != null) BadgePalette.forIndex(colorIndex, dark) else BadgePalette.forKey(key, dark)
    }
    Box(
        modifier = modifier
            .size(size)
            .background(resolved.container, SlateTileShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = resolved.content,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** The same pigmented tile with a letter, for things named by people, such as categories and payments. */
@Composable
fun MonogramBadge(text: String, key: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE
    val resolved = remember(key, dark) { BadgePalette.forKey(key.lowercase(), dark) }
    Box(modifier.size(size).background(resolved.container, SlateTileShape), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(
            text = text.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = dev.qtremors.eonslate.core.ui.theme.EonslateDisplay),
            color = resolved.content,
        )
    }
}

private const val DARK_SURFACE = 0.5f
