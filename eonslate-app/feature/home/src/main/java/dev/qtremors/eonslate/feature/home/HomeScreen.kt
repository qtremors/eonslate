package dev.qtremors.eonslate.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import dev.qtremors.eonslate.core.tracker.domain.insights.TodayProgress
import dev.qtremors.eonslate.core.tracker.domain.model.Tags
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.time.UuidIdGenerator
import dev.qtremors.eonslate.core.tracker.ui.Templates
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.MainActionButton
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FilterTop
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics

@Composable
fun HomeScreen(
    onNewTracker: () -> Unit,
    onOpenTracker: (String) -> Unit,
    onLog: (String) -> Unit,
    onArchived: () -> Unit,
    onEditTracker: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val selectedTag by viewModel.homeTag.collectAsStateWithLifecycle(initialValue = null)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    HomeContent(
        trackers = trackers,
        onNewTracker = onNewTracker,
        onOpenTracker = onOpenTracker,
        onLog = { tracker -> if (HomeViewModel.isQuickLog(tracker)) viewModel.quickLog(tracker) else onLog(tracker.id) },
        onArchived = onArchived,
        onSettings = onSettings,
        onStarter = { id ->
            val spec = Templates.byId(id) ?: return@HomeContent
            viewModel.createFrom { currency ->
                Templates.instantiate(spec = spec, text = context::getString, ids = UuidIdGenerator, currency = currency, now = 0)
            }
        },
        streaks = streaks,
        today = today,
        selectedTag = selectedTag,
        onTagChange = viewModel::setHomeTag,
        snackbar = snackbar,
        actions = SlateActions(
            onMove = viewModel::move,
            onEdit = { onEditTracker(it.id) },
            onDuplicate = { tracker -> viewModel.duplicate(tracker, context.getString(R.string.home_copy_name, tracker.name)) },
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
    onSettings: (() -> Unit)? = null,
    streaks: Map<String, Int> = emptyMap(),
    today: TodayProgress? = null,
    snackbar: SnackbarHostState? = null,
    actions: SlateActions? = null,
    /** Creates a tracker from a template in one tap; offered while Home is empty. */
    onStarter: ((String) -> Unit)? = null,
    nowMillis: Long = System.currentTimeMillis(),
    selectedTag: String? = null,
    onTagChange: (String?) -> Unit = {},
) {
    val haptics = rememberAppHaptics()
    var query by rememberSaveable { mutableStateOf("") }
    val tags = remember(trackers) { Tags.all(trackers.orEmpty().map { it.tracker }) }
    val gridState = rememberLazyGridState()
    var hadTags by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val hasTags = tags.isNotEmpty()
    LaunchedEffect(trackers != null, hasTags) {
        if (trackers != null) {
            // The grid keeps its first visible key when the chip row is inserted above it.
            if (hadTags == false && hasTags) gridState.requestScrollToItem(0)
            hadTags = hasTags
        }
    }
    val tag = tags.firstOrNull { it.equals(selectedTag, ignoreCase = true) }
    val shown = remember(trackers, query, tag) {
        trackers?.filter { TrackerSearch.matches(it.tracker, query) && TrackerSearch.hasTag(it.tracker, tag) }
    }
    EonslateScaffold(
        title = stringResource(R.string.home_title),
        modifier = modifier,
        filter = if (trackers.isNullOrEmpty()) null else FilterTop(query, { query = it }, stringResource(R.string.home_search)),
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        actions = {
            if (onArchived != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onArchived) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = stringResource(R.string.home_archived))
                }
            }
            if (onSettings != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.home_settings))
                }
            }
        },
        floatingActionButton = {
            if (!trackers.isNullOrEmpty()) {
                MainActionButton(
                    text = stringResource(R.string.home_new_tracker),
                    icon = Icons.Rounded.Add,
                    onClick = onNewTracker,
                )
            }
        },
    ) { padding ->
        when {
            trackers == null -> LoadingState(Modifier.fillMaxSize().padding(padding))
            trackers.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.home_empty_title),
                    message = stringResource(R.string.home_empty_message),
                    actionLabel = stringResource(R.string.home_empty_action),
                    onAction = onNewTracker,
                )
                if (onStarter != null) Starters(onStarter)
            }
            shown.isNullOrEmpty() && tags.isEmpty() -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(R.string.home_no_matches),
                message = query,
                modifier = Modifier.padding(padding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tags.isNotEmpty()) {
                    item(key = "tags", span = { GridItemSpan(maxLineSpan) }) {
                        TagChips(tags, tag, onPick = { picked -> onTagChange(picked.takeUnless { it == tag }) })
                    }
                }
                if (today != null && today.total > 0 && query.isBlank() && tag == null) {
                    item(key = "today", span = { GridItemSpan(maxLineSpan) }) { TodayLine(today) }
                }
                if (shown.isNullOrEmpty()) {
                    item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(icon = Icons.Rounded.SearchOff, title = stringResource(R.string.home_no_matches), message = query)
                    }
                }
                items(shown.orEmpty(), key = { s -> s.tracker.id }) { summary ->
                    // The position in the full list, so moving still works while searching.
                    val index = trackers.indexOf(summary)
                    var menu by rememberSaveable { mutableStateOf(false) }
                    Box(Modifier.animateItem()) {
                        TrackerSlate(
                            name = summary.tracker.name,
                            iconName = summary.tracker.iconName,
                            colorIndex = summary.tracker.colorIndex,
                            key = summary.tracker.id,
                            supporting = summaryLine(summary, nowMillis),
                            streak = streaks[summary.tracker.id] ?: 0,
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
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
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
                                    listOf(
                                    MenuAction(stringResource(R.string.home_edit), Icons.Rounded.Edit) { actions.onEdit(summary.tracker) },
                                    MenuAction(stringResource(R.string.home_duplicate), Icons.Rounded.ContentCopy) { actions.onDuplicate(summary.tracker) },
                                ),
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

/** "2 of 3 logged today" with an ember meter, above the slates. */
@Composable
private fun TodayLine(progress: TodayProgress) {
    val text = if (progress.logged == progress.total) stringResource(R.string.home_today_all, progress.total)
    else stringResource(R.string.home_today, progress.logged, progress.total)
    // One announcement for the line and its meter, not two.
    Column(
        Modifier.padding(horizontal = 4.dp, vertical = 4.dp).clearAndSetSemantics { contentDescription = text },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Meter(
            fraction = progress.logged.toFloat() / progress.total,
            color = MaterialTheme.colorScheme.primary,
            contentDescription = text,
        )
    }
}

/** One-tap starters under the empty state, for someone who skipped the welcome. */
@Composable
private fun Starters(onStarter: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.home_starters),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HOME_STARTERS.mapNotNull(Templates::byId).forEach { spec ->
                AssistChip(
                    onClick = { onStarter(spec.id) },
                    label = { Text(stringResource(spec.name)) },
                    leadingIcon = { Icon(TrackerIcons.resolve(spec.iconName), contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

/** The starters an empty Home offers: the quickest to begin with. */
internal val HOME_STARTERS = listOf("habit", "mood", "water")

/** What a long-press on a slate offers. */
class SlateActions(
    val onMove: (Tracker, Int) -> Unit,
    val onEdit: (Tracker) -> Unit,
    val onArchive: (Tracker) -> Unit,
    val onDuplicate: (Tracker) -> Unit = {},
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
