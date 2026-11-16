package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors

/**
 * The tracker database. Every schema version is exported to `core/tracker/data/schemas` and
 * kept in git; a new version adds a [Migration] to [TrackerMigrations.ALL] and a test that walks
 * from every older schema to it.
 */
@Database(
    entities = [
        TrackerEntity::class,
        TrackerFieldEntity::class,
        EntryEntity::class,
        EntryValueEntity::class,
    ],
    version = TrackerDatabase.VERSION,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
    abstract fun entryDao(): EntryDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val VERSION = 1
        const val NAME = "eonslate.db"

        /**
         * Applies what every builder of this database needs: the migrations and the integrity
         * triggers. Production and tests both go through here, so tests see the real rules.
         */
        fun RoomDatabase.Builder<TrackerDatabase>.configure(): RoomDatabase.Builder<TrackerDatabase> =
            addMigrations(*TrackerMigrations.ALL)
                .addCallback(IntegrityCallback)
    }
}

/**
 * Installs the current triggers every time the database opens, after any migration ran. A
 * database created, migrated or restored by any path therefore always carries today's rules,
 * and no migration can forget them. It is a dozen small statements, once per process.
 */
internal object IntegrityCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            TrackerIntegrity.install(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/** Every migration, oldest first. Triggers are reinstalled after them by [IntegrityCallback]. */
object TrackerMigrations {
    val ALL: Array<Migration> = emptyArray()
}

/**
 * SQLite triggers that refuse rows breaking the model's rules, as a last line of defence under
 * the validators. A bug, a bad restore or a hand-edited database fails loudly instead of
 * storing values the app would later misread.
 *
 * Room does not record triggers in its schema, so [install] drops and recreates them all and is
 * safe to run any number of times.
 */
object TrackerIntegrity {
    private val TYPES = FieldType.entries.joinToString(",") { "'${it.storageKey}'" }

    /** Kinds stored in `long_value`; every other kind is stored in `text_value`. */
    private val LONG_TYPES = FieldType.entries.filter(ValueColumns::usesLong).joinToString(",") { "'${it.storageKey}'" }

    /** Table name to the condition that makes a new row invalid. */
    private val rules: Map<String, String> = mapOf(
        "trackers" to """
            length(trim(NEW.name)) = 0
            OR length(trim(NEW.icon_name)) = 0
            OR NEW.color_index < 0 OR NEW.color_index >= ${TrackerColors.COUNT}
            OR NEW.archived NOT IN (0, 1)
            OR NEW.updated_at < NEW.created_at
        """,
        "tracker_fields" to """
            length(trim(NEW.name)) = 0
            OR NEW.type NOT IN ($TYPES)
            OR length(NEW.config_json) = 0
            OR NEW.required NOT IN (0, 1)
        """,
        "entries" to """
            length(trim(NEW.zone_id)) = 0
            OR NEW.updated_at < NEW.created_at
        """,
        "entry_values" to """
            NEW.type NOT IN ($TYPES)
            OR (NEW.type IN ($LONG_TYPES) AND (NEW.long_value IS NULL OR NEW.text_value IS NOT NULL))
            OR (NEW.type NOT IN ($LONG_TYPES) AND (NEW.text_value IS NULL OR NEW.long_value IS NOT NULL))
            OR (NEW.type = 'yes_no' AND NEW.long_value NOT IN (0, 1))
            OR (NEW.type = 'rating' AND NEW.long_value < 1)
            OR (NEW.type = 'duration' AND NEW.long_value < 0)
            OR NEW.type IS NOT (SELECT type FROM tracker_fields WHERE id = NEW.field_id)
            OR (SELECT tracker_id FROM tracker_fields WHERE id = NEW.field_id)
                IS NOT (SELECT tracker_id FROM entries WHERE id = NEW.entry_id)
        """,
    )

    /** Names of every trigger [install] creates, for tests and future migrations. */
    val TRIGGER_NAMES: List<String> = rules.keys.flatMap { listOf("validate_${it}_insert", "validate_${it}_update") } +
        listOf("guard_field_type_change", "guard_entry_tracker_change")

    fun install(db: SupportSQLiteDatabase) {
        rules.forEach { (table, invalidWhen) ->
            listOf("INSERT" to "insert", "UPDATE" to "update").forEach { (operation, suffix) ->
                db.execSQL("DROP TRIGGER IF EXISTS validate_${table}_$suffix")
                db.execSQL(
                    """CREATE TRIGGER validate_${table}_$suffix
                       BEFORE $operation ON $table
                       WHEN ${invalidWhen.trim()}
                       BEGIN SELECT RAISE(ABORT, 'Invalid $table row'); END"""
                )
            }
        }

        // A field may change type only once its old values are gone, or they would be misread.
        db.execSQL("DROP TRIGGER IF EXISTS guard_field_type_change")
        db.execSQL(
            """CREATE TRIGGER guard_field_type_change
               BEFORE UPDATE OF type ON tracker_fields
               WHEN OLD.type != NEW.type
                 AND EXISTS (SELECT 1 FROM entry_values WHERE field_id = NEW.id)
               BEGIN SELECT RAISE(ABORT, 'Field type changed while values exist'); END"""
        )

        // An entry never moves between trackers: its values point at the old tracker's fields.
        db.execSQL("DROP TRIGGER IF EXISTS guard_entry_tracker_change")
        db.execSQL(
            """CREATE TRIGGER guard_entry_tracker_change
               BEFORE UPDATE OF tracker_id ON entries
               WHEN OLD.tracker_id != NEW.tracker_id
               BEGIN SELECT RAISE(ABORT, 'Entry moved to another tracker'); END"""
        )
    }
}
