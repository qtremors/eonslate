package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

class SharesTest {
    private val bank = Account("bank", "HDFC", "INR")
    private val day = LocalDate.of(2026, 11, 20).toEpochDay()
    private fun expense(id: String, minor: Long, vararg shares: Share) =
        Transaction(id, "bank", TransactionKind.EXPENSE, minor, day, category = "Food", shares = shares.toList())
    private fun income(id: String, minor: Long, vararg shares: Share) =
        Transaction(id, "bank", TransactionKind.INCOME, minor, day, category = "Other", shares = shares.toList())

    @Test
    fun lendingSplittingAndPayingBackReadAsTheyShould() {
        assertEquals(ShareKind.LENT, expense("a", 500, Share("rahul", 500)).shareKind)
        assertEquals(ShareKind.SPLIT, expense("b", 900, Share("rahul", 300), Share("priya", 300)).shareKind)
        assertEquals(ShareKind.PAID_BACK, expense("c", 1000, Share("rahul", 1000, settles = true)).shareKind)
        assertEquals(ShareKind.BORROWED, income("d", 1000, Share("rahul", -1000)).shareKind)
        assertEquals(ShareKind.GOT_BACK, income("e", 500, Share("rahul", -500, settles = true)).shareKind)
        assertEquals(null, expense("f", 500).shareKind)
    }

    @Test
    fun onlyYourOwnPartCountsAsSpending() {
        val dinner = expense("b", 900, Share("rahul", 300), Share("priya", 300))
        assertEquals(300L, dinner.own)
        assertEquals(0L, expense("a", 500, Share("rahul", 500)).own)
        val ledger = Ledger(accounts = listOf(bank), transactions = listOf(dinner, expense("a", 500, Share("rahul", 500))))
        val summary = MoneySummary.of(ledger, LocalDate.ofEpochDay(day), "INR")!!
        assertEquals(BigInteger.valueOf(300), summary.monthExpense)
        // The account still paid it all.
        assertEquals(BigInteger.valueOf(-1400), ledger.balances().getValue("bank"))
        assertEquals(mapOf("bank" to BigInteger.valueOf(300)), ledger.monthlySpending(YearMonth.of(2026, 11)))
    }

    @Test
    fun whoOwesWhomAddsUpAndSettlingClearsIt() {
        val ledger = Ledger(
            accounts = listOf(bank),
            transactions = listOf(
                expense("a", 500, Share("rahul", 500)),
                expense("b", 900, Share("rahul", 300), Share("priya", 300)),
                income("c", 1000, Share("priya", -1000)),
                income("d", 800, Share("rahul", -800, settles = true)),
            ),
        )
        val owed = ledger.owed()
        assertEquals(null, owed["rahul"])
        assertEquals(mapOf("INR" to BigInteger.valueOf(-700)), owed["priya"])
    }

    @Test
    fun sharesMustFitTheirTransaction() {
        assertTrue(expense("a", 500, Share("rahul", 500)).sharesValid())
        assertFalse(expense("a", 500, Share("rahul", 600)).sharesValid())
        assertFalse(expense("a", 500, Share("rahul", -100)).sharesValid())
        assertFalse(income("a", 500, Share("rahul", 100)).sharesValid())
        assertFalse(expense("a", 500, Share("rahul", 100), Share("rahul", 100)).sharesValid())
        assertFalse(expense("a", 500, Share("rahul", 100)).copy(kind = TransactionKind.TRANSFER, destinationId = "x").sharesValid())
    }

    @Test
    fun anEqualSplitLeavesYouTheOddPaise() {
        assertEquals(listOf(Share("a", 333), Share("b", 333)), splitEqually(1000, listOf("a", "b"), includeMe = true))
        assertEquals(listOf(Share("a", 500), Share("b", 500)), splitEqually(1000, listOf("a", "b"), includeMe = false))
        assertTrue(splitEqually(1000, emptyList(), includeMe = true).isEmpty())
    }
}
