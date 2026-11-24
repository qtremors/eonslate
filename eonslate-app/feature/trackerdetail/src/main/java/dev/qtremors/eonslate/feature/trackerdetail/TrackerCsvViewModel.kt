package dev.qtremors.eonslate.feature.trackerdetail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvException
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvFiles
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvProblem
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerCsv
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerCsvRepository
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerHistory
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal data class CsvUiState(
    val busy: Boolean = false,
    val pendingCount: Int? = null,
    val notice: CsvNotice? = null,
)

internal sealed interface CsvNotice {
    data object Exported : CsvNotice
    data class Imported(val count: Int) : CsvNotice
    data class Invalid(val problem: CsvProblem, val row: Int) : CsvNotice
    data object Failed : CsvNotice
}

@HiltViewModel
internal class TrackerCsvViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedState: SavedStateHandle,
    private val trackers: TrackerRepository,
    private val csv: TrackerCsvRepository,
    private val clock: AppClock,
    private val ids: IdGenerator,
) : ViewModel() {
    private val resolver = context.contentResolver
    private val trackerId: String = checkNotNull(savedState[TrackerDetailViewModel.ARG_TRACKER_ID])
    private val _state = MutableStateFlow(CsvUiState())
    val state = _state.asStateFlow()
    private var pending: TrackerHistory? = null

    fun export(uri: Uri) = work {
        val history = csv.readHistory(trackerId) ?: throw CsvException(CsvProblem.TRACKER_CHANGED)
        withContext(Dispatchers.IO) {
            val stream = checkNotNull(resolver.openOutputStream(uri, "wt"))
            stream.bufferedWriter().use { TrackerCsv.write(history.tracker, history.entries, it) }
        }
        _state.value = CsvUiState(notice = CsvNotice.Exported)
    }

    fun preview(uri: Uri) = work {
        val tracker = trackers.getTracker(trackerId) ?: throw CsvException(CsvProblem.TRACKER_CHANGED)
        val entries = withContext(Dispatchers.IO) {
            val source = checkNotNull(resolver.openInputStream(uri)).use(CsvFiles::read)
            TrackerCsv.read(source, tracker, clock.nowMillis(), ids)
        }
        pending = TrackerHistory(tracker, entries)
        _state.value = CsvUiState(pendingCount = entries.size)
    }

    fun append() {
        val data = pending ?: return
        work {
            pending = null
            csv.append(data.tracker, data.entries)
            _state.value = CsvUiState(notice = CsvNotice.Imported(data.entries.size))
        }
    }

    fun discard() {
        pending = null
        _state.value = CsvUiState()
    }

    fun noticeShown() {
        _state.value = _state.value.copy(notice = null)
    }

    private fun work(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.value = CsvUiState(busy = true)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: CsvException) {
                _state.value = CsvUiState(notice = CsvNotice.Invalid(error.problem, error.row))
            } catch (_: Exception) {
                _state.value = CsvUiState(notice = CsvNotice.Failed)
            }
        }
    }
}
