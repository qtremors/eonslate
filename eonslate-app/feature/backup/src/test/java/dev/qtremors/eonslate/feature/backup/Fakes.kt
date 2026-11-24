package dev.qtremors.eonslate.feature.backup

import dev.qtremors.eonslate.core.tracker.domain.backup.ArchiveHeader
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupArchive
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupCheck
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupException
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupProblem
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import dev.qtremors.eonslate.core.tracker.domain.backup.Backups
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.tracker.domain.backup.EntryRow
import dev.qtremors.eonslate.core.tracker.domain.backup.TrackerRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

fun snapshotOf(trackers: Int, entries: Int, createdAt: Long = 1_762_740_000_000L) = BackupSnapshot(
    schema = 1,
    createdAt = createdAt,
    appVersion = "2.0.9",
    trackers = (1..trackers).map { TrackerRow("t$it", "Tracker $it", "", "Mood", 0, false, it, null, 0, 0) },
    entries = (1..entries).map { EntryRow("e$it", "t1", it * 1_000L, "UTC", "", 0, 0) },
)

/** [Backups] over a snapshot in memory, with the real file format and checks. */
class FakeBackups(var data: BackupSnapshot = snapshotOf(2, 5)) : Backups {
    private val point = MutableStateFlow<Long?>(null)
    private var kept: BackupSnapshot? = null
    var failNextRestore = false

    override val restorePoint: StateFlow<Long?> = point

    override suspend fun create(password: CharArray?, appVersion: String): ByteArray =
        BackupArchive.encode(data, password, iterations = 10_000)

    override fun inspect(bytes: ByteArray): ArchiveHeader = BackupArchive.inspect(bytes)

    override suspend fun open(bytes: ByteArray, password: CharArray?): BackupSnapshot =
        BackupArchive.decode(bytes, password).also { BackupCheck.requireSound(it, 1) }

    override suspend fun counts() = DataCounts(data.trackers.size, data.entries.size)

    override suspend fun restore(snapshot: BackupSnapshot, appVersion: String) {
        if (failNextRestore) {
            failNextRestore = false
            throw IllegalStateException("trigger refused a row")
        }
        kept = data
        data = snapshot
        point.value = 42L
    }

    override suspend fun undoRestore() {
        data = kept ?: throw BackupException(BackupProblem.NOT_A_BACKUP)
        kept = null
        point.value = null
    }
}

/** Picked files in memory, keyed by their uri. */
class FakeDocuments : Documents {
    val files = mutableMapOf<String, ByteArray>()
    var refuseWrites = false

    override suspend fun read(uri: String): ByteArray = files[uri] ?: throw IOException("missing $uri")

    override suspend fun write(uri: String, bytes: ByteArray) {
        if (refuseWrites) throw IOException("read-only")
        files[uri] = bytes
    }
}
