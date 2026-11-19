package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

class SubscriptionSpendingTest {
    private val bank = Account("account", "Bank", "INR")
    private val today = LocalDate.of(2026, 11, 11)
    private val start = LocalDate.of(2026, 5, 15)
    private val next = LocalDate.of(2026, 11, 15)
    private val payment = RecurringPayment(
        "codex", bank.id, 199_900, "Codex", PaymentInterval.MONTHLY, 15,
        next.toEpochDay(), startEpochDay = start.toEpochDay(),
    )

    @Test
    fun aMayStartGivesAnEstimateWithoutInventingPayments() {
        val ledger = Ledger(accounts = listOf(bank), recurring = listOf(payment))
        val report = SubscriptionSpending.of(payment, ledger, today)
        assertEquals(BigInteger.ZERO, report.recorded)
        assertEquals(BigInteger.valueOf(1_199_400), report.estimatedSinceStart)
        assertEquals(BigInteger.valueOf(2_398_800), report.projected)
        assertEquals(BigInteger.valueOf(199_900), report.forecast[YearMonth.of(2026, 11)])
    }

    @Test
    fun recordedTotalsUseLinkedAmountsAndAnUnambiguousLegacyPayment() {
        val may = payment.transactionOn(start.toEpochDay()).copy(minor = 180_000)
        val june = may.copy(id = "legacy-id", epochDay = start.plusMonths(1).toEpochDay(), note = "Codex · Recurring subscription payment")
        val sameName = may.copy(id = "manual", note = payment.note)
        val ledger = Ledger(listOf(bank), listOf(may, june, sameName), listOf(payment))
        val report = SubscriptionSpending.of(payment, ledger, today)
        assertEquals(2, report.paymentCount)
        assertEquals(BigInteger.valueOf(360_000), report.recorded)
        assertEquals(BigInteger.valueOf(180_000), report.history[YearMonth.of(2026, 5)])
        val ambiguous = ledger.copy(recurring = listOf(payment, payment.copy(id = "duplicate")))
        assertEquals(1, SubscriptionSpending.of(payment, ambiguous, today).paymentCount)
    }

    @Test
    fun forecastSkipsOverdueDatesAndPausedPayments() {
        val overdue = payment.copy(nextEpochDay = LocalDate.of(2026, 9, 30).toEpochDay(), anchorDay = 31)
        val ledger = Ledger(accounts = listOf(bank), recurring = listOf(overdue))
        val report = SubscriptionSpending.of(overdue, ledger, today)
        assertEquals(BigInteger.valueOf(2_398_800), report.projected)
        val paused = SubscriptionSpending.of(overdue.copy(paused = true), ledger, today)
        assertEquals(BigInteger.ZERO, paused.projected)
        assertEquals(null, paused.estimatedSinceStart)
    }
}
