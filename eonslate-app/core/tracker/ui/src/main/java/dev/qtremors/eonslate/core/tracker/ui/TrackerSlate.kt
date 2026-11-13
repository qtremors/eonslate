package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape

/**
 * Eonslate's signature tile: one tracker as a slate tinted with its pigment, its icon on a slate
 * tile in the corner, its name in the display face, and a line about what was logged. [action]
 * sits top right for a quick action such as logging.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackerSlate(
    name: String,
    iconName: String,
    colorIndex: Int,
    key: String,
    supporting: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val badge = remember(colorIndex, dark) { BadgePalette.forIndex(colorIndex, dark) }
    // A whisper of the pigment over the row color, so a grid of trackers reads by color at a glance.
    val container: Color = lerp(colors.surfaceBright, badge.container, if (dark) 0.28f else 0.42f)

    val clickable = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            role = Role.Button,
            onClick = {
                haptics.tap()
                onClick?.invoke()
            },
            onLongClick = onLongClick?.let { handler ->
                {
                    haptics.longPress()
                    handler()
                }
            }
        )
    } else Modifier

    Surface(color = container, shape = SlateCardShape, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .clip(SlateCardShape)
                .then(clickable)
                .heightIn(min = 148.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(
                    icon = TrackerIcons.resolve(iconName),
                    key = key,
                    colors = badge,
                    size = 48.dp,
                    iconSize = 26.dp
                )
                Spacer(Modifier.weight(1f))
                if (action != null) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Row(content = action)
                    }
                }
            }
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
