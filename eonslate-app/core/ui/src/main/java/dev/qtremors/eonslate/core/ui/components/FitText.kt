package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * One line that never wraps and never cuts: a long amount shrinks until it fits. For figures such
 * as money, where a line broken after the sign, or a clipped "₹2,17", would read as another number.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 10.sp,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = style.fontSize),
    )
}
