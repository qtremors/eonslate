package dev.qtremors.eonslate.feature.home

import dev.qtremors.eonslate.core.ui.components.PressableCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape

/**
 * The trackers half of Home: today's progress and one tap to log each tracker, the same quick
 * log the Trackers tab uses. Shows nothing until there is a tracker.
 */
@Composable
fun TodayStrip(
    onLog: (String) -> Unit,
    onAll: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val list = trackers?.takeIf { it.isNotEmpty() } ?: return
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.today_title), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    today?.let {
                        Text(stringResource(R.string.today_logged, it.logged, it.total), style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = colors.onSurface)
                    }
                }
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onAll) { Text(stringResource(R.string.today_all)) }
            }
            today?.takeIf { it.total > 0 }?.let {
                Meter(it.logged / it.total.toFloat(), colors.primary, stringResource(R.string.today_logged, it.logged, it.total), Modifier.padding(horizontal = 20.dp))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                items(list, key = { it.tracker.id }) { summary ->
                    val tracker = summary.tracker
                    PressableCard(
                        modifier = Modifier.width(112.dp),
                        onClick = { if (HomeViewModel.isQuickLog(tracker)) viewModel.quickLog(tracker) else onLog(tracker.id) },
                        shape = SlateTileShape,
                        color = colors.surfaceContainerHigh,
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = TrackerIcons.resolve(tracker.iconName), key = tracker.id, colorIndex = tracker.colorIndex)
                            Text(tracker.name, style = MaterialTheme.typography.labelLarge, color = colors.onSurface, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
