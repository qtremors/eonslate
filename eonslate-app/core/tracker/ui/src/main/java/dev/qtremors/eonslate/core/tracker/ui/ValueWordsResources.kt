package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** [ValueWords] from this module's string resources, for screens. */
@Composable
fun rememberValueWords(): ValueWords {
    val context = LocalContext.current
    return remember(context) {
        val res = context.resources
        ValueWords(
            yes = res.getString(R.string.value_yes),
            no = res.getString(R.string.value_no),
            hoursMinutes = { h, m -> res.getString(R.string.value_hours_minutes, h, m) },
            minutesSeconds = { m, s -> res.getString(R.string.value_minutes_seconds, m, s) },
            rating = { v, max -> res.getString(R.string.value_rating, v, max) },
            removedOption = res.getString(R.string.value_removed_option),
        )
    }
}
