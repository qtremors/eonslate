package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.model.Interval
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AlertsTest {
    private val today = LocalDate.of(2026, 11, 14)
    private val day = today.toEpochDay()
    private val account = Account("a", "Bank", "INR")

    private fun payment(id: String, next: Long, variable: Boolean, paused: Boolean = false) =
        RecurringPayment(id, "a", 100, id, PaymentInterval.MONTHLY, 1, next, variable = variable, paused = paused)

    @Test
    fun orderAndCountFollowWhatNeedsDoing() {
        val budget = CategoryBudget("b", "a", "Food", 1_000, PaymentInterval.MONTHLY, 1, today.withDayOfMonth(1).toEpochDay())
        val ledger = Ledger(
            accounts = listOf(account),
            transactions = listOf(Transaction("t", "a", TransactionKind.EXPENSE, 1_500, day, category = "Food")),
            recurring = listOf(
                payment("power", day - 2, variable = true),
                payment("rent", day - 1, variable = false),
                payment("netflix", day + 2, variable = false),
                payment("gym", day + 2, variable = true, paused = true),
                payment("far", day + 9, variable = false),
            ),
            budgets = listOf(budget),
        )
        val status = IntervalStatus(Interval("i", every = 30), "Oil change", today.minusDays(40), null, today.minusDays(10), null, null, today)
        val alerts = Alerts.of(ledger, today, found = 3, intervals = listOf(Tracker(id = "bike", name = "Bike", createdAt = 0) to listOf(status)))

        assertEquals(Alert.FoundPayments(3), alerts[0])
        assertEquals("power", (alerts[1] as Alert.PaymentDue).payment.id)
        assertEquals(2L, (alerts[1] as Alert.PaymentDue).daysLate)
        assertTrue(alerts[2] is Alert.OverBudget)
        assertEquals("Bike", (alerts[3] as Alert.IntervalDue).tracker.name)
        assertEquals(listOf("netflix"), alerts.filterIsInstance<Alert.PaymentSoon>().map { it.payment.id })
        assertEquals(4, alerts.count { it.counts })
    }

    @Test
    fun nothingWaitingIsEmpty() {
        assertTrue(Alerts.of(Ledger(accounts = listOf(account)), today, found = 0, intervals = emptyList()).isEmpty())
    }
}
