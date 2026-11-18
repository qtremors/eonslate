package dev.qtremors.eonslate.feature.trackerdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.tracker.ui.rememberValueWords
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.MainActionButton
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FilterTop
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TrackerDetailScreen(
    onBack: () -> Unit,
    onEditTracker: (String) -> Unit,
    onLog: (String) -> Unit,
    onEditEntry: (trackerId: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
    onReminders: (String) -> Unit = {},
    viewModel: TrackerDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val csv: TrackerCsvViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DetailEvent.Deleted -> {
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.detail_deleted),
                        actionLabel = context.getString(R.string.detail_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.entry)
                }
            }
        }
    }

    val tracker = state.tracker
    TrackerCsvFeedback(csv, snackbar)
    EonslateScaffold(
        title = tracker?.name.orEmpty(),
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        filter = if (tracker != null && state.view == DetailView.LIST) {
            FilterTop(state.query, viewModel::setQuery, stringResource(R.string.detail_search))
        } else null,
        actions = {
            if (tracker != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onReminders(tracker.id) }) {
                    Icon(Icons.Rounded.NotificationsNone, contentDescription = stringResource(R.string.detail_reminders))
                }
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onEditTracker(tracker.id) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.detail_edit_tracker))
                }
                TrackerCsvMenu(tracker.name, csv)
            }
        },
        floatingActionButton = {
            if (tracker != null && !tracker.archived) {
                MainActionButton(text = stringResource(R.string.detail_log), icon = Icons.Rounded.Add, onClick = { onLog(tracker.id) })
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.missing || tracker == null -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(R.string.detail_missing_title),
                message = stringResource(R.string.detail_missing_message),
                actionLabel = stringResource(R.string.detail_missing_action),
                onAction = onBack,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> DetailContent(
                state = state,
                tracker = tracker,
                viewModel = viewModel,
                onEditEntry = { onEditEntry(tracker.id, it.id) },
                padding = padding,
            )
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailState,
    tracker: Tracker,
    viewModel: TrackerDetailViewModel,
    onEditEntry: (Entry) -> Unit,
    padding: PaddingValues,
) {
    val today = LocalDate.now()
    val totals = remember(state.times, today) { DetailModels.totals(state.times, today) }
    val counts = remember(state.times) { DetailModels.countsByDay(state.times) }
    val groups = state.groups
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val open = openId?.let(state::find)
    if (open != null) {
        EntrySheet(
            entry = open,
            tracker = tracker,
            onEdit = {
                openId = null
                onEditEntry(open)
            },
            onDelete = {
                openId = null
                viewModel.delete(open)
            },
            onDismiss = { openId = null },
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
        if (tracker.archived) {
            item(key = "archived") {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            stringResource(R.string.detail_archived),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                        FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { viewModel.setArchived(false) }) {
                            Icon(Icons.Rounded.Unarchive, contentDescription = null)
                            Text(stringResource(R.string.detail_unarchive), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
        item(key = "header") {
            TrackerSlate(
                name = tracker.name,
                iconName = tracker.iconName,
                colorIndex = tracker.colorIndex,
                key = tracker.id,
                supporting = tracker.description.ifBlank { stringResource(R.string.detail_no_description) },
            )
        }
        item(key = "totals") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Total(stringResource(R.string.detail_total_all), totals.all, Modifier.weight(1f))
                Total(stringResource(R.string.detail_total_week), totals.lastSevenDays, Modifier.weight(1f))
                Total(stringResource(R.string.detail_total_days), totals.activeDays, Modifier.weight(1f))
            }
        }
        item(key = "switch") {
            Box(Modifier.padding(top = 16.dp)) {
                ConnectedChoices(
                    options = DetailView.entries,
                    selected = state.view,
                    label = {
                        stringResource(
                            when (it) {
                                DetailView.LIST -> R.string.detail_view_list
                                DetailView.CALENDAR -> R.string.detail_view_calendar
                                DetailView.INSIGHTS -> R.string.detail_view_insights
                            }
                        )
                    },
                    onSelect = viewModel::setView,
                )
            }
        }
        if (state.view == DetailView.CALENDAR) {
            item(key = "calendar") {
                MonthCalendar(
                    month = state.month,
                    counts = counts,
                    colorIndex = tracker.colorIndex,
                    selected = state.selectedDay,
                    today = today,
                    firstDay = state.firstDay,
                    onMonth = viewModel::showMonth,
                    onDay = viewModel::selectDay,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        if (state.view == DetailView.INSIGHTS) {
            item(key = "insights") {
                InsightsView(state = state, tracker = tracker, today = today, modifier = Modifier.padding(top = 12.dp))
            }
            return@LazyColumn
        }

        if (state.searching && groups.isEmpty()) {
            item(key = "empty") {
                if (state.searchResults == null) LoadingState()
                else EmptyState(icon = Icons.Rounded.SearchOff, title = stringResource(R.string.detail_no_matches), message = "")
            }
        } else if (groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.EditNote,
                    title = stringResource(if (state.selectedDay != null) R.string.detail_empty_day_title else R.string.detail_empty_title),
                    message = stringResource(if (state.selectedDay != null) R.string.detail_empty_day_message else R.string.detail_empty_message),
                )
            }
        }
        groups.forEach { group ->
            item(key = "day-" + group.day) { SectionHeader(dayTitle(group.day, today)) }
            itemsIndexed(group.entries, key = { _, e -> "entry-" + e.id }) { index, entry ->
                ConnectedItem(index = index, count = group.entries.size, modifier = Modifier.animateItem()) {
                    EntryRow(
                        entry = entry,
                        tracker = tracker,
                        onOpen = { openId = entry.id },
                        onEdit = { onEditEntry(entry) },
                        onDelete = { viewModel.delete(entry) },
                    )
                }
            }
        }
        if (state.hasMore) {
            item(key = "older") {
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::showOlder) {
                        Text(stringResource(R.string.detail_show_older))
                    }
                }
            }
        }
    }
}

@Composable
private fun Total(label: String, value: Int, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(value.toString(), style = MetricTextStyle, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, tracker: Tracker, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val words = rememberValueWords()
    var menu by rememberSaveable { mutableStateOf(false) }
    val time = entry.zoned().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val note = entry.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() && entry.values.isNotEmpty() }
    Box {
        ConnectedRow(
            title = DetailModels.summary(entry, tracker, words).ifEmpty { stringResource(R.string.detail_entry_empty) },
            supporting = listOfNotNull(time, note).joinToString(" · "),
            onClick = onOpen,
            onLongClick = { menu = true },
        )
        EonslateMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            groups = listOf(
                listOf(MenuAction(stringResource(R.string.detail_entry_edit), Icons.Rounded.Edit, onClick = onEdit)),
                listOf(MenuAction(stringResource(R.string.detail_entry_delete), Icons.Rounded.Delete, destructive = true, onClick = onDelete)),
            ),
        )
    }
}

@Composable
private fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(R.string.detail_today)
    today.minusDays(1) -> stringResource(R.string.detail_yesterday)
    // The locale's own order for weekday, day and month, with the year only when it differs.
    else -> day.format(
        if (day.year == today.year) {
            DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "EEEdMMM"))
        } else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    )
}
