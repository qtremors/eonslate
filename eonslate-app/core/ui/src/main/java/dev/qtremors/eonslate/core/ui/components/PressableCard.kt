package dev.qtremors.eonslate.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import dev.qtremors.eonslate.core.ui.theme.LocalReduceMotion
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape

/**
 * A tonal card that answers a press with a small spring, the Expressive response to touch. With
 * Reduce Motion on it stays still. Without [onClick] it is a plain tonal card.
 */
@Composable
fun PressableCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = SlateCardShape,
    color: Color = MaterialTheme.colorScheme.surfaceBright,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Surface(color = color, shape = shape, modifier = modifier, content = content)
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val still = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !still) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPress",
    )
    val haptics = rememberAppHaptics()
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        color = color,
        shape = shape,
        interactionSource = interaction,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        content = content,
    )
}

private const val PRESSED_SCALE = 0.96f
