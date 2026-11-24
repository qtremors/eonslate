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
    fun cashFlowEndsTodayAndCategoriesRankLargestFirst() {
        val half = summary.cashFlow(CashFlowRange.HALF_YEAR, today)
        assertEquals(CashFlow.Step.MONTH, half.step)
        assertEquals(6, half.buckets.size)
        assertEquals(YearMonth.of(2026, 11).atDay(1).toEpochDay()..day, half.buckets.last().days)
        assertEquals(BigInteger.valueOf(4_000), half.buckets[4].spending)
        assertEquals(BigInteger.valueOf(15_000), half.spending)
        assertEquals(BigInteger.valueOf(35_000), half.net)
        assertEquals(listOf("Rent", "Food"), summary.categories.map { it.first })
    }

    @Test
    fun aDayShowsItsTopCategoryAndTheWeekCountsOnlyItsDays() {
        val week = summary.cashFlow(CashFlowRange.WEEK, today)
        assertEquals(CashFlow.Step.DAY, week.step)
        assertEquals(7, week.buckets.size)
        val now = week.buckets.last()
        assertEquals("Rent", now.topCategory)
        assertEquals(BigInteger.valueOf(7_000), now.topAmount)
        assertEquals(BigInteger.valueOf(40_000), now.net)
        assertEquals(BigInteger.valueOf(11_000), week.spending)
        assertNull(week.buckets.first().topCategory)
    }

    @Test
    fun allTimeUsesMonthsThenYears() {
        assertEquals(2, summary.cashFlow(CashFlowRange.ALL, today).buckets.size)
        val old = tx("old", TransactionKind.EXPENSE, 100, epochDay = LocalDate.of(2023, 6, 1).toEpochDay())
        val long = MoneySummary.of(ledger.copy(transactions = ledger.transactions + old), today, "INR")!!.cashFlow(CashFlowRange.ALL, today)
        assertEquals(CashFlow.Step.YEAR, long.step)
        assertEquals(4, long.buckets.size)
        assertEquals(BigInteger.valueOf(100), long.buckets.first().spending)
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
