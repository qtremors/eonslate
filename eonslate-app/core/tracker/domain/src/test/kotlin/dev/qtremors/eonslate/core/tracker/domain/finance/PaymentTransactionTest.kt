package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentTransactionTest {
    private val cash = Account("cash", "Cash", "INR")
    private val card = Account("card", "HDFC 4321", "INR")
    private val usd = Account("usd", "Travel", "USD")

    private val payment = PaymentMessage(
        kind = TransactionKind.EXPENSE, currency = "INR", minor = 45_000,
        merchant = "Swiggy", category = "Food & Dining", accountEnding = "4321", reference = "123456", epochDay = 20_400, key = "k".repeat(64),
    )

    @Test
    fun theNamedCardIsChosen() {
        val t = payment.asTransaction("t", listOf(cash, card, usd), today = 20_405)!!
        assertEquals("card", t.accountId)
        assertEquals(45_000, t.minor)
        assertEquals(20_400, t.epochDay)
        assertEquals("Swiggy · Ref 123456", t.note)
        assertEquals("Food & Dining", t.category)
        assertEquals("k".repeat(64), t.smsKey)
    }

    @Test
    fun withoutAMatchTheFirstAccountInTheCurrencyIsUsed() {
        assertEquals("cash", payment.copy(accountEnding = "9999").asTransaction("t", listOf(cash, card), 0)!!.accountId)
        assertEquals(7L, payment.copy(epochDay = null).asTransaction("t", listOf(cash), 7)!!.epochDay)
    }

    @Test
    fun noAccountInTheCurrencyMeansNoTransaction() {
        assertNull(payment.asTransaction("t", listOf(usd), 0))
    }
}
