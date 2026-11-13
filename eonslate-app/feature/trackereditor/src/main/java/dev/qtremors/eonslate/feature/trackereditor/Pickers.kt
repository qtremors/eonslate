package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.BadgeColors
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape

/** Every tracker icon as a tile; the chosen one wears the tracker's pigment and grows a little. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconPicker(selected: String, colorIndex: Int, onSelect: (String) -> Unit) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val names = stringArrayResource(R.array.editor_icon_names)
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrackerIcons.all.entries.forEachIndexed { index, (name, icon) ->
                val isSelected = name == selected
                val scale by animateFloatAsState(if (isSelected) 1f else 0.86f, label = "iconScale")
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(SlateTileShape)
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            haptics.tick()
                            onSelect(name)
                        }
                        .semantics { contentDescription = names.getOrElse(index) { name } },
                    contentAlignment = Alignment.Center,
                ) {
                    IconBadge(
                        icon = icon,
                        key = name,
                        size = 52.dp,
                        iconSize = 26.dp,
                        colors = if (isSelected) BadgePalette.forIndex(colorIndex, dark)
                        else BadgeColors(colors.surfaceContainerHigh, colors.onSurfaceVariant),
                        modifier = Modifier.scale(scale),
                    )
                }
            }
        }
    }
}

/** The seventeen pigments as swatches; the chosen one carries a check in its own glyph color. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PigmentPicker(selected: Int, onSelect: (Int) -> Unit) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val names = stringArrayResource(R.array.editor_pigment_names)
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(BadgePalette.size) { index ->
                val badge = BadgePalette.forIndex(index, dark)
                val isSelected = index == selected
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(SlateTileShape)
                        .background(badge.container)
                        .then(
                            if (isSelected) Modifier.border(BorderStroke(3.dp, badge.content), SlateTileShape) else Modifier
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            haptics.tick()
                            onSelect(index)
                        }
                        .semantics { contentDescription = names.getOrElse(index) { "$index" } },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = badge.content)
                }
            }
        }
    }
}
