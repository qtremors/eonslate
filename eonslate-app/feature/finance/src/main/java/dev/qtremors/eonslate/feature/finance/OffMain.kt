package dev.qtremors.eonslate.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs [compute] on a background thread each time [keys] change and returns its latest result,
 * null until the first one is in. The last result stays shown while the next is worked out, so a
 * filter change never flashes an empty list, and a long ledger never holds up a frame.
 */
@Composable
internal fun <T : Any> rememberOffMain(vararg keys: Any?, compute: () -> T?): T? {
    val latest by rememberUpdatedState(compute)
    return produceState<T?>(initialValue = null, *keys) {
        value = withContext(Dispatchers.Default) { latest() }
    }.value
}
