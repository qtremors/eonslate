package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringArrayResource
import dev.qtremors.eonslate.core.ui.components.BadgeIconPicker
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.BadgePigmentPicker
import dev.qtremors.eonslate.core.ui.components.TrackerIcons

@Composable
internal fun IconPicker(selected: String, colorIndex: Int, onSelect: (String) -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    BadgeIconPicker(
        selected, TrackerIcons.all, stringArrayResource(R.array.editor_icon_names),
        BadgePalette.forIndex(colorIndex, dark), onSelect,
    )
}

@Composable
internal fun PigmentPicker(selected: Int, onSelect: (Int) -> Unit) {
    BadgePigmentPicker(selected, stringArrayResource(R.array.editor_pigment_names), onSelect)
}
