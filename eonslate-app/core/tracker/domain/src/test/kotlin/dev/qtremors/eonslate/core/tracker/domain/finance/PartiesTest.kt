package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.backup.BackupCheck
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import dev.qtremors.eonslate.core.tracker.domain.csv.FinanceCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter

class PartiesTest {
    private val bank = Account("bank", "HDFC", "INR")
    private val rahul = Party("p1", "Rahul Sharma")
    private var n = 0
    private val ids = dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator { "new${n++}" }

    @Test
    fun aBookFindsNamesWhateverTheCaseAndMakesNewOnesOnce() {
        val book = PartyBook(listOf(rahul), ids)
        assertEquals("p1", book.idFor("  rahul   SHARMA "))
        val swiggy = book.idFor("Swiggy")
        assertEquals(swiggy, book.idFor("SWIGGY"))
        assertEquals(null, book.idFor("  "))
        assertEquals(listOf("Swiggy"), book.created.map { it.name })
        assertEquals(PartyKind.BUSINESS, book.created.single().kind)
        assertEquals(PartyKind.PERSON, PartyKind.guess("Asha Verma"))
        assertEquals(PartyKind.BUSINESS, PartyKind.guess("Reliance Fresh Pvt Ltd"))
    }

    @Test
    fun aPaymentMessageNamesItsPayeeAsAParty() {
        val message = PaymentMessage(TransactionKind.EXPENSE, "INR", 25_000, merchant = "Zomato", reference = "123")
        val book = PartyBook(emptyList(), ids)
        val t = message.asTransaction("t", listOf(bank), 0, book)!!
        assertEquals(book.created.single().id, t.partyId)
        assertEquals("Ref 123", t.note)
        // Without a book the payee stays in the note, as before.
        assertEquals("Zomato · Ref 123", message.asTransaction("t", listOf(bank), 0)!!.note)
    }

    @Test
    fun searchAndFiltersReachTheParty() {
        val ledger = Ledger(
            accounts = listOf(bank),
            parties = listOf(rahul),
            transactions = listOf(
                Transaction("a", "bank", TransactionKind.EXPENSE, 100, 1, partyId = "p1"),
                Transaction("b", "bank", TransactionKind.EXPENSE, 100, 1, pending = true),
            ),
        )
        assertEquals(listOf("a"), ledger.filteredTransactions(TransactionFilter(query = "rahul")).map { it.id })
        assertEquals(listOf("a"), ledger.filteredTransactions(TransactionFilter(partyId = "p1")).map { it.id })
        assertEquals(listOf("b"), ledger.filteredTransactions(TransactionFilter(pendingOnly = true)).map { it.id })
    }

    @Test
    fun csvCarriesThePartyAndPending() {
        val ledger = Ledger(
            accounts = listOf(bank),
            parties = listOf(rahul),
            transactions = listOf(Transaction("a", "bank", TransactionKind.EXPENSE, 100, 20_000, partyId = "p1", pending = true)),
        )
        val out = StringWriter()
        FinanceCsv.write(ledger, out)
        val book = PartyBook(emptyList(), ids)
        val read = FinanceCsv.read(out.toString(), listOf(bank), bank, book, ids).single()
        assertEquals("Rahul Sharma", book.created.single().name)
        assertEquals(book.created.single().id, read.partyId)
        assertTrue(read.pending)
    }

    @Test
    fun aBackupMustHoldEveryPartyItsTransactionsName() {
        val snapshot = BackupSnapshot(
            schema = 16, createdAt = 0, appVersion = "t",
            accounts = listOf(bank),
            transactions = listOf(Transaction("a", "bank", TransactionKind.EXPENSE, 100, 1, partyId = "ghost")),
            parties = listOf(rahul, rahul.copy(id = "p2", name = "RAHUL sharma")),
        )
        val problems = BackupCheck.problems(snapshot, currentSchema = 16)
        assertTrue(problems.toString(), problems.any { "missing party" in it })
        assertTrue(problems.toString(), problems.any { "two parties" in it })
    }

    @Test
    fun whatYouPayACompanyAddsUpAcrossItsCategories() {
        val apple = Party("apple", "Apple", PartyKind.BUSINESS)
        val today = java.time.LocalDate.of(2026, 11, 24)
        val day = { m: Int, d: Int -> java.time.LocalDate.of(2026, m, d).toEpochDay() }
        val usd = Account("usd", "Wise", "USD")
        val ledger = Ledger(
            accounts = listOf(bank, usd),
            parties = listOf(apple, rahul),
            transactions = listOf(
                Transaction("m1", "bank", TransactionKind.EXPENSE, 11_900, day(11, 3), "Apple Music", category = "Music", partyId = "apple"),
                Transaction("m2", "bank", TransactionKind.EXPENSE, 11_900, day(10, 3), "Apple Music", category = "music", partyId = "apple"),
                Transaction("i1", "bank", TransactionKind.EXPENSE, 7_500, day(11, 10), "iCloud", category = "Cloud", partyId = "apple"),
                Transaction("old", "bank", TransactionKind.EXPENSE, 50_000, java.time.LocalDate.of(2025, 6, 1).toEpochDay(), partyId = "apple"),
                Transaction("refund", "bank", TransactionKind.INCOME, 2_000, day(11, 12), partyId = "apple"),
                Transaction("dollars", "usd", TransactionKind.EXPENSE, 999, day(11, 1), partyId = "apple"),
                Transaction("chai", "bank", TransactionKind.EXPENSE, 2_000, day(11, 20), partyId = "p1"),
            ),
        )
        val (inr, dollars) = ledger.totalsWith("apple", today)
        assertEquals("INR", inr.currency)
        assertEquals(java.math.BigInteger.valueOf(19_400), inr.paidThisMonth)
        assertEquals(java.math.BigInteger.valueOf(31_300), inr.paidThisYear)
        assertEquals(java.math.BigInteger.valueOf(81_300), inr.paidInAll)
        assertEquals(java.math.BigInteger.valueOf(2_000), inr.receivedInAll)
        // Music in either case is one category; money paid with none is its own line.
        assertEquals(
            listOf("" to 50_000L, "Music" to 23_800L, "Cloud" to 7_500L),
            inr.byCategory.map { it.first to it.second.toLong() },
        )
        assertEquals("USD", dollars.currency)
        // A person paid for a purchase counts like a shop.
        assertEquals(java.math.BigInteger.valueOf(2_000), ledger.paidThisYear(today)["p1"]?.get("INR"))
        assertEquals(java.math.BigInteger.valueOf(31_300), ledger.paidThisYear(today)["apple"]?.get("INR"))
    }

    @Test
    fun aRecurringPaymentPostsWithItsPayee() {
        val music = RecurringPayment("r", "bank", 11_900, "Apple Music", PaymentInterval.MONTHLY, 3, 20_000, partyId = "apple")
        assertEquals("apple", music.transactionOn(20_000).partyId)
        val snapshot = BackupSnapshot(schema = 21, createdAt = 0, appVersion = "t", accounts = listOf(bank), recurring = listOf(music))
        assertTrue(BackupCheck.problems(snapshot, currentSchema = 21).any { "recurring r names a missing party" in it })
    }
}
