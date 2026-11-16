package dev.qtremors.eonslate.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics

/** One chip per tag; picking one shows only its trackers, picking it again shows all. */
@Composable
internal fun TagChips(tags: List<String>, selected: String?, onPick: (String) -> Unit) {
    val haptics = rememberAppHaptics()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            val on = tag.equals(selected, ignoreCase = true)
            FilterChip(
                selected = on,
                onClick = {
                    haptics.toggle(!on)
                    onPick(tag)
                },
                label = { Text(tag) },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}
