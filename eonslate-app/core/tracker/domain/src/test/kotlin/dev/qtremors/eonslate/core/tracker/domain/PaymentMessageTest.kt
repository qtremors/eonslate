package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessages
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PaymentMessageTest {
    @Test
    fun aDebitUsesTheAmountBeforeTheBalance() {
        val text = "Your A/C was debited by Rs. 1,25,100.50. Avl bal INR 4,500.00."
        val parsed = PaymentMessages.parse(text)!!
        assertEquals(TransactionKind.EXPENSE, parsed.kind)
        assertEquals("INR", parsed.currency)
        assertEquals(12_510_050L, parsed.minor)
    }

    @Test
    fun aCreditRespectsTheCurrencysFractionDigits() {
        val parsed = PaymentMessages.parse("JPY 2,300 credited to your account.")!!
        assertEquals(TransactionKind.INCOME, parsed.kind)
        assertEquals(2300L, parsed.minor)
        assertNull(PaymentMessages.parse("JPY 23.45 credited to your account."))
    }

    @Test
    fun aBankAlertKeepsReviewDetailsAndADuplicateKey() {
        val text = "A/c XX1234 INR 1,250.50 debited. Paid to Swiggy on 11-11-2026. UPI ref 123456789012."
        val parsed = PaymentMessages.parse(text, "VM-BANK")!!
        assertEquals("1234", parsed.accountEnding)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals("Food & Dining", parsed.category)
        assertEquals("123456789012", parsed.reference)
        assertEquals(LocalDate.of(2026, 11, 11).toEpochDay(), parsed.epochDay)
        assertEquals("VM-BANK", parsed.sender)
        assertEquals(parsed.key, PaymentMessages.parse(text + " Avl bal INR 4,500.00.")!!.key)
    }

    @Test
    fun incompleteAndAmbiguousAlertsNeedManualEntry() {
        assertNull(PaymentMessages.parse("Your OTP is 1919 for INR 44.00"))
        assertNull(PaymentMessages.parse("Payment INR 50 was declined and not debited."))
        assertNull(PaymentMessages.parse("INR 14.00 debited and USD 2.00 debited."))
        assertNull(PaymentMessages.parse("INR 1,2.00 debited."))
    }
}
