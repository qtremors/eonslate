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
}
