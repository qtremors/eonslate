package dev.qtremors.eonslate.core.tracker.data

import dev.qtremors.eonslate.core.tracker.data.repository.BackupManager
import dev.qtremors.eonslate.core.tracker.data.repository.RoomBackupRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomFinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import dev.qtremors.eonslate.core.tracker.domain.backup.PreferenceStore
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class PreferencesAndIconsBackupTest {
    @get:Rule val folder = TemporaryFolder()

    private class MemoryPreferences(var values: Map<String, String>) : PreferenceStore {
        override suspend fun export() = values
        override suspend fun replace(values: Map<String, String>) {
            this.values = values
        }
    }

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)
    private val bank = Account("bank", "Bank", "INR")

    @Test
    fun preferencesTravelInABackupAndClearingResetsThemWithUndo() = runTest {
        val db = inMemoryDatabase()
        try {
            val prefs = MemoryPreferences(mapOf("theme" to "DARK", "display_name" to "Sam"))
            val manager = BackupManager(RoomBackupRepository(db, FakeClock()), folder.newFolder(), Dispatchers.Unconfined, iterations = 10_000, preferences = prefs)
            val saved = manager.open(manager.create(null, "2.2.5"), null)
            assertEquals(mapOf("theme" to "DARK", "display_name" to "Sam"), saved.preferences)

            manager.restore(BackupSnapshot(schema = 1, createdAt = 0, appVersion = "2.2.5", preferences = emptyMap()), "2.2.5")
            assertEquals(emptyMap<String, String>(), prefs.values)
            manager.undoRestore()
            assertEquals("Sam", prefs.values["display_name"])

            // A file without preferences leaves them as they are.
            manager.restore(BackupSnapshot(schema = 1, createdAt = 0, appVersion = "2.2.5"), "2.2.5")
            assertEquals("Sam", prefs.values["display_name"])
        } finally {
            db.close()
        }
    }

    @Test
    fun onlyRealImagesAreRestoredAndUnusedIconFilesGo() = runTest {
        val db = inMemoryDatabase()
        try {
            val icons = folder.newFolder("icons")
            val good = "a".repeat(64)
            val bad = "b".repeat(64)
            val stale = File(icons, "c".repeat(64) + ".png").apply { writeBytes(png) }
            val manager = BackupManager(RoomBackupRepository(db, FakeClock()), folder.newFolder(), Dispatchers.Unconfined, iterations = 10_000, icons = icons)
            val payment = RecurringPayment("p", bank.id, 100, "Music", PaymentInterval.MONTHLY, 1, 20_000, icon = "file:$good")
            val snapshot = BackupSnapshot(
                schema = 1, createdAt = 0, appVersion = "2.2.5",
                accounts = listOf(bank), recurring = listOf(payment),
                icons = mapOf(good to Base64.getEncoder().encodeToString(png), bad to Base64.getEncoder().encodeToString("not an image".toByteArray())),
            )
            manager.restore(snapshot, "2.2.5")
            assertTrue(File(icons, "$good.png").isFile)
            assertFalse(File(icons, "$bad.png").exists())
            assertFalse(stale.exists())
        } finally {
            db.close()
        }
    }

    @Test
    fun aDeletedCategoryLeavesHistoryAloneAndIsNoLongerOffered() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            finance.saveTransaction(Transaction("t", bank.id, TransactionKind.EXPENSE, 100, 20_000, note = "Bus", category = "Transport"))
            val transport = Categories.all(finance.observeLedger().first()).single { it.key == "transport" }
            assertTrue(finance.deleteCategory(transport))

            val ledger = finance.observeLedger().first()
            assertEquals("Transport", ledger.transactions.single().category)
            assertTrue(Categories.all(ledger).none { it.key == "transport" })
            assertTrue(Categories.suggestions(ledger, TransactionKind.EXPENSE).none { it.equals("Transport", ignoreCase = true) })
            assertTrue(Categories.all(ledger).isEmpty())

            // The same name can be added again later.
            assertTrue(finance.saveCategory(transport.copy(id = "category:new-transport"), null))
            assertTrue(Categories.all(finance.observeLedger().first()).any { it.key == "transport" })
        } finally {
            db.close()
        }
    }

    @Test
    fun aNewLedgerOffersNoCategoriesAndTheLastOneCanGo() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            assertTrue(Categories.all(finance.observeLedger().first()).isEmpty())
            val coffee = dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice("Coffee", dev.qtremors.eonslate.core.tracker.domain.finance.CategoryKind.EXPENSE)
            assertTrue(finance.saveCategory(coffee, null))
            assertTrue(finance.deleteCategory(Categories.all(finance.observeLedger().first()).single()))
            assertTrue(Categories.all(finance.observeLedger().first()).isEmpty())
        } finally {
            db.close()
        }
    }
}
