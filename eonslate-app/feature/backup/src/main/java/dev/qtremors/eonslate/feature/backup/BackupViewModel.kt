package dev.qtremors.eonslate.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupException
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupProblem
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSummary
import dev.qtremors.eonslate.core.tracker.domain.backup.Backups
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** What the page is waiting on while it works; rows are disabled meanwhile. */
enum class Busy { CREATING, OPENING, RESTORING, UNDOING }

/** One thing to tell the person once, as a snackbar. */
enum class BackupMessage {
    SAVED, RESTORED, UNDONE,
    NOT_A_BACKUP, NEWER_VERSION, DAMAGED, INCONSISTENT,
    READ_FAILED, WRITE_FAILED, RESTORE_FAILED,
}

/** An opened backup, waiting for the person to choose to restore it. */
data class Review(val backup: BackupSummary, val current: DataCounts)

data class BackupUiState(
    val counts: DataCounts? = null,
    val restorePoint: Long? = null,
    val busy: Busy? = null,
    /** An encrypted file is open and needs its password; true after a wrong one. */
    val unlock: Boolean? = null,
    val review: Review? = null,
    val message: BackupMessage? = null,
)

/**
 * The backup page. A password lives here only as a [CharArray] between the sheet and the file
 * picker, and is wiped as soon as the backup is written or the picker is closed; an opened file
 * is held only until it is restored or put away.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backups: Backups,
    private val documents: Documents,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private var password: CharArray? = null
    private var opened: ByteArray? = null
    private var snapshot: BackupSnapshot? = null

    init {
        viewModelScope.launch { backups.restorePoint.collect { at -> _state.update { it.copy(restorePoint = at) } } }
        refreshCounts()
    }

    /** Keeps [chosen] for the file about to be picked; empty means no encryption. */
    fun choosePassword(chosen: CharArray) {
        wipePassword()
        password = chosen
    }

    /** The picker closed without a place: the password is not needed any more. */
    fun cancelCreate() = wipePassword()

    fun writeBackup(uri: String, appVersion: String) = work(Busy.CREATING) {
        try {
            val bytes = backups.create(password, appVersion)
            try {
                documents.write(uri, bytes)
                say(BackupMessage.SAVED)
            } catch (e: IOException) {
                say(BackupMessage.WRITE_FAILED)
            } catch (e: SecurityException) {
                say(BackupMessage.WRITE_FAILED)
            }
        } finally {
            wipePassword()
        }
    }

    fun openBackup(uri: String) = work(Busy.OPENING) {
        putAway()
        val bytes = try {
            documents.read(uri)
        } catch (e: IOException) {
            return@work say(BackupMessage.READ_FAILED)
        } catch (e: SecurityException) {
            return@work say(BackupMessage.READ_FAILED)
        }
        val header = try {
            backups.inspect(bytes)
        } catch (e: BackupException) {
            return@work fail(e)
        }
        opened = bytes
        if (header.encrypted) _state.update { it.copy(unlock = false) } else decode(bytes, null)
    }

    fun unlock(entered: CharArray) = work(Busy.OPENING) {
        val bytes = opened ?: return@work
        try {
            decode(bytes, entered)
        } finally {
            entered.fill('\u0000')
        }
    }

    /** Closes the unlock or review sheet without restoring anything. */
    fun putAway() {
        opened = null
        snapshot = null
        _state.update { it.copy(unlock = null, review = null) }
    }

    fun restore(appVersion: String) = work(Busy.RESTORING) {
        val chosen = snapshot ?: return@work
        try {
            backups.restore(chosen, appVersion)
            putAway()
            say(BackupMessage.RESTORED)
        } catch (e: BackupException) {
            putAway()
            fail(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The database refused it inside its transaction, so nothing on the phone changed.
            putAway()
            say(BackupMessage.RESTORE_FAILED)
        }
    }

    fun undoRestore() = work(Busy.UNDOING) {
        try {
            backups.undoRestore()
            say(BackupMessage.UNDONE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            say(BackupMessage.RESTORE_FAILED)
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private suspend fun decode(bytes: ByteArray, entered: CharArray?) {
        try {
            val read = backups.open(bytes, entered)
            snapshot = read
            _state.update { it.copy(unlock = null, review = Review(read.summary, backups.counts())) }
        } catch (e: BackupException) {
            if (e.problem == BackupProblem.WRONG_PASSWORD) {
                _state.update { it.copy(unlock = true) }
            } else {
                putAway()
                fail(e)
            }
        }
    }

    private fun fail(e: BackupException) = say(
        when (e.problem) {
            BackupProblem.NOT_A_BACKUP, BackupProblem.PASSWORD_NEEDED, BackupProblem.WRONG_PASSWORD -> BackupMessage.NOT_A_BACKUP
            BackupProblem.NEWER_VERSION -> BackupMessage.NEWER_VERSION
            BackupProblem.DAMAGED -> BackupMessage.DAMAGED
            BackupProblem.INCONSISTENT -> BackupMessage.INCONSISTENT
        }
    )

    private fun say(message: BackupMessage) = _state.update { it.copy(message = message) }

    /** Runs one operation at a time; a tap while busy is ignored. */
    private fun work(kind: Busy, block: suspend () -> Unit) {
        if (_state.value.busy != null) return
        _state.update { it.copy(busy = kind) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _state.update { it.copy(busy = null) }
                refreshCounts()
            }
        }
    }

    private fun refreshCounts() {
        viewModelScope.launch {
            val counts = backups.counts()
            _state.update { it.copy(counts = counts) }
        }
    }

    private fun wipePassword() {
        password?.fill('\u0000')
        password = null
    }

    override fun onCleared() {
        wipePassword()
        opened = null
        snapshot = null
    }
}
