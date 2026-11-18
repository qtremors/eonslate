package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

class MoneySummaryTest {
    private val today = LocalDate.of(2026, 11, 11)
    private val day = today.toEpochDay()
    private val main = Account("a", "Main", "INR", openingMinor = 10_000)
    private val usd = Account("u", "Travel", "USD", openingMinor = 99_999)

    private fun tx(id: String, kind: TransactionKind, minor: Long, epochDay: Long = day, category: String = "", account: String = "a") =
        Transaction(id, account, kind, minor, epochDay, category = category)

    private val ledger = Ledger(
        accounts = listOf(main, usd),
        transactions = listOf(
            tx("1", TransactionKind.INCOME, 50_000),
            tx("2", TransactionKind.EXPENSE, 3_000, category = "Food"),
            tx("3", TransactionKind.EXPENSE, 7_000, category = "Rent"),
            tx("4", TransactionKind.EXPENSE, 1_000, category = "Food", epochDay = day - 1),
            tx("5", TransactionKind.EXPENSE, 4_000, epochDay = LocalDate.of(2026, 10, 3).toEpochDay()),
            tx("6", TransactionKind.EXPENSE, 500, account = "u"),
        ),
        recurring = listOf(
            RecurringPayment("r1", "a", 999, "Music", PaymentInterval.MONTHLY, 13, day + 2),
            RecurringPayment("r2", "a", 999, "Later", PaymentInterval.MONTHLY, 30, day + 20),
            RecurringPayment("r3", "a", 999, "Paused", PaymentInterval.MONTHLY, 12, day + 1, paused = true),
        ),
        budgets = listOf(CategoryBudget("b", "a", "Food", 3_000, PaymentInterval.MONTHLY, 1, LocalDate.of(2026, 11, 1).toEpochDay())),
    )

    private val summary = MoneySummary.of(ledger, today, "INR")!!

    @Test
    fun totalsStayInOneCurrency() {
        assertEquals("INR", summary.currency)
        assertEquals(BigInteger.valueOf(10_000 + 50_000 - 3_000 - 7_000 - 1_000 - 4_000), summary.balance)
        assertEquals(BigInteger.valueOf(50_000), summary.monthIncome)
        assertEquals(BigInteger.valueOf(11_000), summary.monthExpense)
        assertEquals(BigInteger.valueOf(39_000), summary.monthNet)
    }

    @Test
    fun trendEndsThisMonthAndCategoriesRankLargestFirst() {
        assertEquals(MoneySummary.TREND_MONTHS, summary.trend.size)
        assertEquals(YearMonth.of(2026, 11), summary.trend.last().first)
        assertEquals(BigInteger.valueOf(4_000), summary.trend[summary.trend.size - 2].second)
        assertEquals(listOf("Rent", "Food"), summary.categories.map { it.first })
    }

    @Test
    fun overBudgetAndDueSoonAreFound() {
        assertTrue(summary.budgets.single().over)
        assertEquals(listOf("r1"), summary.dueSoon.map { it.id })
        assertEquals(day, summary.recent.first().epochDay)
        assertEquals(MoneySummary.RECENT, summary.recent.size)
    }

    @Test
    fun noAccountsMeansNoSummary() {
        assertNull(MoneySummary.of(Ledger(), today, "INR"))
        assertEquals("USD", MoneySummary.of(Ledger(accounts = listOf(usd)), today, "INR")!!.currency)
    }
}
