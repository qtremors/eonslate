package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesTest {
    private fun tx(id: String, kind: TransactionKind, category: String) = Transaction(id, "a", kind, 100, 0, category = category)

    @Test
    fun anEmptyLedgerOffersNothingUntilPeopleMakeTheirOwn() {
        assertEquals(emptyList<String>(), Categories.suggestions(Ledger(), TransactionKind.EXPENSE))
        assertEquals(emptyList<String>(), Categories.suggestions(Ledger(), TransactionKind.INCOME))
        assertEquals(emptyList<String>(), Categories.suggestions(Ledger(), TransactionKind.TRANSFER))
    }

    @Test
    fun mostUsedComeFirstAndEachNameOnce() {
        val ledger = Ledger(transactions = listOf(
            tx("1", TransactionKind.EXPENSE, "Rent"),
            tx("2", TransactionKind.EXPENSE, "Coffee"),
            tx("3", TransactionKind.EXPENSE, "Coffee"),
            tx("4", TransactionKind.EXPENSE, "transport"),
            tx("5", TransactionKind.INCOME, "Freelance"),
        ))
        val expense = Categories.suggestions(ledger, TransactionKind.EXPENSE)
        assertEquals(listOf("Coffee", "Rent", "transport"), expense.take(3))
        assertEquals(1, expense.count { it.equals("transport", ignoreCase = true) })
        assertTrue("Freelance" !in expense)
        assertEquals("Freelance", Categories.suggestions(ledger, TransactionKind.INCOME).first())
    }
}
