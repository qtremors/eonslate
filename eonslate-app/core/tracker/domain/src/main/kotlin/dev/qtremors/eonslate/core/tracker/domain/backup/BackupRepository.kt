package dev.qtremors.eonslate.core.tracker.domain.backup

import kotlinx.coroutines.flow.StateFlow

/** How much tracker data there is, to compare a backup with what is on the phone. */
data class DataCounts(
    val trackers: Int,
    val entries: Int,
    val accounts: Int = 0,
    val transactions: Int = 0,
    val subscriptions: Int = 0,
    val budgets: Int = 0,
)

/** Reads and replaces all tracker data at once, for backups. */
interface BackupRepository {
    /** The database schema this app stores, written into every snapshot. */
    val schema: Int

    /** Every row on the phone, read in one transaction so the snapshot is consistent. */
    suspend fun snapshot(appVersion: String): BackupSnapshot

    /**
     * Replaces everything with [snapshot] in one transaction: either all of it is restored or,
     * when anything fails, nothing on the phone has changed. Check it with [BackupCheck] first.
     */
    suspend fun replaceAll(snapshot: BackupSnapshot)

    suspend fun counts(): DataCounts
}

/**
 * Backups as the app uses them: making a file, opening one, and restoring it in a way that can
 * be taken back. Before every restore, what is on the phone is kept as a restore point, so a
 * wrong file or a change of mind never costs any data.
 */
interface Backups {
    /** When the current restore point was kept, or null when there is none. */
    val restorePoint: StateFlow<Long?>

    /** A backup file of everything, encrypted when [password] is not empty. */
    suspend fun create(password: CharArray?, appVersion: String): ByteArray

    /** Whether [bytes] is a backup and needs a password, without reading the rest. */
    fun inspect(bytes: ByteArray): ArchiveHeader

    /** Opens and checks a backup file; throws [BackupException] when it cannot be restored. */
    suspend fun open(bytes: ByteArray, password: CharArray?): BackupSnapshot

    suspend fun counts(): DataCounts

    /** Keeps a restore point of what is here now, then replaces it all with [snapshot]. */
    suspend fun restore(snapshot: BackupSnapshot, appVersion: String)

    /** Puts back the restore point and forgets it. Anything logged since the restore is lost. */
    suspend fun undoRestore()
}
