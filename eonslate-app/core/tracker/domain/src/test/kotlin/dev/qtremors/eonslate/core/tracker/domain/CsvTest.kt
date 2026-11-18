package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.csv.CsvException
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvProblem
import dev.qtremors.eonslate.core.tracker.domain.csv.FinanceCsv
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerCsv
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.StringWriter

class CsvTest {
    @Test
    fun trackerRoundTripKeepsEveryValueAndQuotedNotes() {
        val tracker = everyTypeTracker()
        val original = entry(validValues(), note = "=Note, \"quoted\"\nA second line")
        val file = StringWriter()
        TrackerCsv.write(tracker, listOf(original), file)
        val result = TrackerCsv.read("\uFEFF$file", tracker, NOW) { "new" }.single()
        assertEquals(original.copy(id = "new"), result)
    }

    @Test
    fun anUnclosedQuotedRecordReportsItsRow() {
        val tracker = everyTypeTracker()
        val file = StringWriter()
        TrackerCsv.write(tracker, emptyList(), file)
        val failure = assertThrows(CsvException::class.java) {
            TrackerCsv.read(file.toString() + "\"broken", tracker, NOW) { "new" }
        }
        assertEquals(CsvProblem.FORMAT, failure.problem)
        assertEquals(2, failure.row)
    }

    @Test
    fun financeRoundTripResolvesTransfersAndExactAmounts() {
        val bank = Account("bank", "Cash", "INR")
        val cash = Account("cash", "Cash", "INR")
        val entry = Transaction("x", bank.id, TransactionKind.TRANSFER, 2_525, 20_000, destinationId = cash.id)
        val ledger = Ledger(listOf(bank, cash), listOf(entry))
        val file = StringWriter()
        FinanceCsv.write(ledger, file)
        assertEquals(entry.copy(id = "new"), FinanceCsv.read(file.toString(), ledger.accounts, bank) { "new" }.single())
    }

    @Test
    fun financeExportWritesOnlyTheMatchesInTheSelectedOrder() {
        val bank = Account("bank", "Bank", "INR")
        val lunch = Transaction("lunch", bank.id, TransactionKind.EXPENSE, 500, 20_001, note = "Cafe lunch", category = "Food")
        val dinner = lunch.copy(id = "dinner", minor = 900, epochDay = 20_002, note = "Cafe dinner")
        val ledger = Ledger(
            accounts = listOf(bank),
            transactions = listOf(
                lunch,
                lunch.copy(id = "outside", epochDay = 19_999),
                lunch.copy(id = "other-category", category = "Transport"),
                lunch.copy(id = "other-note", note = "Groceries"),
                dinner,
            ),
        )
        val matching = ledger.filteredTransactions(20_000L..20_005L, "food", " cafe ", TransactionSort.LARGEST)
        val file = StringWriter()
        FinanceCsv.write(ledger.copy(transactions = matching), file)
        val exported = FinanceCsv.read(file.toString(), ledger.accounts, bank) { "new" }
        assertEquals(listOf("Cafe dinner", "Cafe lunch"), exported.map(Transaction::note))
    }

    @Test
    fun aCsvWithNoAccountUsesTheSelectedCurrency() {
        val account = Account("cash", "Cash", "INR")
        val source = "Date,Description,Category,Type,Amount,Notes\r\n2026-11-11,Coffee,Food,expense,120.50,\r\n"
        val entry = FinanceCsv.read(source, listOf(account), account) { "new" }.single()
        assertEquals(12_050L, entry.minor)
        assertEquals("Coffee", entry.note)
        assertEquals("Food", entry.category)
        assertEquals(account.id, entry.accountId)
    }
}
