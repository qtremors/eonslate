package dev.qtremors.eonslate.feature.entry

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.ui.message
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.EmberActionButton
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun LogEntryScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    LaunchedEffect(state.done) {
        if (state.done) {
            haptics.confirm()
            onDone()
        }
    }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    val draft = state.draft
    EonslateScaffold(
        title = state.tracker?.name ?: stringResource(R.string.log_title),
        onBack = { if (state.dirty) confirmDiscard = true else onDone() },
        modifier = modifier,
        actions = {
            if (draft != null && !draft.isNew) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.log_delete))
                }
            }
        },
        floatingActionButton = {
            if (draft != null) {
                EmberActionButton(
                    text = stringResource(if (draft.isNew) R.string.log_save else R.string.log_update),
                    icon = Icons.Rounded.Check,
                    onClick = viewModel::save,
                )
            }
        },
    ) { padding ->
        val tracker = state.tracker
        when {
            state.loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.missing || tracker == null || draft == null -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(R.string.log_missing_title),
                message = stringResource(R.string.log_missing_message),
                actionLabel = stringResource(R.string.log_missing_action),
                onAction = onDone,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tracker.description.isNotBlank()) {
                    item(key = "about") {
                        Text(
                            text = tracker.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                item(key = "when") { WhenRow(state, viewModel) }
                if (state.canUseLast) {
                    item(key = "last") {
                        FilledTonalButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = viewModel::useLastValues,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null)
                            Text(stringResource(R.string.log_same_as_last), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                items(tracker.fields, key = { it.id }) { field ->
                    FieldInputCard(
                        field = field,
                        input = draft.inputs[field.id],
                        error = when {
                            field.id in state.unreadable -> stringResource(R.string.log_unreadable)
                            else -> state.errorFor(field.id)?.message()
                        },
                        onChange = { change -> viewModel.setInput(field.id, change) },
                        onClear = { viewModel.clearInput(field.id) },
                    )
                }
                item(key = "note") {
                    Column {
                        SectionHeader(stringResource(R.string.log_note_header))
                        FormField(
                            value = draft.note,
                            onValueChange = viewModel::setNote,
                            label = stringResource(R.string.log_note),
                            singleLine = false,
                            minLines = 2,
                            maxLength = Entry.NOTE_MAX_LENGTH,
                            error = state.entryErrors.firstOrNull()?.message(),
                        )
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            icon = Icons.Rounded.Warning,
            title = stringResource(R.string.log_discard_title),
            message = stringResource(R.string.log_discard_message),
            confirmLabel = stringResource(R.string.log_discard_confirm),
            onConfirm = {
                confirmDiscard = false
                onDone()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            icon = Icons.Rounded.DeleteForever,
            title = stringResource(R.string.log_delete_title),
            message = stringResource(R.string.log_delete_message),
            confirmLabel = stringResource(R.string.log_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** The day and time the entry happened, each opening its own picker. */
@Composable
private fun WhenRow(state: LogState, viewModel: LogEntryViewModel) {
    val occurred = state.occurred ?: return
    val context = LocalContext.current
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val dayLabel = when (DayLabel.of(occurred.toLocalDate(), LocalDate.now(occurred.zone))) {
        DayLabel.TODAY -> stringResource(R.string.log_today)
        DayLabel.YESTERDAY -> stringResource(R.string.log_yesterday)
        DayLabel.DATE -> occurred.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val timeLabel = occurred.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null)
            Text(dayLabel, modifier = Modifier.padding(start = 8.dp))
        }
        FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null)
            Text(timeLabel, modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (pickDate) {
        // The picker works in UTC days; convert the entry's local day both ways.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = occurred.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.log_ok)) }
            },
            dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { pickDate = false }) { Text(stringResource(R.string.log_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
    if (pickTime) {
        val timeState = rememberTimePickerState(
            initialHour = occurred.hour,
            initialMinute = occurred.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text(stringResource(R.string.log_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    viewModel.setTime(LocalTime.of(timeState.hour, timeState.minute))
                    pickTime = false
                }) { Text(stringResource(R.string.log_ok)) }
            },
            dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { pickTime = false }) { Text(stringResource(R.string.log_cancel)) } },
        )
    }
}

/** How the log screen names an entry's day: by word for the two closest days, else by date. */
internal enum class DayLabel {
    TODAY, YESTERDAY, DATE;

    companion object {
        fun of(day: LocalDate, today: LocalDate): DayLabel = when (day) {
            today -> TODAY
            today.minusDays(1) -> YESTERDAY
            else -> DATE
        }
    }
}
