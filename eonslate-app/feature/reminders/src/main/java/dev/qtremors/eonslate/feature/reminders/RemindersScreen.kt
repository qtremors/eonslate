package dev.qtremors.eonslate.feature.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import dev.qtremors.eonslate.core.reminders.ReminderSync
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.model.Reminder
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.ExpressiveSwitch
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MainActionButton
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(notificationsAllowed(context)) }
    var exactOn by remember { mutableStateOf(ReminderSync.exactAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = notificationsAllowed(context)
        exactOn = ReminderSync.exactAllowed(context)
        onPauseOrDispose {}
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = notificationsAllowed(context)
    }
    RemindersContent(
        state = state,
        notificationsOn = notificationsOn,
        exactOn = exactOn,
        onBack = onBack,
        onSave = { id, minute, days, skip ->
            viewModel.save(id, minute, days, skip)
            // Asked for only once there is a reminder to deliver.
            if (!notificationsOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onToggle = viewModel::setEnabled,
        onDelete = viewModel::delete,
        onTurnOn = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        },
        onAllowExact = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
            }
        },
        modifier = modifier,
    )
}

private fun notificationsAllowed(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

/** The page without permissions or navigation, so it can be shown and tested on its own. */
@Composable
fun RemindersContent(
    state: RemindersUiState?,
    notificationsOn: Boolean,
    onBack: () -> Unit,
    onSave: (id: String?, minuteOfDay: Int, days: Set<DayOfWeek>, skipIfLogged: Boolean) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
    exactOn: Boolean = true,
    onAllowExact: () -> Unit = {},
) {
    // "new" for a new reminder, a reminder id for an edit, null when no sheet is open.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = rememberAppHaptics()

    if (state != null && editing != null) {
        val existing = state.reminders.firstOrNull { it.id == editing }
        ReminderSheet(
            reminder = existing,
            defaultDays = state.defaultDays,
            onSave = { minute, days, skip ->
                onSave(existing?.id, minute, days, skip)
                editing = null
            },
            onDelete = existing?.let { r -> { onDelete(r.id); editing = null } },
            onDismiss = { editing = null },
        )
    }

    EonslateScaffold(
        title = stringResource(R.string.reminders_title),
        onBack = onBack,
        modifier = modifier,
        floatingActionButton = {
            if (state != null) MainActionButton(stringResource(R.string.reminders_add), Icons.Rounded.Add, onClick = { editing = "new" })
        },
    ) { padding ->
        if (state == null) {
            LoadingState(Modifier.fillMaxSize())
            return@EonslateScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            if (!notificationsOn && state.reminders.isNotEmpty()) {
                item(key = "off") {
                    ConnectedGroup {
                        ConnectedRow(
                            title = stringResource(R.string.reminders_off),
                            leading = { IconBadge(icon = Icons.Rounded.NotificationsOff, key = "notifications_off") },
                            trailing = { TextButton(shapes = ButtonDefaults.shapes(), onClick = onTurnOn) { Text(stringResource(R.string.reminders_turn_on)) } },
                        )
                    }
                }
            }
            if (!exactOn && state.reminders.isNotEmpty()) {
                item(key = "exact") {
                    ConnectedGroup {
                        ConnectedRow(
                            title = stringResource(R.string.reminders_exact_off),
                            supporting = stringResource(R.string.reminders_exact_off_hint),
                            leading = { IconBadge(icon = Icons.Rounded.Schedule, key = "exact_off") },
                            trailing = { TextButton(shapes = ButtonDefaults.shapes(), onClick = onAllowExact) { Text(stringResource(R.string.reminders_allow)) } },
                        )
                    }
                }
            }
            if (state.reminders.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.NotificationsNone,
                        title = stringResource(R.string.reminders_empty_title),
                        message = stringResource(R.string.reminders_empty_message, state.trackerName),
                    )
                }
            } else {
                item(key = "list") {
                    ConnectedGroup {
                        state.reminders.forEach { reminder ->
                            ConnectedRow(
                                title = timeText(reminder.minuteOfDay),
                                supporting = daysText(reminder.days),
                                onClick = { editing = reminder.id },
                                trailing = {
                                    ExpressiveSwitch(
                                        checked = reminder.enabled,
                                        onCheckedChange = { on ->
                                            haptics.toggle(on)
                                            onToggle(reminder, on)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSheet(
    reminder: Reminder?,
    defaultDays: Set<DayOfWeek>,
    onSave: (minuteOfDay: Int, days: Set<DayOfWeek>, skipIfLogged: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val start = reminder?.minuteOfDay ?: (20 * 60)
    val time = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60)
    var days by remember { mutableStateOf(reminder?.days ?: defaultDays) }
    var skip by remember { mutableStateOf(reminder?.skipIfLogged ?: true) }
    val haptics = rememberAppHaptics()

    EonslateBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(if (reminder == null) R.string.reminders_sheet_new else R.string.reminders_sheet_edit),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TimeInput(state = time)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                weekOrder().forEach { day ->
                    val on = day in days
                    FilterChip(
                        selected = on,
                        onClick = {
                            haptics.toggle(!on)
                            days = if (on) days - day else days + day
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        shape = MaterialTheme.shapes.medium,
                        // The accent, so a picked day stands out on black in Mono.
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
            if (days.isEmpty()) {
                Text(stringResource(R.string.reminders_no_days), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            ConnectedGroup {
                ToggleRow(title = stringResource(R.string.reminders_skip), checked = skip, onCheckedChange = { skip = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (onDelete != null) {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = onDelete) {
                        Text(stringResource(R.string.reminders_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.reminders_cancel)) }
                Button(
                    shapes = ButtonDefaults.shapes(),
                    enabled = days.isNotEmpty(),
                    onClick = { onSave(time.hour * 60 + time.minute, days, skip) },
                ) { Text(stringResource(R.string.reminders_save)) }
            }
        }
    }
}

/** The week starting on the locale's first day. */
private fun weekOrder(): List<DayOfWeek> {
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}

private fun timeText(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
private fun daysText(days: Set<DayOfWeek>): String =
    if (days.size == 7) stringResource(R.string.reminders_every_day)
    else weekOrder().filter { it in days }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
