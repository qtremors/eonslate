package dev.qtremors.eonslate.feature.home

import androidx.compose.ui.semantics.heading
import dev.qtremors.eonslate.core.tracker.ui.PartRows
import dev.qtremors.eonslate.core.tracker.domain.insights.PartStatus
import dev.qtremors.eonslate.core.ui.components.LocalAccountActions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Surface
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.components.ProgressRing
import dev.qtremors.eonslate.core.tracker.domain.insights.Consistency
import dev.qtremors.eonslate.core.tracker.ui.rhythmText
import dev.qtremors.eonslate.core.tracker.ui.intervalText
import dev.qtremors.eonslate.core.tracker.ui.goalText
import dev.qtremors.eonslate.core.tracker.domain.insights.GoalProgress
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
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
    /** Logs one part of a tracker, with that part picked. */
    onLogPart: (String, String) -> Unit = { id, _ -> onLog(id) },
    onArchived: (() -> Unit)? = null,
    onEditTracker: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onFinance: (() -> Unit)? = null,
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val dueToday by viewModel.dueToday.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val consistency by viewModel.consistency.collectAsStateWithLifecycle()
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
        onFinance = onFinance,
        onStarter = { id ->
            val spec = Templates.byId(id) ?: return@HomeContent
            viewModel.createFrom { currency ->
                Templates.instantiate(spec = spec, text = context::getString, ids = UuidIdGenerator, currency = currency, now = 0)
            }
        },
        streaks = streaks,
        today = today,
        dueToday = dueToday,
        goals = goals,
        upcoming = upcoming,
        parts = parts,
        onLogPart = onLogPart,
        consistency = consistency,
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
    /** The trackers due today; null until known. */
    dueToday: Set<String>? = null,
    /** Each tracker's progress toward its first goal. */
    goals: Map<String, GoalProgress> = emptyMap(),
    /** What comes next for trackers with intervals or a learned rhythm. */
    upcoming: Map<String, HomeViewModel.Upcoming> = emptyMap(),
    /** Each tracker with parts, and where every part stands. */
    parts: Map<String, List<PartStatus>> = emptyMap(),
    onLogPart: (String, String) -> Unit = { _, _ -> },
    /** Each scheduled tracker's consistency, for the overview. */
    consistency: Map<String, Consistency> = emptyMap(),
    snackbar: SnackbarHostState? = null,
    actions: SlateActions? = null,
    /** Creates a tracker from a template in one tap; offered while Home is empty. */
    onStarter: ((String) -> Unit)? = null,
    nowMillis: Long = System.currentTimeMillis(),
    selectedTag: String? = null,
    onTagChange: (String?) -> Unit = {},
    onFinance: (() -> Unit)? = null,
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
    // Sections only once some tracker is not due every day: what is due today first, then what
    // is not due today, then what is logged as needed.
    val scheduled = remember(trackers) { trackers.orEmpty().any { !it.tracker.schedule.isDaily } }
    val sections = remember(shown, dueToday, scheduled) {
        val list = shown.orEmpty()
        if (!scheduled || dueToday == null) listOf(null to list)
        else {
            // An As needed tracker whose interval has come round is due like any other.
            val due = list.filter { it.tracker.id in dueToday }
            val asNeeded = list.filter { it.tracker.schedule.isAsNeeded && it.tracker.id !in dueToday }
            val later = list - due.toSet() - asNeeded.toSet()
            listOf(R.string.home_section_due to due, R.string.home_section_later to later, R.string.home_section_as_needed to asNeeded)
                .filter { it.second.isNotEmpty() }
        }
    }
    EonslateScaffold(
        title = stringResource(R.string.home_title),
        modifier = modifier,
        filter = if (trackers.isNullOrEmpty()) null else FilterTop(query, { query = it }, stringResource(R.string.home_search)),
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        actions = {
            if (onFinance != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onFinance) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = stringResource(R.string.home_finance))
                }
            }
            if (onArchived != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onArchived) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = stringResource(R.string.home_archived))
                }
            }
            val account = LocalAccountActions.current
            if (account != null) account() else if (onSettings != null) {
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
                // How today is going heads the trackers due today; with no sections, it heads them all.
                val todayShown = today?.takeIf { it.total > 0 && query.isBlank() && tag == null }
                val dueSection = sections.any { it.first == R.string.home_section_due }
                if (todayShown != null && !dueSection) {
                    item(key = "today", span = { GridItemSpan(maxLineSpan) }) { TodayLine(todayShown) }
                }
                if (shown.isNullOrEmpty()) {
                    item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(icon = Icons.Rounded.SearchOff, title = stringResource(R.string.home_no_matches), message = query)
                    }
                }
                sections.forEach { (title, list) ->
                if (title == R.string.home_section_due && todayShown != null) item(key = "today", span = { GridItemSpan(maxLineSpan) }) {
                    DueHeader(todayShown)
                } else if (title != null) item(key = "section-$title", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                // A tracker with parts takes the full width, so its rows have room.
                items(list, key = { s -> s.tracker.id }, span = { s -> GridItemSpan(if (parts[s.tracker.id].isNullOrEmpty()) 1 else maxLineSpan) }) { summary ->
                    // The position in the full list, so moving still works while searching.
                    val index = trackers.indexOf(summary)
                    var menu by rememberSaveable { mutableStateOf(false) }
                    Box(Modifier.animateItem()) {
                        TrackerSlate(
                            name = summary.tracker.name,
                            iconName = summary.tracker.iconName,
                            colorIndex = summary.tracker.colorIndex,
                            key = summary.tracker.id,
                            // What comes next, when an interval or a rhythm says; otherwise the latest entry.
                            // With parts, each part says its own, so the line keeps to the latest entry.
                            supporting = upcoming[summary.tracker.id]?.takeIf { parts[summary.tracker.id].isNullOrEmpty() }?.let { next ->
                                next.interval?.let { intervalText(it, summary.tracker) } ?: next.rhythm?.let { rhythmText(it, summary.tracker) }
                            } ?: summaryLine(summary, nowMillis),
                            streak = streaks[summary.tracker.id] ?: 0,
                            goal = goals[summary.tracker.id]?.fraction,
                            goalText = goals[summary.tracker.id]?.let { goalText(it) },
                            onClick = { onOpenTracker(summary.tracker.id) },
                            onLongClick = if (actions != null) ({ menu = true }) else null,
                            parts = parts[summary.tracker.id]?.takeIf { it.isNotEmpty() }?.let { list ->
                                { PartRows(list, summary.tracker, onLog = { option -> onLogPart(summary.tracker.id, option) }) }
                            },
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
                // Consistency across the trackers shown (a tag narrows it), once two or more have any:
                // a look back, so it sits after the trackers rather than above them.
                val measured = shown.orEmpty().mapNotNull { s -> consistency[s.tracker.id]?.let { s.tracker to it } }
                if (measured.size >= 2 && query.isBlank()) {
                    item(key = "overview", span = { GridItemSpan(maxLineSpan) }) { ConsistencyOverview(measured) }
                }
            }
        }
    }
}

/** "Due today" with how many are logged and a meter, as the first section's header. */
@Composable
private fun DueHeader(progress: TodayProgress) {
    val text = if (progress.logged == progress.total) stringResource(R.string.home_today_all, progress.total)
    else stringResource(R.string.home_today, progress.logged, progress.total)
    val title = stringResource(R.string.home_section_due)
    Column(
        // One heading for screen readers: the section and how today is going.
        Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp).clearAndSetSemantics {
            contentDescription = "$title, $text"
            heading()
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Text(
                if (progress.logged == progress.total) stringResource(R.string.home_due_all) else stringResource(R.string.home_due_count, progress.logged, progress.total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Meter(fraction = progress.logged.toFloat() / progress.total, color = MaterialTheme.colorScheme.primary, contentDescription = text)
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

/**
 * When it was last logged, short enough for one line on a slate: "Logged just now", "Logged 2
 * hours ago", "Logged yesterday", "Logged 9 days ago", then the day ("Logged 14 Oct"). The count
 * of entries lives on the tracker's page.
 */
@Composable
private fun summaryLine(summary: TrackerSummary, nowMillis: Long): String {
    val last = summary.lastEntryAt ?: return stringResource(R.string.home_no_entries)
    val zone = java.time.ZoneId.systemDefault()
    val days = java.time.temporal.ChronoUnit.DAYS.between(
        java.time.Instant.ofEpochMilli(last).atZone(zone).toLocalDate(),
        java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate(),
    )
    val locale = java.util.Locale.getDefault()
    val `when` = when {
        // Under a minute reads as "just now" rather than "0 minutes ago".
        nowMillis - last in 0 until DateUtils.MINUTE_IN_MILLIS -> stringResource(R.string.home_just_now)
        // Android capitalises it ("In 4 hours") for the start of a sentence; here it sits mid-line.
        days <= 0L -> DateUtils.getRelativeTimeSpanString(last, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString().replaceFirstChar { it.lowercase(locale) }
        days == 1L -> stringResource(R.string.home_yesterday)
        days < RECENT_DAYS -> pluralStringResource(R.plurals.home_days_ago, days.toInt(), days.toInt())
        else -> {
            val day = java.time.Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
            val pattern = if (day.year == java.time.LocalDate.now(zone).year) "d MMM" else "d MMM yyyy"
            day.format(java.time.format.DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
    return stringResource(R.string.home_last_logged, `when`)
}

/** Up to this many days a slate counts them; past it, it names the day. */
private const val RECENT_DAYS = 30L

/** All shown trackers' due days done, as one ring, beside the three most consistent ones. */
@Composable
private fun ConsistencyOverview(measured: List<Pair<Tracker, Consistency>>) {
    val colors = MaterialTheme.colorScheme
    val done = measured.sumOf { it.second.done }
    val due = measured.sumOf { it.second.due }.coerceAtLeast(1)
    val percent = done * 100 / due
    val top = measured.sortedByDescending { it.second.share }.take(3)
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressRing(
                fraction = done.toFloat() / due,
                color = colors.primary,
                contentDescription = stringResource(R.string.home_overview, percent, measured.size),
                size = 72.dp,
                thickness = 9.dp,
            ) { Text("$percent%", style = MaterialTheme.typography.titleSmall) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    pluralStringResource(R.plurals.home_overview_title, measured.size, measured.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                top.forEach { (tracker, c) ->
                    Text("${tracker.name} · ${c.percent}%", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
