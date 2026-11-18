package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.csv.CsvException
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvProblem
import dev.qtremors.eonslate.core.tracker.domain.csv.FinanceCsv
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerCsv
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
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
