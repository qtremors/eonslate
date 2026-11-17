package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringTest {
    @Test
    fun aShortMonthKeepsTheOriginalDueDay() {
        val january = LocalDate.of(2026, 1, 31).toEpochDay()
        val payment = RecurringPayment("p", "bank", 100, "", PaymentInterval.MONTHLY, 31, january)
        val february = payment.after(january)!!
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), february)
        assertEquals(LocalDate.of(2026, 3, 31).toEpochDay(), payment.after(february))
    }
}
