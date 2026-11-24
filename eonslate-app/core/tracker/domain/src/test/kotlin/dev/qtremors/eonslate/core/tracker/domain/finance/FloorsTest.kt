package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class FloorsTest {
    private val hdfc = Account("hdfc", "HDFC", "INR", openingMinor = 1_200_000, floorMinor = 1_000_000)
    private val card = Account("card", "Card", "INR", kind = AccountKind.CARD, floorMinor = -5_000_000)
    private val ledger = Ledger(accounts = listOf(hdfc, card))
    private fun expense(id: String, account: String, minor: Long) = Transaction(id, account, TransactionKind.EXPENSE, minor, 1)

    @Test
    fun aPaymentThatTakesAnAccountUnderItsFloorIsCaught() {
        val breaches = Floors.breaches(ledger, expense("t", "hdfc", 400_000), null, null)
        val breach = breaches.single()
        assertEquals(hdfc, breach.account)
        assertEquals(BigInteger.valueOf(200_000), breach.under)
        assertTrue(Floors.breaches(ledger, expense("t", "hdfc", 100_000), null, null).isEmpty())
        // A card may go below zero, down to its limit.
        assertTrue(Floors.breaches(ledger, expense("t", "card", 4_000_000), null, null).isEmpty())
        assertEquals(1, Floors.breaches(ledger, expense("t", "card", 6_000_000), null, null).size)
    }

    @Test
    fun anEditCountsOnlyTheDifferenceAndIncomeNeverWarns() {
        val spent = ledger.copy(transactions = listOf(expense("t", "hdfc", 150_000)))
        // Lowering the same payment never warns, raising it does once it crosses.
        assertTrue(Floors.breaches(spent, expense("t", "hdfc", 100_000), expense("t", "hdfc", 150_000), null).isEmpty())
        assertEquals(1, Floors.breaches(spent, expense("t", "hdfc", 300_000), expense("t", "hdfc", 150_000), null).size)
        val under = ledger.copy(transactions = listOf(expense("big", "hdfc", 500_000)))
        assertTrue(Floors.breaches(under, Transaction("i", "hdfc", TransactionKind.INCOME, 1, 1), null, null).isEmpty())
    }

    @Test
    fun theOverallFloorWatchesEveryAccountInItsCurrency() {
        val overall = OverallFloor("INR", 1_000_000)
        val breach = Floors.breaches(ledger, expense("t", "card", 300_000), null, overall).single { it.account == null }
        assertEquals(BigInteger.valueOf(900_000), breach.balance)
        assertEquals(listOf(null), Floors.below(ledger.copy(transactions = listOf(expense("t", "card", 300_000))), overall).map { it.account })
    }
}
