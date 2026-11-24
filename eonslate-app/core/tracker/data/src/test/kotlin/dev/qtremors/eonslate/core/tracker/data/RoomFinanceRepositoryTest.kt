package dev.qtremors.eonslate.core.tracker.data

import dev.qtremors.eonslate.core.tracker.data.repository.BackupManager
import dev.qtremors.eonslate.core.tracker.data.repository.RoomBackupRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomFinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Attachment
import dev.qtremors.eonslate.core.tracker.domain.finance.AttachmentKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Compounding
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposit
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposits
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryKind
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.PriceStep
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
    fun renamingACategoryUpdatesEveryReferenceAndItsAppearanceTravelsInABackup() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            assertTrue(finance.saveAccount(bank))
            assertTrue(finance.saveTransaction(Transaction("lunch", bank.id, TransactionKind.EXPENSE, 500, 20_000, category = "CAFÉ")))
            assertTrue(finance.addRecurring(RecurringPayment("meal", bank.id, 500, "Meal", PaymentInterval.WEEKLY, 1, 20_010, category = "Café"), 20_000))
            val budget = CategoryBudget("budget", bank.id, "café", 2500, PaymentInterval.WEEKLY, 1, 20_000, "Weekend", "Wallet", 0x123456)
            assertTrue(finance.saveBudget(budget))
            val original = Categories.all(finance.observeLedger().first()).single { it.key == "café" }
            val renamed = original.copy(name = "Meals", icon = "Restaurant", color = 0xFF1122, kind = CategoryKind.BOTH)
            assertTrue(finance.saveCategory(renamed, original))
            assertFalse(finance.saveCategory(CategoryChoice("MEALS", CategoryKind.BOTH, "duplicate"), null))

            val ledger = finance.observeLedger().first()
            assertEquals("Meals", ledger.transactions.single().category)
            assertEquals("Meals", ledger.recurring.single().category)
            assertEquals(budget.copy(category = "Meals"), ledger.budgets.single())
            assertTrue("Meals" in Categories.suggestions(ledger, TransactionKind.INCOME))
            val backups = BackupManager(RoomBackupRepository(db, FakeClock()), folder.root, Dispatchers.Unconfined)
            val snapshot = backups.open(backups.create(null, "2.1.8"), null)
            assertEquals(renamed, snapshot.categories.single { it.id == renamed.id })
            assertEquals(ledger.budgets, snapshot.budgets)
        } finally {
            db.close()
        }
    }

    @Test
    fun hourlyCommitmentsKeepTheirTimeAndPostAtMostOncePerDay() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            assertTrue(finance.saveAccount(bank))
            val due = RecurringPayment("coffee", bank.id, 500, "Coffee", PaymentInterval.HOURLY, 1, 20_000,
                every = 2, nextMinute = 510)
            assertTrue(finance.addRecurring(due, 20_000, 509))
            assertTrue(finance.observeLedger().first().transactions.isEmpty())
            finance.postDue(20_000, 510)
            finance.postDue(20_000, 640)
            val firstDay = finance.observeLedger().first()
            assertEquals(1, firstDay.transactions.size)
            assertEquals(750, firstDay.recurring.single().nextMinute)
            finance.postDue(20_001, 35)
            assertEquals(2, finance.observeLedger().first().transactions.size)
        } finally {
            db.close()
        }
    }

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
    fun editsKeepTheirSaveTimeAndSelectedEntriesDeleteTogether() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            assertTrue(finance.saveAccount(bank))
            val tea = Transaction("tea", bank.id, TransactionKind.EXPENSE, 100, 20_000, note = "Tea", details = "Long notes")
            assertTrue(finance.saveTransaction(tea))
            assertTrue(finance.saveTransaction(tea.copy(id = "bun", note = "Bun")))
            assertTrue(finance.saveTransaction(tea.copy(id = "jam", note = "Jam")))
            val saved = finance.observeLedger().first().transactions.single { it.id == "tea" }
            assertTrue(saved.createdAt > 0)
            assertEquals("Long notes", saved.details)

            assertTrue(finance.saveTransaction(saved.copy(minor = 150, createdAt = 0)))
            assertEquals(saved.createdAt, finance.observeLedger().first().transactions.single { it.id == "tea" }.createdAt)

            finance.deleteTransactions(listOf("tea", "bun", "missing"))
            assertEquals(listOf("jam"), finance.observeLedger().first().transactions.map { it.id })
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
            assertEquals(DataCounts(0, 0, 2, 1, subscriptions = 1), restore.counts())
            restore.undoRestore()
            assertEquals(listOf(yen), old.observeLedger().first().accounts)
            assertTrue(old.observeLedger().first().transactions.isEmpty())
        } finally {
            target.close()
            source.close()
        }
    }

    @Test
    fun estimatesPostWhenAskedAndAPaymentEnteredByHandIsNotRepeated() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            val first = LocalDate.of(2026, 1, 5).toEpochDay()
            val power = RecurringPayment("power", bank.id, 4_000, "Power", PaymentInterval.MONTHLY, 5, first, variable = true)
            assertTrue(finance.addRecurring(power, first - 1))
            assertTrue(finance.addRecurring(power.copy(id = "water", note = "Water", autoEstimate = true), first - 1))
            // February's water bill was entered by hand, in another case.
            val february = LocalDate.of(2026, 2, 5).toEpochDay()
            assertTrue(finance.saveTransaction(Transaction("manual", bank.id, TransactionKind.EXPENSE, 3_900, february, note = "WATER")))

            finance.postDue(LocalDate.of(2026, 3, 5).toEpochDay())
            val ledger = finance.observeLedger().first()
            // The estimate posts in January and March; the variable bill without it waits.
            assertEquals(listOf(3_900L, 4_000L, 4_000L), ledger.transactions.map { it.minor }.sorted())
            assertEquals(first, ledger.recurring.single { it.id == "power" }.nextEpochDay)
            assertEquals(LocalDate.of(2026, 4, 5).toEpochDay(), ledger.recurring.single { it.id == "water" }.nextEpochDay)
        } finally {
            db.close()
        }
    }

    @Test
    fun savingAScheduleCatchesUpEveryElapsedOccurrence() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            val start = LocalDate.of(2026, 1, 1).toEpochDay()
            // Far more than one posting batch of daily charges.
            assertTrue(finance.addRecurring(RecurringPayment("tea", bank.id, 10, "Tea", PaymentInterval.DAILY, 1, start), start + 299))
            val ledger = finance.observeLedger().first()
            assertEquals(300, ledger.transactions.size)
            assertEquals(start + 300, ledger.recurring.single().nextEpochDay)
        } finally {
            db.close()
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
    fun aDepositIsFundedClosedAndRemovedWithItsTransactions() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            assertTrue(finance.saveAccount(bank))
            val terms = Deposit(5_000, 700, 20_000, 20_365, Compounding.YEARLY)
            val (fd, funding) = Deposits.opening(Account("fd", "FD", "INR", kind = AccountKind.DEPOSIT, deposit = terms), bank, "fund")
            assertTrue(finance.saveDeposit(fd, funding))
            // Funding twice with the same transfer, or turning the deposit into a bank, is refused.
            assertFalse(finance.saveDeposit(fd, funding))
            assertFalse(finance.saveAccount(fd.copy(kind = AccountKind.BANK, deposit = null)))
            assertFalse(finance.saveAccount(bank.copy(id = "other", kind = AccountKind.DEPOSIT, deposit = terms)))
            var ledger = finance.observeLedger().first()
            assertEquals(terms, ledger.accounts.single { it.id == "fd" }.deposit)
            assertEquals(BigInteger.valueOf(5_000), ledger.balances()["fd"])
            assertEquals(BigInteger.valueOf(5_000), ledger.balances()["bank"])

            val status = Deposits.status(ledger, terms.maturityEpochDay).single()
            var next = 0
            val (closed, payout) = Deposits.closing(status, 5_350, bank, terms.maturityEpochDay, { "close-${next++}" }, "Interest", "Charges")!!
            assertTrue(finance.saveDeposit(closed, payout))
            ledger = finance.observeLedger().first()
            assertTrue(ledger.accounts.single { it.id == "fd" }.isClosed)
            assertEquals(BigInteger.ZERO, ledger.balances()["fd"])
            assertEquals(BigInteger.valueOf(10_350), ledger.balances()["bank"])

            // A deposit added by mistake goes with every transaction in or out of it.
            assertFalse(finance.deleteDeposit(bank.id))
            assertTrue(finance.deleteDeposit("fd"))
            ledger = finance.observeLedger().first()
            assertEquals(listOf(bank), ledger.accounts)
            assertTrue(ledger.transactions.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun aDepositAndItsTermsSurviveABackup() = runTest {
        val source = inMemoryDatabase()
        val target = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(source)
            finance.saveAccount(bank)
            val terms = Deposit(5_000, 725, 20_000, 20_730, Compounding.MONTHLY, closedEpochDay = 20_700)
            assertTrue(finance.saveDeposit(Account("fd", "FD", "INR", 5_000, kind = AccountKind.DEPOSIT, deposit = terms)))
            val clock = FakeClock()
            val backups = BackupManager(RoomBackupRepository(source, clock), folder.newFolder(), Dispatchers.Unconfined)
            val restore = BackupManager(RoomBackupRepository(target, clock), folder.newFolder(), Dispatchers.Unconfined)
            restore.restore(restore.open(backups.create(null, "2.4.8"), null), "2.4.8")
            assertEquals(terms, RoomFinanceRepository(target).observeLedger().first().accounts.single { it.id == "fd" }.deposit)
        } finally {
            target.close()
            source.close()
        }
    }

    @Test
    fun namingWhoARecurringPaymentIsWithNamesWhatItAlreadyPosted() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            val music = RecurringPayment("music", bank.id, 119, "Apple Music", PaymentInterval.MONTHLY, 1, 20_000)
            assertTrue(finance.addRecurring(music, 20_060))
            assertTrue(finance.saveTransaction(Transaction("lunch", bank.id, TransactionKind.EXPENSE, 300, 20_010)))
            // Three months fall in the 60 days it caught up on.
            assertEquals(3, finance.observeLedger().first().transactions.count { it.note == "Apple Music" })
            // Edits start from what is stored: catching up moved its next day on.
            val stored = finance.observeLedger().first().recurring.single()
            // A payee who does not exist is refused.
            assertFalse(finance.updateRecurring(stored, stored.copy(partyId = "nobody"), 20_060))
            assertTrue(finance.saveParty(dev.qtremors.eonslate.core.tracker.domain.finance.Party("apple", "Apple")))
            assertTrue(finance.updateRecurring(stored, stored.copy(partyId = "apple"), 20_060))
            var ledger = finance.observeLedger().first()
            assertTrue(ledger.transactions.filter { it.note == "Apple Music" }.all { it.partyId == "apple" })
            assertEquals(null, ledger.transactions.single { it.id == "lunch" }.partyId)
            // Deleting the payee leaves the payment and what it posted, without the name.
            assertTrue(finance.deleteParty("apple"))
            ledger = finance.observeLedger().first()
            assertEquals(null, ledger.recurring.single().partyId)
            assertTrue(ledger.transactions.all { it.partyId == null })
        } finally {
            db.close()
        }
    }

    @Test
    fun aPriceRiseChargesOnlyThePaymentsFromItsDay() = runTest {
        val db = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(db)
            finance.saveAccount(bank)
            val day = { m: Int, d: Int -> LocalDate.of(2026, m, d).toEpochDay() }
            val music = RecurringPayment("music", bank.id, 119, "Apple Music", PaymentInterval.MONTHLY, 3, day(1, 3))
            assertTrue(finance.addRecurring(music, day(3, 10)))
            suspend fun amounts() = finance.observeLedger().first().transactions.sortedBy { it.epochDay }.map { it.minor }
            assertEquals(listOf(119L, 119L, 119L), amounts())

            // Noticed late: the price went up in February, so February and March change and January stays.
            var stored = finance.observeLedger().first().recurring.single()
            assertTrue(finance.updateRecurring(stored, stored.copy(minor = 149), day(3, 10), priceFrom = day(2, 3)))
            assertEquals(listOf(119L, 149L, 149L), amounts())
            assertEquals(149L, finance.observeLedger().first().recurring.single().minor)

            // Told ahead: from May it costs 199; April still posts 149, then 199 becomes the price.
            stored = finance.observeLedger().first().recurring.single()
            assertTrue(finance.updateRecurring(stored, stored.copy(minor = 199), day(3, 10), priceFrom = day(5, 3)))
            stored = finance.observeLedger().first().recurring.single()
            assertEquals(149L, stored.minor)
            assertEquals(PriceStep(199, day(5, 3)), stored.priceChange)
            finance.postDue(day(5, 10))
            assertEquals(listOf(119L, 149L, 149L, 149L, 199L), amounts())
            stored = finance.observeLedger().first().recurring.single()
            assertEquals(199L, stored.minor)
            assertEquals(null, stored.priceChange)

            // A price cut works the same way: May drops back to 99, April keeps 149.
            assertTrue(finance.updateRecurring(stored, stored.copy(minor = 99), day(5, 10), priceFrom = day(5, 3)))
            assertEquals(listOf(119L, 149L, 149L, 149L, 99L), amounts())
            stored = finance.observeLedger().first().recurring.single()
            assertEquals(99L, stored.minor)

            // Without a day, a new amount applies from the next payment and leaves what was posted.
            assertTrue(finance.updateRecurring(stored, stored.copy(minor = 249), day(5, 10)))
            assertEquals(listOf(119L, 149L, 149L, 149L, 99L), amounts())
        } finally {
            db.close()
        }
    }

    @Test
    fun attachmentsStayWithTheirTransactionAndTravelInABackup() = runTest {
        val source = inMemoryDatabase()
        val target = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(source)
            finance.saveAccount(bank)
            assertTrue(finance.saveTransaction(Transaction("phone", bank.id, TransactionKind.EXPENSE, 50_000, 20_000)))
            val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)
            val hash = "b".repeat(64)
            val files = folder.newFolder("attachments")
            java.io.File(files, "$hash.jpg").writeBytes(jpeg)
            val receipt = Attachment("r", "phone", AttachmentKind.IMAGE, hash, "Receipt")
            val page = Attachment("l", "phone", AttachmentKind.LINK, "https://amazon.in/dp/B0C", "Product page")
            assertTrue(finance.saveAttachments("phone", listOf(receipt, page)))
            assertEquals(listOf("r", "l"), finance.observeLedger().first().attachments.map { it.id })
            // Nothing for a transaction that does not exist, nor a link that is not one.
            assertFalse(finance.saveAttachments("missing", emptyList()))
            assertFalse(finance.saveAttachments("phone", listOf(page.copy(target = "amazon"))))
            assertEquals(2, finance.observeLedger().first().attachments.size)

            val clock = FakeClock()
            val restoredFiles = folder.newFolder("restored")
            val backups = BackupManager(RoomBackupRepository(source, clock), folder.newFolder(), Dispatchers.Unconfined, attachments = files)
            val restore = BackupManager(RoomBackupRepository(target, clock), folder.newFolder(), Dispatchers.Unconfined, attachments = restoredFiles)
            restore.restore(restore.open(backups.create(null, "2.4.9"), null), "2.4.9")
            assertEquals(listOf(receipt, page), RoomFinanceRepository(target).observeLedger().first().attachments.map { it.copy(createdAt = 0) })
            assertTrue(jpeg.contentEquals(java.io.File(restoredFiles, "$hash.jpg").readBytes()))

            // Removing one keeps the other; deleting the transaction takes the rest with it.
            assertTrue(finance.saveAttachments("phone", listOf(page)))
            assertEquals(listOf("l"), finance.observeLedger().first().attachments.map { it.id })
            finance.deleteTransaction("phone")
            assertTrue(finance.observeLedger().first().attachments.isEmpty())
        } finally {
            target.close()
            source.close()
        }
    }

    @Test
    fun aBackupWritesOnlyRealFilesForAttachments() = runTest {
        val source = inMemoryDatabase()
        val target = inMemoryDatabase()
        try {
            val finance = RoomFinanceRepository(source)
            finance.saveAccount(bank)
            finance.saveTransaction(Transaction("t", bank.id, TransactionKind.EXPENSE, 100, 20_000))
            val hash = "c".repeat(64)
            val files = folder.newFolder("fake")
            // Not a PDF, whatever it is called.
            java.io.File(files, "$hash.pdf").writeBytes("#!/bin/sh".toByteArray())
            assertTrue(finance.saveAttachments("t", listOf(Attachment("p", "t", AttachmentKind.PDF, hash))))
            val clock = FakeClock()
            val restoredFiles = folder.newFolder("safe")
            val backups = BackupManager(RoomBackupRepository(source, clock), folder.newFolder(), Dispatchers.Unconfined, attachments = files)
            val restore = BackupManager(RoomBackupRepository(target, clock), folder.newFolder(), Dispatchers.Unconfined, attachments = restoredFiles)
            restore.restore(restore.open(backups.create(null, "2.4.9"), null), "2.4.9")
            assertFalse(java.io.File(restoredFiles, "$hash.pdf").exists())
        } finally {
            target.close()
            source.close()
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
