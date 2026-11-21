package dev.qtremors.eonslate.core.tracker.ui

import dev.qtremors.eonslate.core.ui.theme.LocalReduceMotion
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ripple
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape

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
    /** The current streak in days; from two days on it shows as an ember pill. */
    streak: Int = 0,
    /** Progress toward a goal, 0 to 1, shown as a meter under the line; null without a goal. */
    goal: Float? = null,
    /** What the meter says, such as "5 of 8 glasses". */
    goalText: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val badge = remember(colorIndex, dark) { BadgePalette.forIndex(colorIndex, dark) }
    // A whisper of the pigment over the row color, so a grid of trackers reads by color at a glance.
    val container: Color = lerp(colors.surfaceBright, badge.container, if (dark) 0.28f else 0.42f)

    // The same press spring as every tappable card; still when Reduce Motion is on.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val still = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !still) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "slatePress",
    )
    val clickable = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
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

    Surface(color = container, shape = SlateCardShape, modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }) {
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
                if (streak >= 2) StreakPill(streak)
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (goal != null) {
                    Meter(goal, badge.content, contentDescription = goalText.orEmpty(), modifier = Modifier.padding(top = 10.dp))
                    if (goalText != null) {
                        Text(goalText, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** A small ember pill with a flame and the streak's days, such as "3". */
@Composable
private fun StreakPill(days: Int) {
    val colors = MaterialTheme.colorScheme
    val description = pluralStringResource(R.plurals.slate_streak, days, days)
    Row(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(SlateTileShape)
            .background(colors.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.onTertiaryContainer, modifier = Modifier.size(14.dp))
        Text(days.toString(), style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer, modifier = Modifier.padding(start = 2.dp))
    }
}
