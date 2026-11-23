package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
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
        ReminderEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        RecurringEntity::class,
        CategoryBudgetEntity::class,
        CategoryEntity::class,
        PartyEntity::class,
        PiggyEntity::class,
        PiggyMoveEntity::class,
    ],
    version = TrackerDatabase.VERSION,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
    abstract fun entryDao(): EntryDao
    abstract fun backupDao(): BackupDao
    abstract fun reminderDao(): ReminderDao
    abstract fun financeDao(): FinanceDao

    companion object {
        const val VERSION = 20
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
    /** 2.0.9: reminders. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `tracker_id` TEXT NOT NULL,
                   `minute_of_day` INTEGER NOT NULL, `days_mask` INTEGER NOT NULL, `enabled` INTEGER NOT NULL,
                   `skip_if_logged` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`),
                   FOREIGN KEY(`tracker_id`) REFERENCES `trackers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_tracker_id` ON `reminders` (`tracker_id`)")
        }
    }

    /** 2.0.9: tracker tags. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `tags` TEXT NOT NULL DEFAULT '[]'")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_accounts (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, currency TEXT NOT NULL, opening_minor INTEGER NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_transactions (
                    id TEXT NOT NULL PRIMARY KEY, account_id TEXT NOT NULL, kind TEXT NOT NULL, minor INTEGER NOT NULL,
                    epoch_day INTEGER NOT NULL, note TEXT NOT NULL, destination_id TEXT,
                    FOREIGN KEY(account_id) REFERENCES finance_accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(destination_id) REFERENCES finance_accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_transactions_account_id ON finance_transactions (account_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_transactions_destination_id ON finance_transactions (destination_id)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_accounts ADD COLUMN monthly_budget_minor INTEGER")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_recurring (
                    id TEXT NOT NULL PRIMARY KEY, account_id TEXT NOT NULL, minor INTEGER NOT NULL,
                    note TEXT NOT NULL, interval TEXT NOT NULL, anchor_day INTEGER NOT NULL, next_epoch_day INTEGER,
                    FOREIGN KEY(account_id) REFERENCES finance_accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_recurring_account_id ON finance_recurring (account_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_recurring_next_epoch_day ON finance_recurring (next_epoch_day)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN sms_key TEXT")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_finance_transactions_sms_key ON finance_transactions (sms_key)")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN kind TEXT NOT NULL DEFAULT 'EXPENSE'")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN every INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN paused INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN variable INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_budgets (
                    id TEXT NOT NULL PRIMARY KEY, account_id TEXT NOT NULL, category TEXT NOT NULL, minor INTEGER NOT NULL,
                    interval TEXT NOT NULL, every INTEGER NOT NULL, start_epoch_day INTEGER NOT NULL,
                    FOREIGN KEY(account_id) REFERENCES finance_accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_budgets_account_id ON finance_budgets (account_id)")
        }
    }

    /** Subscriptions keep when they began, notes, and an icon. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN start_epoch_day INTEGER")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN details TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN icon TEXT")
            // Older payments never recorded when they began; left empty rather than guessed.
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_budgets ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE finance_budgets ADD COLUMN icon TEXT")
            db.execSQL("ALTER TABLE finance_budgets ADD COLUMN color INTEGER")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN color INTEGER")
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN next_minute INTEGER")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS finance_categories (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, name_key TEXT NOT NULL,
                    kind TEXT NOT NULL, icon TEXT, color INTEGER
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_finance_categories_name_key ON finance_categories (name_key)")
        }
    }

    /** Transactions get notes apart from their name, and a save time to order a day by. */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN details TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
            // The row id follows insertion, so older entries keep the order they were added in.
            db.execSQL("UPDATE finance_transactions SET created_at = rowid")
        }
    }

    /** A category budget can count every account in its currency. */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_budgets ADD COLUMN all_accounts INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** A variable payment can post its estimate when due. */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_recurring ADD COLUMN auto_estimate INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** A deleted category stays as a marker, so it is not offered again. */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_categories ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** A tracker has a schedule: weekdays it is due (none: as needed), or days a week; every day until set. */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE trackers ADD COLUMN schedule_days INTEGER NOT NULL DEFAULT 127")
            db.execSQL("ALTER TABLE trackers ADD COLUMN times_per_week INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** A tracker can be due again after a time or an amount since it was last logged. */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE trackers ADD COLUMN intervals TEXT NOT NULL DEFAULT '[]'")
        }
    }

    /** Accounts say what they are, and the last digits payment messages name. */
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_accounts ADD COLUMN kind TEXT NOT NULL DEFAULT 'bank'")
            db.execSQL("ALTER TABLE finance_accounts ADD COLUMN digits TEXT")
        }
    }

    /** People and businesses for transactions, and transactions not cleared yet. */
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS finance_parties (id TEXT NOT NULL, name TEXT NOT NULL, name_key TEXT NOT NULL, " +
                    "kind TEXT NOT NULL DEFAULT 'person', created_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_finance_parties_name_key ON finance_parties (name_key)")
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN party_id TEXT")
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN pending INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_transactions_party_id ON finance_transactions (party_id)")
        }
    }

    /** Shares with people on transactions: lent, borrowed, split and paid back. */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_transactions ADD COLUMN shares TEXT NOT NULL DEFAULT '[]'")
        }
    }

    /** The least to keep in each account. */
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE finance_accounts ADD COLUMN floor_minor INTEGER")
        }
    }

    /** Piggy banks: money set aside inside an account, and what goes in and out. */
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS finance_piggies (id TEXT NOT NULL, account_id TEXT NOT NULL, name TEXT NOT NULL, " +
                    "target_minor INTEGER NOT NULL, target_epoch_day INTEGER, created_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id), " +
                    "FOREIGN KEY(account_id) REFERENCES finance_accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_piggies_account_id ON finance_piggies (account_id)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS finance_piggy_moves (id TEXT NOT NULL, piggy_id TEXT NOT NULL, minor INTEGER NOT NULL, " +
                    "epoch_day INTEGER NOT NULL, created_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id), " +
                    "FOREIGN KEY(piggy_id) REFERENCES finance_piggies(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_finance_piggy_moves_piggy_id ON finance_piggy_moves (piggy_id)")
        }
    }

    /** Fixed deposits: an account's terms, which stay null on every other account. */
    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "deposit_principal_minor INTEGER", "deposit_rate_bp INTEGER", "deposit_start_epoch_day INTEGER",
                "deposit_maturity_epoch_day INTEGER", "deposit_compounding TEXT", "deposit_closed_epoch_day INTEGER",
            ).forEach { db.execSQL("ALTER TABLE finance_accounts ADD COLUMN $it") }
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
        MIGRATION_19_20,
    )
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
            OR NEW.schedule_days < 0 OR NEW.schedule_days > 127
            OR NEW.times_per_week < 0 OR NEW.times_per_week > ${Schedule.MAX_TIMES_PER_WEEK}
        """,
        "tracker_fields" to """
            length(trim(NEW.name)) = 0
            OR NEW.type NOT IN ($TYPES)
            OR length(NEW.config_json) = 0
            OR NEW.required NOT IN (0, 1)
        """,
        "reminders" to """
            NEW.minute_of_day < 0 OR NEW.minute_of_day >= 1440
            OR NEW.days_mask < 1 OR NEW.days_mask > 127
            OR NEW.enabled NOT IN (0, 1)
            OR NEW.skip_if_logged NOT IN (0, 1)
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
