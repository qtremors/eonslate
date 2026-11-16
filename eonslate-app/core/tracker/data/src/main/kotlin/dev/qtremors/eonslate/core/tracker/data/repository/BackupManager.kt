package dev.qtremors.eonslate.core.tracker.data.repository

import dev.qtremors.eonslate.core.tracker.domain.backup.ArchiveHeader
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupArchive
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupCheck
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupException
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupProblem
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import dev.qtremors.eonslate.core.tracker.domain.backup.Backups
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [Backups] on the phone. The restore point is a plain backup file in the app's private files,
 * as private as the database it copies, and is written to a temporary file that only takes its
 * place once the restore succeeded: a failed restore leaves the previous point untouched.
 *
 * One operation runs at a time, so two quick taps can never restore over each other.
 *
 * @param directory where the restore point lives, `files/backups` in the app.
 * @param work the dispatcher for key derivation, compression and file access.
 */
class BackupManager(
    private val repository: BackupRepository,
    private val directory: File,
    private val work: CoroutineDispatcher,
    private val iterations: Int = BackupArchive.DEFAULT_ITERATIONS,
) : Backups {
    private val lock = Mutex()
    private val point = File(directory, RESTORE_POINT)
    private val pending = File(directory, "$RESTORE_POINT.tmp")
    private val _restorePoint = MutableStateFlow(point.takeIf(File::isFile)?.lastModified())

    override val restorePoint: StateFlow<Long?> = _restorePoint.asStateFlow()

    override suspend fun create(password: CharArray?, appVersion: String): ByteArray = lock.withLock {
        val snapshot = repository.snapshot(appVersion)
        withContext(work) { BackupArchive.encode(snapshot, password, iterations) }
    }

    override fun inspect(bytes: ByteArray): ArchiveHeader = BackupArchive.inspect(bytes)

    override suspend fun open(bytes: ByteArray, password: CharArray?): BackupSnapshot = withContext(work) {
        BackupArchive.decode(bytes, password).also { BackupCheck.requireSound(it, repository.schema) }
    }

    override suspend fun counts(): DataCounts = repository.counts()

    override suspend fun restore(snapshot: BackupSnapshot, appVersion: String) = lock.withLock {
        BackupCheck.requireSound(snapshot, repository.schema)
        val current = repository.snapshot(appVersion)
        withContext(work) {
            directory.mkdirs()
            pending.writeBytes(BackupArchive.encode(current, password = null))
        }
        try {
            repository.replaceAll(snapshot)
        } catch (e: Exception) {
            withContext(work) { pending.delete() }
            throw e
        }
        withContext(work) {
            // On the same directory a rename replaces the old point in one step.
            if (!pending.renameTo(point)) {
                point.delete()
                pending.renameTo(point)
            }
        }
        _restorePoint.value = point.takeIf(File::isFile)?.lastModified()
    }

    override suspend fun undoRestore() = lock.withLock {
        val bytes = withContext(work) { point.takeIf(File::isFile)?.readBytes() }
            ?: throw BackupException(BackupProblem.NOT_A_BACKUP)
        val snapshot = withContext(work) { BackupArchive.decode(bytes, password = null) }
        BackupCheck.requireSound(snapshot, repository.schema)
        repository.replaceAll(snapshot)
        withContext(work) { point.delete() }
        _restorePoint.value = null
    }

    companion object {
        const val RESTORE_POINT = "before-restore.${BackupArchive.EXTENSION}"
    }
}
