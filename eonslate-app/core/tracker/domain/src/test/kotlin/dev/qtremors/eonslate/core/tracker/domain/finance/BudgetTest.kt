package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.csv.BudgetCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter
import java.math.BigInteger
import java.time.LocalDate

class BudgetTest {
    private val today = LocalDate.of(2026, 11, 13)
    private val bank = Account("bank", "Bank", "INR")
    private val cash = Account("cash", "Cash", "INR", monthlyBudgetMinor = 1_000)
    private val travel = Account("travel", "Travel", "EUR")
    private val food = CategoryBudget("food", bank.id, "Food", 10_000, PaymentInterval.MONTHLY, 1, LocalDate.of(2026, 11, 1).toEpochDay())

    private fun expense(id: String, account: Account, minor: Long, category: String = "Food") =
        Transaction(id, account.id, TransactionKind.EXPENSE, minor, today.toEpochDay(), category = category)

    private val ledger = Ledger(
        accounts = listOf(bank, cash, travel),
        transactions = listOf(expense("a", bank, 5_000), expense("b", cash, 3_500), expense("c", travel, 9_999), expense("d", cash, 200, "Bus")),
        budgets = listOf(food),
    )

    @Test
    fun allAccountsCountsEveryAccountInTheBudgetsCurrencyOnly() {
        assertEquals(BigInteger.valueOf(5_000), ledger.spendingFor(food, today))
        assertEquals(BigInteger.valueOf(8_500), ledger.spendingFor(food.copy(allAccounts = true), today))
    }

    @Test
    fun healthWarnsFromEightyPercentAndCoversAccountLimits() {
        val health = ledger.copy(budgets = listOf(food.copy(allAccounts = true))).budgetHealth(today)
        val budget = health.single { it.budget != null }
        assertEquals(85, budget.percent)
        assertTrue(budget.warning)
        assertFalse(budget.over)
        assertEquals(BigInteger.valueOf(1_500), budget.remaining)

        val limit = health.single { it.account == cash }
        assertEquals(BigInteger.valueOf(3_700), limit.spent)
        assertTrue(limit.over)
        assertFalse(limit.warning)
        assertEquals(370, limit.percent)
    }

    @Test
    fun hourlyBudgetsLastTheirHoursRoundedUpToDays() {
        val start = LocalDate.of(2026, 11, 10)
        val hourly = food.copy(interval = PaymentInterval.HOURLY, every = 36, startEpochDay = start.toEpochDay())
        assertTrue(hourly.isValid(setOf(bank.id)))
        // 36 hours is two days: the 10th and 11th, then the 12th and 13th.
        assertEquals(start.plusDays(2).toEpochDay() until start.plusDays(4).toEpochDay(), hourly.periodOn(today))
        assertEquals(null, hourly.periodOn(start.minusDays(1)))
    }

    @Test
    fun budgetCsvWritesSpendingAndUtilization() {
        val out = StringWriter()
        BudgetCsv.write(ledger, today, out)
        val rows = out.toString().trim().lines()
        assertEquals("name,category,account,currency,limit,spent,remaining,utilization,interval,every,period_start,period_end,all_accounts", rows[0])
        assertEquals("Food,Food,Bank,INR,100.00,50.00,50.00,50%,monthly,1,2026-11-01,2026-11-30,false", rows[1])
        assertEquals("Cash,,Cash,INR,10.00,37.00,-27.00,370%,monthly,1,2026-11-01,2026-11-30,false", rows[2])
    }
}
