package dev.qtremors.eonslate.feature.finance

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvException
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvFiles
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvProblem
import dev.qtremors.eonslate.core.tracker.domain.csv.FinanceCsv
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.MenuAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Writer
import javax.inject.Inject

internal data class FinanceCsvState(
    val busy: Boolean = false,
    val pendingCount: Int? = null,
    val notice: FinanceCsvNotice? = null,
)

internal sealed interface FinanceCsvNotice {
    data object Exported : FinanceCsvNotice
    data class Imported(val count: Int) : FinanceCsvNotice
    data class Failed(val row: Int = 0) : FinanceCsvNotice
}

@HiltViewModel
internal class FinanceCsvViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: FinanceRepository,
    private val ids: IdGenerator,
) : ViewModel() {
    private val resolver = context.contentResolver
    private val _state = MutableStateFlow(FinanceCsvState())
    val state = _state.asStateFlow()
    private var pending: List<Transaction>? = null
    private var exportSnapshot: Ledger? = null
    private var exportWriter: (Ledger, Writer) -> Unit = FinanceCsv::write

    /** Holds [ledger] until a file is picked; [write] turns it into CSV, transactions by default. */
    fun prepareExport(ledger: Ledger, write: (Ledger, Writer) -> Unit = FinanceCsv::write) {
        exportSnapshot = ledger
        exportWriter = write
    }

    fun finishExport(uri: Uri?) {
        val ledger = exportSnapshot
        val write = exportWriter
        exportSnapshot = null
        if (uri == null) return
        work {
            checkNotNull(ledger)
            withContext(Dispatchers.IO) {
                checkNotNull(resolver.openOutputStream(uri, "wt")).bufferedWriter().use { write(ledger, it) }
            }
            _state.value = FinanceCsvState(notice = FinanceCsvNotice.Exported)
        }
    }

    fun preview(uri: Uri, fallbackId: String) = work {
        val ledger = repository.observeLedger().first()
        val fallback = ledger.accounts.firstOrNull { it.id == fallbackId } ?: throw CsvException(CsvProblem.ENTRY)
        val entries = withContext(Dispatchers.IO) {
            val text = checkNotNull(resolver.openInputStream(uri)).use(CsvFiles::read)
            FinanceCsv.read(text, ledger.accounts, fallback, ids)
        }
        pending = entries
        _state.value = FinanceCsvState(pendingCount = entries.size)
    }

    fun append() {
        val entries = pending ?: return
        work {
            pending = null
            if (!repository.appendTransactions(entries)) throw CsvException(CsvProblem.ENTRY)
            _state.value = FinanceCsvState(notice = FinanceCsvNotice.Imported(entries.size))
        }
    }

    fun discard() {
        pending = null
        _state.value = FinanceCsvState()
    }

    fun noticeShown() {
        _state.value = _state.value.copy(notice = null)
    }

    private fun work(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.value = FinanceCsvState(busy = true)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: CsvException) {
                _state.value = FinanceCsvState(notice = FinanceCsvNotice.Failed(error.row))
            } catch (_: Exception) {
                _state.value = FinanceCsvState(notice = FinanceCsvNotice.Failed())
            }
        }
    }
}

@Composable
internal fun FinanceCsvMenu(
    ledger: Ledger?, viewModel: FinanceCsvViewModel, onExport: () -> Unit, more: List<MenuAction> = emptyList(),
    exportLabel: String = stringResource(R.string.finance_csv_export),
) {
    val accounts = ledger?.accounts.orEmpty()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var chooseAccount by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    val account = accounts.firstOrNull { it.id == selected } ?: accounts.firstOrNull()
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && account != null) viewModel.preview(uri, account.id)
    }
    Box {
        IconButton(shapes = IconButtonDefaults.shapes(), enabled = !state.busy, onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.finance_csv_options))
        }
        EonslateMenu(
            expanded,
            onDismissRequest = { expanded = false },
            groups = listOf(more, listOf(
                MenuAction(exportLabel, Icons.Rounded.FileDownload, enabled = ledger != null, onClick = onExport),
                MenuAction(stringResource(R.string.finance_csv_import), Icons.Rounded.FileUpload, enabled = accounts.isNotEmpty()) {
                    chooseAccount = true
                },
            )),
        )
    }
    if (chooseAccount && account != null) {
        EditorSheet(stringResource(R.string.finance_csv_import), busy = false, failed = false, onDismiss = { chooseAccount = false }) {
            AccountChoice(stringResource(R.string.finance_csv_fallback), account, accounts, true) { selected = it.id }
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    chooseAccount = false
                    open.launch(CsvFiles.importTypes)
                },
            ) { Text(stringResource(R.string.finance_csv_choose)) }
        }
    }
}

@Composable
internal fun FinanceCsvFeedback(viewModel: FinanceCsvViewModel, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.pendingCount?.let { count ->
        ConfirmDialog(
            icon = Icons.Rounded.FileUpload,
            title = stringResource(R.string.finance_csv_import),
            message = pluralStringResource(R.plurals.finance_csv_review, count, count),
            confirmLabel = stringResource(R.string.finance_csv_add),
            onConfirm = viewModel::append,
            onDismiss = viewModel::discard,
            destructive = false,
        )
    }
    val notice = state.notice
    val message = when (notice) {
        null -> null
        FinanceCsvNotice.Exported -> stringResource(R.string.finance_csv_saved)
        is FinanceCsvNotice.Imported -> pluralStringResource(R.plurals.finance_csv_imported, notice.count, notice.count)
        is FinanceCsvNotice.Failed -> if (notice.row > 0) stringResource(R.string.finance_csv_row, notice.row)
        else stringResource(R.string.finance_csv_failed)
    }
    LaunchedEffect(notice) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.noticeShown()
        }
    }
}
