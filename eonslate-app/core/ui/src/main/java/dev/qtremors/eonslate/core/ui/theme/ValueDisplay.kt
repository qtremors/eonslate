package dev.qtremors.eonslate.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

data class ValueDisplay(
    val numberLocale: Locale = Locale.getDefault(),
    val datePattern: String? = null,
)

val LocalValueDisplay = compositionLocalOf { ValueDisplay() }
