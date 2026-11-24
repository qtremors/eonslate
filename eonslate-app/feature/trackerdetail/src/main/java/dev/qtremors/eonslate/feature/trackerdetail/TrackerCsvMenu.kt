package dev.qtremors.eonslate.feature.trackerdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvProblem
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvFiles
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.MenuAction

@Composable
internal fun TrackerCsvMenu(trackerName: String, viewModel: TrackerCsvViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CsvFiles.MIME)) { uri ->
        uri?.let(viewModel::export)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::preview)
    }
    Box {
        IconButton(shapes = IconButtonDefaults.shapes(), enabled = !state.busy, onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.detail_csv_actions))
        }
        EonslateMenu(
            expanded,
            onDismissRequest = { expanded = false },
            groups = listOf(listOf(
                MenuAction(stringResource(R.string.detail_csv_export), Icons.Rounded.FileDownload) {
                    val name = trackerName.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(60).ifBlank { "tracker" }
                    create.launch("$name.csv")
                },
                MenuAction(stringResource(R.string.detail_csv_import), Icons.Rounded.FileUpload) {
                    open.launch(CsvFiles.importTypes)
                },
            )),
        )
    }
}

@Composable
internal fun TrackerCsvFeedback(viewModel: TrackerCsvViewModel, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.pendingCount?.let { count ->
        ConfirmDialog(
            icon = Icons.Rounded.FileUpload,
            title = stringResource(R.string.detail_csv_import),
            message = pluralStringResource(R.plurals.detail_csv_review, count, count),
            confirmLabel = stringResource(R.string.detail_csv_add),
            onConfirm = viewModel::append,
            onDismiss = viewModel::discard,
            destructive = false,
        )
    }
    val notice = state.notice
    val message = when (notice) {
        CsvNotice.Exported -> stringResource(R.string.detail_csv_saved)
        is CsvNotice.Imported -> pluralStringResource(R.plurals.detail_csv_imported, notice.count, notice.count)
        is CsvNotice.Invalid -> when (notice.problem) {
            CsvProblem.COLUMNS -> stringResource(R.string.detail_csv_columns)
            CsvProblem.TOO_LARGE -> stringResource(R.string.detail_csv_large)
            CsvProblem.TRACKER_CHANGED -> stringResource(R.string.detail_csv_tracker_changed)
            CsvProblem.ENTRY, CsvProblem.FORMAT -> if (notice.row > 0) {
                stringResource(R.string.detail_csv_row, notice.row)
            } else stringResource(R.string.detail_csv_failed)
        }
        CsvNotice.Failed -> stringResource(R.string.detail_csv_failed)
        null -> null
    }
    LaunchedEffect(notice) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.noticeShown()
        }
    }
}
