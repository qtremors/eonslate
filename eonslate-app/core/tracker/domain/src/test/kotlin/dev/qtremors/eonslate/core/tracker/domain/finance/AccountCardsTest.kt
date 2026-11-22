package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCardsTest {
    private val bank = Account("bank", "HDFC Savings", "INR", digits = "4821")
    private val card = Account("card", "Amazon Pay ICICI", "INR", kind = AccountKind.CARD, digits = "9012")
    private val named = Account("named", "SBI 7711", "INR")
    private val usd = Account("usd", "Travel", "USD", digits = "9012")
    private val accounts = listOf(bank, card, named, usd)

    @Test
    fun paymentsFindTheirAccountByLastDigitsThenNameThenCurrency() {
        assertEquals(card, accounts.forPayment("INR", "9012"))
        assertEquals(usd, accounts.forPayment("USD", "9012"))
        assertEquals(named, accounts.forPayment("INR", "7711"))
        assertEquals(bank, accounts.forPayment("INR", "0000"))
        assertEquals(bank, accounts.forPayment("INR", null))
        assertEquals(null, accounts.forPayment("EUR", "9012"))
    }

    @Test
    fun digitsAreFourNumbersAndShowInTheLabel() {
        assertTrue(card.isValid())
        assertFalse(card.copy(digits = "901").isValid())
        assertFalse(card.copy(digits = "90a2").isValid())
        assertEquals("Amazon Pay ICICI •9012", card.label)
        assertEquals("SBI 7711", named.label)
    }

    @Test
    fun anAccountFilterKeepsBothSidesOfTransfers() {
        val ledger = Ledger(
            accounts = listOf(bank, card),
            transactions = listOf(
                Transaction("a", "bank", TransactionKind.EXPENSE, 100, 1),
                Transaction("b", "card", TransactionKind.EXPENSE, 100, 1),
                Transaction("c", "bank", TransactionKind.TRANSFER, 100, 1, destinationId = "card"),
            ),
        )
        assertEquals(setOf("b", "c"), ledger.filteredTransactions(TransactionFilter(accountId = "card")).map { it.id }.toSet())
        assertEquals(3, ledger.filteredTransactions(TransactionFilter()).size)
    }
}
