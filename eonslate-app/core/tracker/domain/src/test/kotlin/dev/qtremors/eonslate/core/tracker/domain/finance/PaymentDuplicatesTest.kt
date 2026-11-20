package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentDuplicatesTest {
    private val bank = Account("bank", "HDFC 1234", "INR")
    private val day = 20_400L

    private fun message(key: String, minor: Long = 25_000) =
        PaymentMessage(TransactionKind.EXPENSE, "INR", minor, merchant = "Cafe", epochDay = day, key = key.repeat(64).take(64))

    @Test
    fun aManualEntryStandsForOneMatchingMessage() {
        val manual = Transaction("m", bank.id, TransactionKind.EXPENSE, 25_000, day, note = "Coffee")
        val ledger = Ledger(accounts = listOf(bank), transactions = listOf(manual))
        // Two identical coffees that day, one already typed in: one still waits.
        val fresh = PaymentDuplicates.fresh(listOf(message("a"), message("b")), ledger, day)
        assertEquals(1, fresh.size)
        // A different amount is a different payment.
        assertEquals(1, PaymentDuplicates.fresh(listOf(message("c", 30_000)), ledger, day).size)
    }

    @Test
    fun aRecordedFingerprintIsNeverOfferedAgain() {
        val saved = message("a").asTransaction("t", listOf(bank), day)!!
        val ledger = Ledger(accounts = listOf(bank), transactions = listOf(saved))
        assertEquals(listOf(message("b").key), PaymentDuplicates.fresh(listOf(message("a"), message("b")), ledger, day).map { it.key })
    }

    @Test
    fun approvedPaymentsKeepWhereTheyCameFrom() {
        val parsed = PaymentMessages.parse("Rs 499.00 debited from A/c XX1234 to NETFLIX on 11-11-2026. UPI Ref 998877.", "AD-HDFCBKUPI")!!
        assertEquals("HDFC Bank", parsed.issuer)
        val transaction = parsed.asTransaction("t", listOf(bank), day)!!
        assertEquals("From HDFC Bank · Account •1234", transaction.details)
        assertEquals(bank.id, transaction.accountId)
    }

    @Test
    fun aReferenceWrittenInCapitalsIsKept() {
        val parsed = PaymentMessages.parse("INR 1,200.00 credited to A/c XX9876 from ACME LTD. Ref: SALNOVPAY", "VM-SBIINB")!!
        assertEquals("SALNOVPAY", parsed.reference)
    }
}
