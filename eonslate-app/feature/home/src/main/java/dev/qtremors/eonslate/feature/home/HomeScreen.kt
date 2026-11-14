package dev.qtremors.eonslate.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.EmberActionButton
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics

@Composable
fun HomeScreen(
    onNewTracker: () -> Unit,
    onOpenTracker: (String) -> Unit,
    onLog: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    HomeContent(
        trackers = trackers,
        onNewTracker = onNewTracker,
        onOpenTracker = onOpenTracker,
        onLog = { tracker -> if (HomeViewModel.isQuickLog(tracker)) viewModel.quickLog(tracker) else onLog(tracker.id) },
        modifier = modifier,
    )
}

/**
 * Home: every active tracker as a slate in a grid that adapts to the screen width. Null while
 * loading, then the empty state or the slates.
 */
@Composable
fun HomeContent(
    trackers: List<TrackerSummary>?,
    onNewTracker: () -> Unit,
    onOpenTracker: (String) -> Unit,
    onLog: (Tracker) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val haptics = rememberAppHaptics()
    EonslateScaffold(
        title = stringResource(R.string.home_title),
        modifier = modifier,
        floatingActionButton = {
            if (!trackers.isNullOrEmpty()) {
                EmberActionButton(
                    text = stringResource(R.string.home_new_tracker),
                    icon = Icons.Rounded.Add,
                    onClick = onNewTracker,
                )
            }
        },
    ) { padding ->
        when {
            trackers == null -> LoadingState(Modifier.fillMaxSize().padding(padding))
            trackers.isEmpty() -> EmptyState(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = onNewTracker,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(trackers, key = { it.tracker.id }) { summary ->
                    TrackerSlate(
                        name = summary.tracker.name,
                        iconName = summary.tracker.iconName,
                        colorIndex = summary.tracker.colorIndex,
                        key = summary.tracker.id,
                        supporting = summaryLine(summary, nowMillis),
                        onClick = { onOpenTracker(summary.tracker.id) },
                        modifier = Modifier.animateItem(),
                        action = {
                            val quick = HomeViewModel.isQuickLog(summary.tracker)
                            FilledIconButton(
                                shapes = IconButtonDefaults.shapes(),
                                onClick = {
                                    if (quick) haptics.confirm() else haptics.tap()
                                    onLog(summary.tracker)
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                            ) {
                                Icon(
                                    imageVector = if (quick) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.home_log, summary.tracker.name),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun summaryLine(summary: TrackerSummary, nowMillis: Long): String {
    val last = summary.lastEntryAt ?: return stringResource(R.string.home_no_entries)
    val count = pluralStringResource(R.plurals.home_entry_count, summary.entryCount, summary.entryCount)
    // Under a minute reads as "just now" rather than "0 minutes ago".
    val relative = if (nowMillis - last in 0 until DateUtils.MINUTE_IN_MILLIS) stringResource(R.string.home_just_now)
    else DateUtils.getRelativeTimeSpanString(last, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
    return stringResource(R.string.home_entry_summary, count, relative)
}
