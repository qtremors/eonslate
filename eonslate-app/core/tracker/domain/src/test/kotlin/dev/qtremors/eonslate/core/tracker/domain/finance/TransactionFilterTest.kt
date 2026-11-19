package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFilterTest {
    private val bank = Account("bank", "Bank", "INR")
    private val coffee = Transaction("coffee", bank.id, TransactionKind.EXPENSE, 300, 20_000, note = "Coffee", category = "Food", createdAt = 2)
    private val rent = Transaction("rent", bank.id, TransactionKind.EXPENSE, 90_000, 20_000, note = "Rent", category = "Bills", createdAt = 1)
    private val pay = Transaction("pay", bank.id, TransactionKind.INCOME, 150_000, 19_990, note = "Pay", category = "Salary", details = "November bonus")
    private val bus = Transaction("bus", bank.id, TransactionKind.EXPENSE, 50, 19_950, note = "Bus", category = "")
    private val ledger = Ledger(accounts = listOf(bank), transactions = listOf(bus, rent, pay, coffee))

    private fun ids(filter: TransactionFilter) = ledger.filteredTransactions(filter).map(Transaction::id)

    @Test
    fun sameDayEntriesFollowTheOrderTheyWereSaved() {
        assertEquals(listOf("coffee", "rent", "pay", "bus"), ids(TransactionFilter()))
        assertEquals(listOf("bus", "pay", "rent", "coffee"), ids(TransactionFilter(sort = TransactionSort.OLDEST)))
    }

    @Test
    fun everySortOrdersByItsOwnKey() {
        assertEquals(listOf("bus", "coffee", "rent", "pay"), ids(TransactionFilter(sort = TransactionSort.SMALLEST)))
        assertEquals(listOf("pay", "rent", "coffee", "bus"), ids(TransactionFilter(sort = TransactionSort.LARGEST)))
        // Named categories alphabetically, then the uncategorized.
        assertEquals(listOf("rent", "coffee", "pay", "bus"), ids(TransactionFilter(sort = TransactionSort.CATEGORY)))
    }

    @Test
    fun kindDaysAndSearchNarrowTogether() {
        assertEquals(listOf("pay"), ids(TransactionFilter(kind = TransactionKind.INCOME)))
        assertEquals(listOf("coffee", "rent", "pay"), ids(TransactionFilter(days = 19_960L..20_000L)))
        assertEquals(listOf("pay"), ids(TransactionFilter(query = "bonus")))
        assertEquals(emptyList<String>(), ids(TransactionFilter(kind = TransactionKind.INCOME, category = "food")))
    }

    @Test
    fun notesHaveTheirOwnLimit() {
        val accounts = mapOf(bank.id to bank)
        assertEquals(true, coffee.copy(details = "x".repeat(Transaction.DETAILS_LIMIT)).isValid(accounts))
        assertEquals(false, coffee.copy(details = "x".repeat(Transaction.DETAILS_LIMIT + 1)).isValid(accounts))
    }
}
