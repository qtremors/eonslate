package dev.qtremors.eonslate.core.tracker.data

import dev.qtremors.eonslate.core.tracker.data.repository.BackupManager
import dev.qtremors.eonslate.core.tracker.data.repository.RoomBackupRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomFinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
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
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class RoomFinanceRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val bank = Account("bank", "Bank", "INR", openingMinor = 10_000)
    private val cash = Account("cash", "Cash", "INR", openingMinor = 500)
    private val yen = Account("yen", "Yen", "JPY")
    private val transfer = Transaction("transfer", bank.id, TransactionKind.TRANSFER, 2_525, 20_000, destinationId = cash.id)

    @Test
    fun aTransferAndItsEditsAffectBothBalances() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            assertTrue(finance.saveAccount(bank))
            assertTrue(finance.saveAccount(cash))
            assertTrue(finance.saveAccount(yen))
            assertFalse(finance.saveTransaction(transfer.copy(destinationId = yen.id)))
            assertFalse(finance.saveTransaction(transfer.copy(destinationId = bank.id)))
            assertFalse(finance.saveTransaction(transfer.copy(minor = 0)))
            assertTrue(finance.saveTransaction(transfer))
            assertFalse(finance.deleteAccount(cash.id))
            assertFalse(finance.saveAccount(bank.copy(currency = "USD")))
            val balances = finance.observeLedger().first().balances()
            assertEquals(BigInteger.valueOf(7_475), balances[bank.id])
            assertEquals(BigInteger.valueOf(3_025), balances[cash.id])

            assertTrue(finance.saveTransaction(transfer.copy(minor = 700)))
            val updated = finance.observeLedger().first()
            assertEquals(1, updated.transactions.size)
            assertEquals(BigInteger.valueOf(9_300), updated.balances()[bank.id])
            assertEquals(BigInteger.valueOf(1_200), updated.balances()[cash.id])
            finance.deleteTransaction(transfer.id)
            assertTrue(finance.deleteAccount(cash.id))
        } finally {
            db.close()
        }
    }

    @Test
    fun financeSurvivesBackupRestoreAndUndo() = runTest {
        val source = inMemoryDatabase()
        val target = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(source)
            finance.saveAccount(bank.copy(monthlyBudgetMinor = 8_000))
            finance.saveAccount(cash)
            finance.saveTransaction(transfer)
            finance.addRecurring(RecurringPayment("p", bank.id, 100, "Rent", PaymentInterval.MONTHLY, 1, 20_000), 19_999)
            val old = RoomFinanceRepository(target)
            old.saveAccount(yen)
            val clock = FakeClock()
            val backups = BackupManager(RoomBackupRepository(source, clock), folder.newFolder(), Dispatchers.Unconfined)
            val restore = BackupManager(RoomBackupRepository(target, clock), folder.newFolder(), Dispatchers.Unconfined)
            val bytes = backups.create(null, "2.1.2")
            restore.restore(restore.open(bytes, null), "2.1.2")
            assertEquals(finance.observeLedger().first(), old.observeLedger().first())
            assertEquals(DataCounts(0, 0, 2, 1), restore.counts())
            restore.undoRestore()
            assertEquals(listOf(yen), old.observeLedger().first().accounts)
            assertTrue(old.observeLedger().first().transactions.isEmpty())
        } finally {
            target.close()
            source.close()
        }
    }

    @Test
    fun missedPaymentsPostOnceAndCountTowardTheBudget() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank.copy(monthlyBudgetMinor = 8_000))
            finance.saveAccount(cash)
            val january = LocalDate.of(2026, 1, 31).toEpochDay()
            val march = LocalDate.of(2026, 3, 31).toEpochDay()
            assertTrue(finance.addRecurring(RecurringPayment("p", bank.id, 100, "", PaymentInterval.MONTHLY, 31, january), january))
            finance.postDue(march)
            finance.postDue(march)
            finance.saveTransaction(transfer.copy(epochDay = march))
            val ledger = finance.observeLedger().first()
            assertEquals(4, ledger.transactions.size)
            assertEquals(LocalDate.of(2026, 4, 30).toEpochDay(), ledger.recurring.single().nextEpochDay)
            assertEquals(BigInteger.valueOf(100), ledger.monthlySpending(YearMonth.of(2026, 3))[bank.id])
            finance.deleteRecurring("p")
            assertEquals(4, finance.observeLedger().first().transactions.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun variablePaymentsWaitAndCategoryBudgetsUseTheirOwnCycle() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            val january = LocalDate.of(2026, 1, 31)
            val march = LocalDate.of(2026, 3, 31)
            val payment = RecurringPayment(
                "variable", bank.id, 100, "Utility", PaymentInterval.MONTHLY, 31, january.toEpochDay(),
                every = 2, variable = true, category = "Utilities",
            )
            val budget = CategoryBudget("budget", bank.id, "Utilities", 500, PaymentInterval.MONTHLY, 2, january.toEpochDay())
            assertTrue(finance.saveBudget(budget))
            assertTrue(finance.addRecurring(payment, march.toEpochDay()))
            finance.postDue(march.toEpochDay())
            assertTrue(finance.observeLedger().first().transactions.isEmpty())

            assertTrue(finance.recordRecurring(payment, 425, january.plusDays(1).toEpochDay()))
            assertFalse(finance.recordRecurring(payment, 425, january.plusDays(1).toEpochDay()))
            val ledger = finance.observeLedger().first()
            assertEquals(march.toEpochDay(), ledger.recurring.single().nextEpochDay)
            assertEquals(BigInteger.valueOf(425), ledger.spendingFor(budget, march.minusDays(1)))
            assertEquals(BigInteger.ZERO, ledger.spendingFor(budget, march))
        } finally {
            db.close()
        }
    }

    @Test
    fun aPaymentMessageCanOnlyBeSavedOnce() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            val transaction = Transaction("sms", bank.id, TransactionKind.EXPENSE, 100, 20_000, smsKey = "a".repeat(64))
            assertTrue(finance.saveTransaction(transaction))
            assertFalse(finance.saveTransaction(transaction.copy(id = "duplicate")))
            assertTrue(finance.saveTransaction(transaction.copy(note = "Corrected")))
            assertEquals("Corrected", finance.observeLedger().first().transactions.single().note)
        } finally {
            db.close()
        }
    }
}
