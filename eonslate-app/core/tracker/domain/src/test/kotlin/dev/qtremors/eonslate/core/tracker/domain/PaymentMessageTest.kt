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

    @Test
    fun legacySenderAndCardFormatsKeepThePayeeAndIgnoreTheCreditLimit() {
        val parsed = PaymentMessages.parse(
            "Rs.232.34 spent on credit card ending in •••• 4440 at Zomato on 16-AUG-26. Avail limit: Rs.45,000.00.",
            "VD-HDFCBK-S",
        )!!
        assertEquals("HDFC Bank", parsed.issuer)
        assertEquals("Zomato", parsed.merchant)
        assertEquals("4440", parsed.accountEnding)
        assertEquals(true, parsed.isCard)
        assertEquals(23_234L, parsed.minor)
        assertEquals(LocalDate.of(2026, 8, 16).toEpochDay(), parsed.epochDay)
        assertEquals(1f, parsed.confidence)
    }

    @Test
    fun indianUpiAlertsCanOmitTheCurrencyAndGiveTheReferenceBeforeThePayee() {
        val parsed = PaymentMessages.parse(
            "Dear UPI user AC No: XX 9876 debited by 320.0 on 18Aug26. Info: UPI/CR/654321/Uber Eats. Bal: INR 2000.",
            "VDSBIUPI",
        )!!
        assertEquals(TransactionKind.EXPENSE, parsed.kind)
        assertEquals("INR", parsed.currency)
        assertEquals(32_000L, parsed.minor)
        assertEquals("SBI", parsed.issuer)
        assertEquals("9876", parsed.accountEnding)
        assertEquals("654321", parsed.reference)
        assertEquals("Uber Eats", parsed.merchant)
        assertEquals("Food & Dining", parsed.category)
        assertEquals(LocalDate.of(2026, 8, 18).toEpochDay(), parsed.epochDay)
    }

    @Test
    fun senderFallbackKeepsTheCategoryAndMissingAccountLowersConfidence() {
        val parsed = PaymentMessages.parse("Your payment of Rs 232.34 was successful. Ref: 4422.", "CP-ZOMATO-S")!!
        assertEquals("Zomato", parsed.merchant)
        assertEquals("Food & Dining", parsed.category)
        assertEquals("4422", parsed.reference)
        assertEquals(0.85f, parsed.confidence)
    }

    @Test
    fun referenceAndMonthTokensAreNeverCurrencyCodes() {
        val parsed = PaymentMessages.parse(
            "Rs 232.34 spent on credit card ending 4440 at Zomato on 16-AUG-26. UPI Ref 4422.",
        )!!
        assertEquals("INR", parsed.currency)
        assertEquals(23_234L, parsed.minor)
        assertEquals("4422", parsed.reference)
    }
}
