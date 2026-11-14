package dev.qtremors.eonslate.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.EmberActionButton
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics

@Composable
fun HomeScreen(
    onNewTracker: () -> Unit,
    onOpenTracker: (String) -> Unit,
    onLog: (String) -> Unit,
    onArchived: () -> Unit,
    onEditTracker: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    HomeContent(
        trackers = trackers,
        onNewTracker = onNewTracker,
        onOpenTracker = onOpenTracker,
        onLog = { tracker -> if (HomeViewModel.isQuickLog(tracker)) viewModel.quickLog(tracker) else onLog(tracker.id) },
        onArchived = onArchived,
        snackbar = snackbar,
        actions = SlateActions(
            onMove = viewModel::move,
            onEdit = { onEditTracker(it.id) },
            onArchive = { tracker ->
                viewModel.setArchived(tracker, true)
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.home_archived_message, tracker.name),
                        actionLabel = context.getString(R.string.home_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.setArchived(tracker, false)
                }
            },
        ),
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
    onArchived: (() -> Unit)? = null,
    snackbar: SnackbarHostState? = null,
    actions: SlateActions? = null,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val haptics = rememberAppHaptics()
    EonslateScaffold(
        title = stringResource(R.string.home_title),
        modifier = modifier,
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        actions = {
            if (onArchived != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onArchived) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = stringResource(R.string.home_archived))
                }
            }
        },
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
                itemsIndexed(trackers, key = { _, s -> s.tracker.id }) { index, summary ->
                    var menu by rememberSaveable { mutableStateOf(false) }
                    Box(Modifier.animateItem()) {
                        TrackerSlate(
                            name = summary.tracker.name,
                            iconName = summary.tracker.iconName,
                            colorIndex = summary.tracker.colorIndex,
                            key = summary.tracker.id,
                            supporting = summaryLine(summary, nowMillis),
                            onClick = { onOpenTracker(summary.tracker.id) },
                            onLongClick = if (actions != null) ({ menu = true }) else null,
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
                        if (actions != null) {
                            EonslateMenu(
                                expanded = menu,
                                onDismissRequest = { menu = false },
                                groups = listOf(
                                    listOf(
                                        MenuAction(stringResource(R.string.home_move_earlier), Icons.AutoMirrored.Rounded.ArrowBack, enabled = index > 0) {
                                            actions.onMove(summary.tracker, -1)
                                        },
                                        MenuAction(stringResource(R.string.home_move_later), Icons.AutoMirrored.Rounded.ArrowForward, enabled = index < trackers.lastIndex) {
                                            actions.onMove(summary.tracker, 1)
                                        },
                                    ),
                                    listOf(MenuAction(stringResource(R.string.home_edit), Icons.Rounded.Edit) { actions.onEdit(summary.tracker) }),
                                    listOf(MenuAction(stringResource(R.string.home_archive), Icons.Rounded.Archive) { actions.onArchive(summary.tracker) }),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What a long-press on a slate offers. */
class SlateActions(
    val onMove: (Tracker, Int) -> Unit,
    val onEdit: (Tracker) -> Unit,
    val onArchive: (Tracker) -> Unit,
)

@Composable
private fun summaryLine(summary: TrackerSummary, nowMillis: Long): String {
    val last = summary.lastEntryAt ?: return stringResource(R.string.home_no_entries)
    val count = pluralStringResource(R.plurals.home_entry_count, summary.entryCount, summary.entryCount)
    // Under a minute reads as "just now" rather than "0 minutes ago".
    val relative = if (nowMillis - last in 0 until DateUtils.MINUTE_IN_MILLIS) stringResource(R.string.home_just_now)
    else DateUtils.getRelativeTimeSpanString(last, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
    return stringResource(R.string.home_entry_summary, count, relative)
}
