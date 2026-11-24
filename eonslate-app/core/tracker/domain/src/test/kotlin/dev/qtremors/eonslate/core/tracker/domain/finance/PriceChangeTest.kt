package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

class PriceChangeTest {
    private val day = { m: Int, d: Int -> LocalDate.of(2026, m, d).toEpochDay() }
    private val music = RecurringPayment(
        "music", "bank", 11_900, "Apple Music", PaymentInterval.MONTHLY, 3, day(12, 3),
        priceChange = PriceStep(14_900, day(1, 3) + 365),
    )

    @Test
    fun eachPaymentCostsThePriceForItsDay() {
        assertEquals(11_900L, music.amountOn(day(12, 3)))
        assertEquals(14_900L, music.amountOn(day(1, 3) + 365))
        assertEquals(11_900L, music.transactionOn(day(12, 3)).minor)
        assertEquals(14_900L, music.transactionOn(day(1, 3) + 365).minor)
    }

    @Test
    fun theForecastShowsTheNewPriceFromItsMonth() {
        val account = Account("bank", "HDFC", "INR")
        val spending = SubscriptionSpending.of(music, Ledger(accounts = listOf(account), recurring = listOf(music)), LocalDate.of(2026, 11, 24))
        assertEquals(BigInteger.valueOf(11_900), spending.forecast[YearMonth.of(2026, 12)])
        assertEquals(BigInteger.valueOf(14_900), spending.forecast[YearMonth.of(2027, 1)])
    }

    @Test
    fun aNewPriceNeedsAnAmountAndADay() {
        val accounts = setOf("bank")
        assertFalse(music.copy(priceChange = PriceStep(0, day(1, 1))).isValid(accounts))
        assertFalse(music.copy(priceChange = PriceStep(100, Long.MAX_VALUE)).isValid(accounts))
    }
}
