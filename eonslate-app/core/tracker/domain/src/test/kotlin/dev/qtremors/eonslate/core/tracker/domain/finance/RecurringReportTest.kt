package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.csv.RecurringCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter

class RecurringReportTest {
    private val bank = Account("bank", "Bank", "INR")
    private val day = 20_000L

    @Test
    fun csvListsEachScheduleWithItsMonthlyEquivalent() {
        val netflix = RecurringPayment("n", bank.id, 64_900, "Netflix, \"4K\"", PaymentInterval.YEARLY, 1, day, category = "Subscriptions")
        val pay = RecurringPayment("p", bank.id, 5_000_000, "Salary", PaymentInterval.MONTHLY, 1, day, kind = TransactionKind.INCOME, paused = true)
        val out = StringWriter()
        RecurringCsv.write(Ledger(accounts = listOf(bank)), listOf(netflix, pay), out)
        val rows = out.toString().trim().lines()
        assertEquals("name,kind,amount,currency,account,category,interval,every,next_billing,active,variable,monthly_equivalent", rows[0])
        assertEquals("\"Netflix, \"\"4K\"\"\",expense,649.00,INR,Bank,Subscriptions,yearly,1,2024-10-04,true,false,54.08", rows[1])
        assertEquals("Salary,income,50000.00,INR,Bank,,monthly,1,2024-10-04,false,false,50000.00", rows[2])
    }

    @Test
    fun hourlyPaymentsPostAtMostDailyWhileTheirNominalRateStaysHourly() {
        val hourly = RecurringPayment("h", bank.id, 100, "Parking", PaymentInterval.HOURLY, 1, day, every = 2, nextMinute = 0)
        assertEquals(36_525.0, hourly.monthlyMinor(), 0.001)
        assertEquals(100 * 365.25 / 12, hourly.monthlyPostedMinor(), 0.001)
        val twoDays = hourly.copy(every = 48)
        assertEquals(twoDays.monthlyMinor(), twoDays.monthlyPostedMinor(), 0.001)
    }

    @Test
    fun onlyFixedAndEstimatedPaymentsPostThemselves() {
        val fixed = RecurringPayment("f", bank.id, 100, "Rent", PaymentInterval.MONTHLY, 1, day)
        assertTrue(fixed.postsAutomatically)
        assertFalse(fixed.copy(variable = true).postsAutomatically)
        assertTrue(fixed.copy(variable = true, autoEstimate = true).postsAutomatically)
    }
}
