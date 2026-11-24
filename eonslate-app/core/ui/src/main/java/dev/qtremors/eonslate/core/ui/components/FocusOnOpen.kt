package dev.qtremors.eonslate.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

/**
 * Focuses the field it is set on once, when [enabled], so a new item's editor opens with the
 * keyboard on its first field. The short wait lets a sheet finish opening first.
 */
@Composable
fun rememberFocusOnOpen(enabled: Boolean): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (enabled) {
            delay(300)
            runCatching { requester.requestFocus() }
        }
    }
    return Modifier.focusRequester(requester)
}
