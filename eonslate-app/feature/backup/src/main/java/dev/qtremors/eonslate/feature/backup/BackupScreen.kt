package dev.qtremors.eonslate.feature.backup

import androidx.compose.material.icons.rounded.DeleteForever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupArchive
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSummary
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.PasswordField
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay

/** Short enough to type on a phone, long enough that guessing takes real work. */
const val MIN_PASSWORD = 8

@Composable
fun BackupScreen(
    versionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupArchive.MIME_TYPE)) { uri ->
        if (uri == null) viewModel.cancelCreate() else viewModel.writeBackup(uri.toString(), versionName)
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.openBackup(uri.toString())
    }
    BackupContent(
        state = state,
        onBack = onBack,
        onCreate = { password ->
            viewModel.choosePassword(password)
            createFile.launch("Eonslate-${LocalDate.now()}.${BackupArchive.EXTENSION}")
        },
        // Any file: pickers do not know the .eonslate type, and the header check refuses others.
        onPickBackup = { openFile.launch(arrayOf("*/*")) },
        onUnlock = viewModel::unlock,
        onRestore = { viewModel.restore(versionName) },
        onPutAway = viewModel::putAway,
        onUndo = viewModel::undoRestore,
        onClearAll = { viewModel.clearAll(versionName) },
        onMessageShown = viewModel::messageShown,
        modifier = modifier,
    )
}

/** The page itself, without the file pickers, so it can be shown and tested on its own. */
@Composable
fun BackupContent(
    state: BackupUiState,
    onBack: () -> Unit,
    onCreate: (CharArray) -> Unit,
    onPickBackup: () -> Unit,
    onUnlock: (CharArray) -> Unit,
    onRestore: () -> Unit,
    onPutAway: () -> Unit,
    onUndo: () -> Unit,
    onMessageShown: () -> Unit,
    onClearAll: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var creating by rememberSaveable { mutableStateOf(false) }
    var confirmUndo by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (confirmClear) {
        ConfirmDialog(
            icon = Icons.Rounded.DeleteForever,
            title = stringResource(R.string.backup_clear_dialog),
            message = stringResource(R.string.backup_clear_message),
            confirmLabel = stringResource(R.string.backup_clear_action),
            onConfirm = {
                confirmClear = false
                onClearAll()
            },
            onDismiss = { confirmClear = false },
        )
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val canUndo = message == BackupMessage.RESTORED || message == BackupMessage.CLEARED
        // Cleared only once the snackbar is gone: clearing it first changes this effect's key,
        // which cancels the effect and the snackbar with it.
        val result = try {
            snackbar.showSnackbar(
                message = context.getString(message.text()),
                actionLabel = if (canUndo) context.getString(R.string.backup_snackbar_undo) else null,
                duration = if (canUndo) SnackbarDuration.Long else SnackbarDuration.Short,
            )
        } finally {
            onMessageShown()
        }
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }

    if (creating) {
        CreateSheet(
            onCreate = { password ->
                creating = false
                onCreate(password)
            },
            onDismiss = { creating = false },
        )
    }
    state.unlock?.let { wrong -> UnlockSheet(wrongPassword = wrong, busy = state.busy != null, onUnlock = onUnlock, onDismiss = onPutAway) }
    state.review?.let { review -> ReviewSheet(review, busy = state.busy != null, onRestore = onRestore, onDismiss = onPutAway) }
    if (confirmUndo) {
        ConfirmDialog(
            icon = Icons.Rounded.Undo,
            title = stringResource(R.string.backup_undo_dialog_title),
            message = stringResource(R.string.backup_undo_dialog_message),
            confirmLabel = stringResource(R.string.backup_undo_action),
            onConfirm = {
                confirmUndo = false
                onUndo()
            },
            onDismiss = { confirmUndo = false },
        )
    }

    val idle = state.busy == null
    EonslateScaffold(
        title = stringResource(R.string.backup_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "intro") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    state.counts?.let { counts ->
                        Text(
                            stringResource(R.string.backup_on_phone, countText(counts.trackers, counts.entries, counts.accounts, counts.transactions, counts.subscriptions, counts.budgets)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "busy") {
                if (state.busy != null) BusyCard(state.busy)
            }
            item(key = "create") {
                SectionHeader(stringResource(R.string.backup_section_create))
                ConnectedGroup {
                    ConnectedRow(
                        title = stringResource(R.string.backup_create),
                        leading = { IconBadge(icon = Icons.Rounded.SaveAlt, key = "backup_create") },
                        onClick = { creating = true },
                        enabled = idle,
                    )
                }
            }
            item(key = "restore") {
                SectionHeader(stringResource(R.string.backup_section_restore))
                ConnectedGroup {
                    ConnectedRow(
                        title = stringResource(R.string.backup_restore),
                        leading = { IconBadge(icon = Icons.Rounded.Restore, key = "backup_restore") },
                        onClick = onPickBackup,
                        enabled = idle,
                    )
                    state.restorePoint?.let { at ->
                        ConnectedRow(
                            title = stringResource(R.string.backup_undo),
                            supporting = stringResource(R.string.backup_undo_hint, dateTime(at)),
                            leading = { IconBadge(icon = Icons.Rounded.Undo, key = "backup_undo") },
                            onClick = { confirmUndo = true },
                            enabled = idle,
                        )
                    }
                }
                SectionHeader(stringResource(R.string.backup_section_danger))
                ConnectedGroup {
                    ConnectedRow(
                        title = stringResource(R.string.backup_clear),
                        supporting = stringResource(R.string.backup_clear_hint),
                        leading = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { confirmClear = true },
                        enabled = idle,
                    )
                }
            }
        }
    }
}

@Composable
private fun BusyCard(busy: Busy) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = SlateCardShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LoadingIndicator(Modifier.size(40.dp))
            Text(
                stringResource(
                    when (busy) {
                        Busy.CREATING -> R.string.backup_busy_creating
                        Busy.OPENING -> R.string.backup_busy_opening
                        Busy.RESTORING -> R.string.backup_busy_restoring
                        Busy.UNDOING -> R.string.backup_busy_undoing
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** Choose whether to encrypt, and the password twice, before the system asks where to save. */
@Composable
private fun CreateSheet(onCreate: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var protect by rememberSaveable { mutableStateOf(true) }
    // Not saveable on purpose: a password is never written into saved instance state.
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val short = protect && password.length < MIN_PASSWORD
    val mismatch = protect && password != again
    val haptics = rememberAppHaptics()

    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.backup_sheet_create)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.backup_protect),
                    checked = protect,
                    onCheckedChange = { protect = it },
                    icon = Icons.Rounded.Lock,
                )
            }
            if (protect) {
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.backup_password),
                    error = if (tried && short) stringResource(R.string.backup_password_short, MIN_PASSWORD) else null,
                )
                PasswordField(
                    value = again,
                    onValueChange = { again = it },
                    label = stringResource(R.string.backup_password_again),
                    error = if (tried && !short && mismatch) stringResource(R.string.backup_password_mismatch) else null,
                    imeAction = ImeAction.Done,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.backup_cancel)) }
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        tried = true
                        if (short || mismatch) {
                            haptics.reject()
                        } else {
                            val chosen = if (protect) password.toCharArray() else CharArray(0)
                            password = ""
                            again = ""
                            onCreate(chosen)
                        }
                    },
                ) { Text(stringResource(R.string.backup_choose_place)) }
            }
        }
    }
}

@Composable
private fun UnlockSheet(wrongPassword: Boolean, busy: Boolean, onUnlock: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.backup_sheet_unlock)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.backup_password),
                error = if (wrongPassword) stringResource(R.string.backup_wrong_password) else null,
                imeAction = ImeAction.Done,
            )
            if (busy) BusyCard(Busy.OPENING)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.backup_cancel)) }
                Button(
                    shapes = ButtonDefaults.shapes(),
                    enabled = password.isNotEmpty() && !busy,
                    onClick = { onUnlock(password.toCharArray()) },
                ) { Text(stringResource(R.string.backup_open)) }
            }
        }
    }
}

/** What the backup holds next to what is here now, then the choice. */
@Composable
private fun ReviewSheet(review: Review, busy: Boolean, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val backup: BackupSummary = review.backup
    val legacy = backup.legacy
    val current: DataCounts = review.current
    val colors = MaterialTheme.colorScheme
    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.backup_sheet_review)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (legacy == null) countText(backup.trackers, backup.entries, backup.accounts, backup.transactions, backup.subscriptions, backup.budgets)
                        else listOfNotNull(
                            if (legacy.transactions) pluralStringResource(R.plurals.backup_transactions, backup.transactions, backup.transactions) else null,
                            if (legacy.budgets) pluralStringResource(R.plurals.backup_budgets, backup.budgets, backup.budgets) else null,
                            if (legacy.subscriptions) pluralStringResource(R.plurals.backup_subscriptions, backup.subscriptions, backup.subscriptions) else null,
                            if (legacy.categories) pluralStringResource(R.plurals.backup_categories, backup.categories, backup.categories) else null,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    if (backup.archived > 0) {
                        Text(stringResource(R.string.backup_review_archived, backup.archived), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Text(
                        stringResource(R.string.backup_review_made, dateTime(backup.createdAt), backup.appVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                if (legacy == null) stringResource(R.string.backup_review_replaces, countText(current.trackers, current.entries, current.accounts, current.transactions, current.subscriptions, current.budgets))
                else stringResource(R.string.backup_review_legacy),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (busy) BusyCard(Busy.RESTORING)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(shapes = ButtonDefaults.shapes(), enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.backup_cancel)) }
                Button(shapes = ButtonDefaults.shapes(), enabled = !busy, onClick = onRestore) { Text(stringResource(R.string.backup_restore_action)) }
            }
        }
    }
}

@Composable
private fun countText(trackers: Int, entries: Int, accounts: Int, transactions: Int, subscriptions: Int, budgets: Int): String {
    // Grouped digits, so 3,147 transactions read at a glance.
    val numbers = java.text.NumberFormat.getIntegerInstance(LocalValueDisplay.current.numberLocale)
    @Composable
    fun count(id: Int, n: Int) = pluralStringResource(id, n, numbers.format(n))
    val counts = mutableListOf(count(R.plurals.backup_trackers, trackers), count(R.plurals.backup_entries, entries))
    if (accounts > 0 || transactions > 0) {
        counts += count(R.plurals.backup_accounts, accounts)
        counts += count(R.plurals.backup_transactions, transactions)
    }
    if (subscriptions > 0) counts += count(R.plurals.backup_subscriptions, subscriptions)
    if (budgets > 0) counts += count(R.plurals.backup_budgets, budgets)
    return counts.joinToString(", ")
}

@Composable
private fun dateTime(millis: Long): String {
    val date = Date(millis)
    if (LocalValueDisplay.current.datePattern == null) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
    }
    val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    return displayDate(day) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
}

private fun BackupMessage.text(): Int = when (this) {
    BackupMessage.SAVED -> R.string.backup_saved
    BackupMessage.RESTORED -> R.string.backup_restored
    BackupMessage.UNDONE -> R.string.backup_undone
    BackupMessage.CLEARED -> R.string.backup_cleared
    BackupMessage.NOT_A_BACKUP -> R.string.backup_error_not_backup
    BackupMessage.NEWER_VERSION -> R.string.backup_error_newer
    BackupMessage.DAMAGED -> R.string.backup_error_damaged
    BackupMessage.INCONSISTENT -> R.string.backup_error_inconsistent
    BackupMessage.READ_FAILED -> R.string.backup_error_read
    BackupMessage.WRITE_FAILED -> R.string.backup_error_write
    BackupMessage.RESTORE_FAILED -> R.string.backup_error_restore
}
