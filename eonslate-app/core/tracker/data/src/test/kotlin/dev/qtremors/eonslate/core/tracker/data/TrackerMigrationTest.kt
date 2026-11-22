package dev.qtremors.eonslate.core.tracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase.Companion.configure
import dev.qtremors.eonslate.core.tracker.data.db.TrackerIntegrity
import dev.qtremors.eonslate.core.tracker.data.db.TrackerMigrations
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomFinanceRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Opens a database written by every exported schema with the current app, the way an upgrade
 * does. Old databases are built from the exported JSON by [ExportedSchema]; each new schema
 * version adds a test that writes data under the old one and reads it back here.
 */
@RunWith(RobolectricTestRunner::class)
class TrackerMigrationTest {
    private val name = "migration-test.db"
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun everyExportedSchemaIsPresent() {
        assertEquals((1..TrackerDatabase.VERSION).toList(), ExportedSchema.versions())
    }

    @Test
    fun everyVersionHasAMigrationToTheNext() {
        val steps = TrackerMigrations.ALL.map { it.startVersion to it.endVersion }
        assertEquals((1 until TrackerDatabase.VERSION).map { it to it + 1 }, steps)
    }

    @Test
    fun version16TransactionsShareWithNobody() = runTest {
        ExportedSchema.create(context, name, 16).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor, kind) VALUES ('bank', 'Bank', 'INR', 0, 'bank')")
            it.execSQL(
                "INSERT INTO finance_transactions (id, account_id, kind, minor, epoch_day, note, category, details, created_at, pending) " +
                    "VALUES ('t', 'bank', 'EXPENSE', 500, 20000, 'Lunch', '', '', 1, 0)"
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().allowMainThreadQueries().build()
        try {
            assertTrue(RoomFinanceRepository(db).observeLedger().first().transactions.single().shares.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version15TransactionsNameNoPartyAndAreCleared() = runTest {
        ExportedSchema.create(context, name, 15).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor, kind) VALUES ('bank', 'Bank', 'INR', 0, 'bank')")
            it.execSQL(
                "INSERT INTO finance_transactions (id, account_id, kind, minor, epoch_day, note, category, details, created_at) " +
                    "VALUES ('t', 'bank', 'EXPENSE', 500, 20000, 'Lunch', '', '', 1)"
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().allowMainThreadQueries().build()
        try {
            val ledger = RoomFinanceRepository(db).observeLedger().first()
            assertEquals(null, ledger.transactions.single().partyId)
            assertEquals(false, ledger.transactions.single().pending)
            assertTrue(ledger.parties.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version14AccountsAreBanksWithoutDigits() = runTest {
        ExportedSchema.create(context, name, 14).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('hdfc', 'HDFC 4821', 'INR', 0)")
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().allowMainThreadQueries().build()
        try {
            val account = RoomFinanceRepository(db).observeLedger().first().accounts.single()
            assertEquals(dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind.BANK, account.kind)
            assertEquals(null, account.digits)
        } finally {
            db.close()
        }
    }

    @Test
    fun version13TrackersHaveNoIntervals() = runTest {
        ExportedSchema.create(context, name, 13).use {
            it.execSQL(
                "INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, template_id, created_at, updated_at, tags, schedule_days, times_per_week) " +
                    "VALUES ('t13', 'Haircut', '', 'Star', 2, 0, 0, NULL, 100, 100, '[]', 0, 0)"
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().allowMainThreadQueries().build()
        try {
            val tracker = RoomTrackerRepository(db, FakeClock()).getTracker("t13")!!
            assertEquals(Schedule.AS_NEEDED, tracker.schedule)
            assertEquals(emptyList<Any>(), tracker.intervals)
        } finally {
            db.close()
        }
    }

    @Test
    fun version12TrackersAreDueEveryDay() = runTest {
        ExportedSchema.create(context, name, 12).use {
            it.execSQL(
                "INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, template_id, created_at, updated_at, tags) " +
                    "VALUES ('t12', 'Gym', '', 'FitnessCenter', 8, 0, 0, NULL, 100, 100, '[]')"
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().allowMainThreadQueries().build()
        try {
            val tracker = RoomTrackerRepository(db, FakeClock()).getTracker("t12")!!
            assertEquals(Schedule.DAILY, tracker.schedule)
        } finally {
            db.close()
        }
    }

    @Test
    fun version11CategoriesStayOffered() = runTest {
        ExportedSchema.create(context, name, 11).use {
            it.execSQL("INSERT INTO finance_categories (id, name, name_key, kind) VALUES ('category:pets', 'Pets', 'pets', 'EXPENSE')")
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().build()
        try {
            val category = RoomFinanceRepository(db).observeLedger().first().categories.single()
            assertEquals("Pets", category.name)
            assertEquals(false, category.deleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun version10VariablePaymentsKeepWaitingForAnAmount() = runTest {
        ExportedSchema.create(context, name, 10).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('cash', 'Cash', 'INR', 0)")
            it.execSQL(
                """INSERT INTO finance_recurring (id, account_id, minor, note, interval, anchor_day, next_epoch_day, kind, every, paused, variable, category, details)
                   VALUES ('power', 'cash', 4000, 'Power', 'MONTHLY', 5, 20000, 'EXPENSE', 1, 0, 1, 'Bills', '')"""
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().build()
        try {
            val finance = RoomFinanceRepository(db)
            assertEquals(false, finance.observeLedger().first().recurring.single().autoEstimate)
            finance.postDue(20_100)
            assertTrue(finance.observeLedger().first().transactions.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version9BudgetsStayWithTheirOwnAccount() = runTest {
        ExportedSchema.create(context, name, 9).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('cash', 'Cash', 'INR', 0)")
            it.execSQL(
                """INSERT INTO finance_budgets (id, account_id, category, minor, interval, every, start_epoch_day, name)
                   VALUES ('food', 'cash', 'Food', 8500, 'MONTHLY', 1, 20000, '')"""
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().build()
        try {
            val finance = RoomFinanceRepository(db)
            val budget = finance.observeLedger().first().budgets.single()
            assertEquals(false, budget.allAccounts)
            assertTrue(finance.saveBudget(budget.copy(allAccounts = true)))
            assertEquals(true, finance.observeLedger().first().budgets.single().allAccounts)
        } finally {
            db.close()
        }
    }

    @Test
    fun version8TransactionsKeepTheirOrderAndStartWithoutNotes() = runTest {
        ExportedSchema.create(context, name, 8).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('cash', 'Cash', 'INR', 0)")
            listOf("b-first", "a-second").forEach { id ->
                it.execSQL(
                    """INSERT INTO finance_transactions (id, account_id, kind, minor, epoch_day, note, category)
                       VALUES ('$id', 'cash', 'EXPENSE', 100, 20000, 'Tea · with Sam', 'Food')"""
                )
            }
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().build()
        try {
            val transactions = RoomFinanceRepository(db).observeLedger().first().transactions
            // Newest first within the day, so the later insert leads despite its id.
            assertEquals(listOf("a-second", "b-first"), transactions.map { it.id })
            assertEquals("Tea · with Sam", transactions.first().note)
            assertEquals("", transactions.first().details)
        } finally {
            db.close()
        }
    }

    @Test
    fun version7BudgetsKeepTheirCategoryAndStartWithDefaultStyling() = runTest {
        ExportedSchema.create(context, name, 7).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('cash', 'Cash', 'INR', 100)")
            it.execSQL(
                """INSERT INTO finance_budgets (id, account_id, category, minor, interval, every, start_epoch_day)
                   VALUES ('food', 'cash', 'Food', 8500, 'MONTHLY', 1, 20000)"""
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).configure().build()
        try {
            val ledger = RoomFinanceRepository(db).observeLedger().first()
            val budget = ledger.budgets.single()
            assertEquals("Food", budget.title)
            assertEquals(8500L, budget.minor)
            assertEquals(null, budget.icon)
            assertEquals(null, budget.color)
            assertTrue(ledger.categories.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version5SchedulesKeepTheirExpenseDefaults() = runTest {
        ExportedSchema.create(context, name, 5).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('cash', 'Cash', 'INR', 100)")
            it.execSQL(
                """INSERT INTO finance_recurring (id, account_id, minor, note, interval, anchor_day, next_epoch_day)
                   VALUES ('rent', 'cash', 200, 'Rent', 'MONTHLY', 1, 20000)"""
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name)
            .configure()
            .allowMainThreadQueries()
            .build()
        try {
            val ledger = RoomFinanceRepository(db).observeLedger().first()
            val payment = ledger.recurring.single()
            assertEquals("EXPENSE", payment.kind.name)
            assertEquals(1, payment.every)
            assertTrue(!payment.variable && !payment.paused && ledger.budgets.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version4AccountsOpenWithoutBudgetsOrSchedules() = runTest {
        ExportedSchema.create(context, name, 4).use {
            it.execSQL("INSERT INTO finance_accounts (id, name, currency, opening_minor) VALUES ('cash', 'Cash', 'INR', 100)")
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name)
            .configure()
            .allowMainThreadQueries()
            .build()
        try {
            val ledger = RoomFinanceRepository(db).observeLedger().first()
            assertEquals("cash", ledger.accounts.single().id)
            assertEquals(null, ledger.accounts.single().monthlyBudgetMinor)
            assertTrue(ledger.recurring.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version3TrackersKeepTheirTagsAndStartWithNoAccounts() = runTest {
        ExportedSchema.create(context, name, 3).use {
            it.execSQL(
                """INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, created_at, updated_at, tags)
                   VALUES ('t3', 'Water', '', 'LocalDrink', 4, 0, 0, 100, 100, '["Health"]')"""
            )
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name)
            .configure()
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(listOf("Health"), RoomTrackerRepository(db, FakeClock()).getTracker("t3")!!.tags)
            assertTrue(RoomFinanceRepository(db).observeLedger().first().accounts.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun version2TrackersOpenWithoutTags() = runTest {
        ExportedSchema.create(context, name, 2).use {
            it.execSQL(
                "INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, template_id, created_at, updated_at) " +
                    "VALUES ('t2', 'Water', '', 'LocalDrink', 4, 0, 0, NULL, 100, 100)"
            )
        }

        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name)
            .configure()
            .allowMainThreadQueries()
            .build()
        try {
            val tracker = RoomTrackerRepository(db, FakeClock()).getTracker("t2")!!
            assertEquals("Water", tracker.name)
            assertEquals(emptyList<String>(), tracker.tags)
        } finally {
            db.close()
        }
    }

    @Test
    fun version1DataOpensWithTheCurrentApp() = runTest {
        ExportedSchema.create(context, name, 1).apply {
            execSQL(
                "INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, template_id, created_at, updated_at) " +
                    "VALUES ('t1', 'Water', '', 'LocalDrink', 4, 0, 0, NULL, 100, 100)"
            )
            execSQL(
                "INSERT INTO tracker_fields (id, tracker_id, name, type, config_json, required, position) " +
                    "VALUES ('glasses', 't1', 'Glasses', 'number', '{\"kind\":\"number\",\"unit\":null,\"decimals\":0,\"min\":null,\"max\":null}', 1, 0)"
            )
            execSQL(
                "INSERT INTO entries (id, tracker_id, occurred_at, zone_id, note, created_at, updated_at) " +
                    "VALUES ('e1', 't1', 200, 'Asia/Kolkata', '', 200, 200)"
            )
            execSQL(
                "INSERT INTO entry_values (entry_id, field_id, type, long_value, text_value) " +
                    "VALUES ('e1', 'glasses', 'number', 6, NULL)"
            )
            close()
        }

        // The real builder checks Room's identity hash, runs any migrations and installs triggers.
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name)
            .configure()
            .allowMainThreadQueries()
            .build()
        try {
            val tracker = RoomTrackerRepository(db, FakeClock()).getTracker("t1")!!
            assertEquals("Water", tracker.name)
            assertTrue(tracker.field("glasses")!!.required)
            assertEquals(FieldValue.Number(6), RoomEntryRepository(db).getEntry("e1")!!.values["glasses"])

            val triggers = mutableSetOf<String>()
            db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'trigger'").use {
                while (it.moveToNext()) triggers += it.getString(0)
            }
            assertEquals(TrackerIntegrity.TRIGGER_NAMES.toSet(), triggers)
        } finally {
            db.close()
        }
    }
}
