package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.insights.Alert
import dev.qtremors.eonslate.core.tracker.domain.insights.Alerts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

class DepositsTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    /** ₹1,00,000 at 7% a year, compounded quarterly, for one year from 1 Jan 2026. */
    private val terms = Deposit(10_000_000, 700, day(2026, 1, 1), day(2027, 1, 1))
    private val fd = Account("fd", "SBI FD", "INR", openingMinor = 10_000_000, kind = AccountKind.DEPOSIT, deposit = terms)
    private val hdfc = Account("hdfc", "HDFC", "INR", openingMinor = 5_000_000)

    @Test
    fun quarterlyCompoundingMatchesABankCalculator() {
        // ₹1,07,185.90, as any bank's FD calculator gives.
        assertEquals(BigInteger.valueOf(10_718_590), Deposits.valueOn(terms, terms.maturityEpochDay))
    }

    @Test
    fun daysPastTheLastWholePeriodEarnSimpleInterest() {
        // One quarter compounds to ₹1,01,750; 30 more days earn 7% × 30/365 on that.
        assertEquals(BigInteger.valueOf(10_233_541), Deposits.valueOn(terms, day(2026, 5, 1)))
    }

    @Test
    fun simpleInterestIsOnThePrincipalAlone() {
        val simple = terms.copy(rateBp = 600, compounding = Compounding.SIMPLE)
        // 6% × 182/365 of ₹1,00,000 is ₹2,991.78.
        assertEquals(BigInteger.valueOf(10_299_178), Deposits.valueOn(simple, terms.startEpochDay + 182))
    }

    @Test
    fun nothingIsEarnedBeforeTheStartOrAfterMaturity() {
        assertEquals(BigInteger.valueOf(10_000_000), Deposits.valueOn(terms, terms.startEpochDay - 30))
        assertEquals(Deposits.valueOn(terms, terms.maturityEpochDay), Deposits.valueOn(terms, terms.maturityEpochDay + 400))
    }

    @Test
    fun aMonthFromTheLastDayOfJanuaryEndsOnTheLastDayOfFebruary() {
        val monthly = Deposit(10_000_000, 1_200, day(2026, 1, 31), day(2027, 1, 31), Compounding.MONTHLY)
        // A whole month at 1%, and no part month on top.
        assertEquals(BigInteger.valueOf(10_100_000), Deposits.valueOn(monthly, day(2026, 2, 28)))
    }

    @Test
    fun aDepositAccountNeedsItsTermsAndOnlyItHasThem() {
        assertTrue(fd.isValid())
        assertFalse(fd.copy(deposit = null).isValid())
        assertFalse(hdfc.copy(deposit = terms).isValid())
        assertFalse(fd.copy(deposit = terms.copy(rateBp = 0)).isValid())
        assertFalse(fd.copy(deposit = terms.copy(rateBp = Deposit.MAX_RATE_BP + 1)).isValid())
        assertFalse(fd.copy(deposit = terms.copy(maturityEpochDay = terms.startEpochDay)).isValid())
        assertFalse(fd.copy(deposit = terms.copy(principalMinor = 0)).isValid())
        assertFalse(fd.copy(deposit = terms.copy(closedEpochDay = terms.startEpochDay - 1)).isValid())
        assertFalse(AccountKind.DEPOSIT in AccountKind.EVERYDAY)
    }

    @Test
    fun paidFromAnAccountItStartsEmptyAndATransferFillsIt() {
        val (account, funding) = Deposits.opening(fd.copy(openingMinor = 0), hdfc, "t")
        assertEquals(0L, account.openingMinor)
        val transfer = funding.single()
        assertEquals(TransactionKind.TRANSFER, transfer.kind)
        assertEquals("hdfc", transfer.accountId)
        assertEquals("fd", transfer.destinationId)
        assertEquals(terms.principalMinor, transfer.minor)
        assertEquals(terms.startEpochDay, transfer.epochDay)
        val balances = Ledger(accounts = listOf(account, hdfc), transactions = funding).balances()
        assertEquals(BigInteger.valueOf(10_000_000), balances["fd"])
        assertEquals(BigInteger.valueOf(-5_000_000), balances["hdfc"])

        val (already, none) = Deposits.opening(fd.copy(openingMinor = 0), null, "t")
        assertEquals(terms.principalMinor, already.openingMinor)
        assertTrue(none.isEmpty())
    }

    @Test
    fun closingRecordsTheInterestAndPaysEverythingOut() {
        val ledger = Ledger(accounts = listOf(fd, hdfc))
        val status = Deposits.status(ledger, terms.maturityEpochDay).single()
        var next = 0
        val (closed, moves) = Deposits.closing(status, 10_718_590, hdfc, terms.maturityEpochDay, { "c${next++}" }, "Interest", "Charges")!!
        assertEquals(terms.maturityEpochDay, closed.deposit?.closedEpochDay)
        assertTrue(closed.isClosed)
        val (interest, payout) = moves
        assertEquals(TransactionKind.INCOME, interest.kind)
        assertEquals(718_590L, interest.minor)
        assertEquals("Interest", interest.note)
        assertEquals(TransactionKind.TRANSFER, payout.kind)
        assertEquals("hdfc", payout.destinationId)
        val after = ledger.copy(accounts = listOf(closed, hdfc), transactions = moves).balances()
        assertEquals(BigInteger.ZERO, after["fd"])
        assertEquals(BigInteger.valueOf(15_718_590), after["hdfc"])
        // Closed early or late, it is worth what came out, and earned what that adds to the principal.
        val early = Deposits.closing(status, 10_050_000, hdfc, terms.startEpochDay, { "e${next++}" }, "Interest", "Charges")!!
        val shut = Deposits.status(ledger.copy(accounts = listOf(early.first, hdfc), transactions = early.second), terms.maturityEpochDay).single()
        assertEquals(BigInteger.valueOf(10_050_000), shut.worth)
        assertEquals(BigInteger.valueOf(50_000), shut.earned)
    }

    @Test
    fun closingEarlyForLessRecordsTheCharges() {
        val status = Deposits.status(Ledger(accounts = listOf(fd, hdfc)), day(2026, 6, 1)).single()
        val (_, moves) = Deposits.closing(status, 9_900_000, hdfc, day(2026, 6, 1), { "x" }, "Interest", "Charges")!!
        assertEquals(TransactionKind.EXPENSE, moves.first().kind)
        assertEquals(100_000L, moves.first().minor)
        assertEquals("Charges", moves.first().note)
        // Exactly what it holds needs nothing but the payout.
        assertEquals(1, Deposits.closing(status, 10_000_000, hdfc, day(2026, 6, 1), { "x" }, "", "")!!.second.size)
    }

    @Test
    fun aDepositCannotCloseIntoItselfAnotherCurrencyOrTwice() {
        val usd = Account("usd", "Wise", "USD")
        val status = Deposits.status(Ledger(accounts = listOf(fd, hdfc)), terms.maturityEpochDay).single()
        val close = { into: Account, day: Long, s: DepositStatus -> Deposits.closing(s, 10_718_590, into, day, { "x" }, "", "") }
        assertNull(close(fd, terms.maturityEpochDay, status))
        assertNull(close(usd, terms.maturityEpochDay, status))
        assertNull(close(hdfc, terms.startEpochDay - 1, status))
        val shut = status.copy(deposit = terms.copy(closedEpochDay = terms.maturityEpochDay))
        assertNull(close(hdfc, terms.maturityEpochDay, shut))
        assertNull(Deposits.closing(status, 0, hdfc, terms.maturityEpochDay, { "x" }, "", ""))
    }

    @Test
    fun runningDepositsComeFirstBySoonestMaturity() {
        val later = fd.copy(id = "later", name = "Later", deposit = terms.copy(maturityEpochDay = day(2028, 1, 1)))
        val shut = fd.copy(id = "shut", name = "Shut", deposit = terms.copy(closedEpochDay = day(2026, 3, 1)))
        val order = Deposits.status(Ledger(accounts = listOf(shut, later, fd, hdfc)), day(2026, 6, 1)).map { it.account.id }
        assertEquals(listOf("fd", "later", "shut"), order)
    }

    @Test
    fun aStatusSaysWhatItIsWorthAndHowFarAlongItIs() {
        val status = Deposits.status(Ledger(accounts = listOf(fd)), day(2026, 7, 2)).single()
        assertEquals(BigInteger.valueOf(10_718_590), status.atMaturity)
        assertTrue(status.earned.signum() > 0)
        assertEquals(0.5f, status.fraction, 0.01f)
        assertFalse(status.matured)
    }

    @Test
    fun aMaturedDepositCountsOnTheBellAndOneDueSoonIsOnlyShown() {
        val ledger = Ledger(accounts = listOf(fd, hdfc))
        val matured = Alerts.of(ledger, LocalDate.ofEpochDay(terms.maturityEpochDay + 2), 0, emptyList())
            .filterIsInstance<Alert.DepositMatures>().single()
        assertTrue(matured.counts)
        val soon = Alerts.of(ledger, LocalDate.ofEpochDay(terms.maturityEpochDay - 5), 0, emptyList())
            .filterIsInstance<Alert.DepositMatures>().single()
        assertFalse(soon.counts)
        assertTrue(Alerts.of(ledger, LocalDate.ofEpochDay(terms.maturityEpochDay - 30), 0, emptyList()).none { it is Alert.DepositMatures })
        val shut = ledger.copy(accounts = listOf(fd.copy(deposit = terms.copy(closedEpochDay = terms.maturityEpochDay)), hdfc))
        assertTrue(Alerts.of(shut, LocalDate.ofEpochDay(terms.maturityEpochDay + 2), 0, emptyList()).none { it is Alert.DepositMatures })
    }

    @Test
    fun theOverallFloorLeavesLockedMoneyOut() {
        val ledger = Ledger(accounts = listOf(fd, hdfc))
        // ₹50,000 to spend is under ₹60,000, though the deposit holds ₹1,00,000 more.
        assertEquals(BigInteger.valueOf(5_000_000), Floors.below(ledger, OverallFloor("INR", 6_000_000)).single().balance)
        // Moving money into a deposit lowers what is left to spend.
        val funding = Transaction("t", "hdfc", TransactionKind.TRANSFER, 1_000_000, 1, destinationId = "fd")
        assertEquals(1, Floors.breaches(ledger, funding, null, OverallFloor("INR", 4_500_000)).size)
    }

    @Test
    fun paymentMessagesNeverLandInADeposit() {
        val digits = fd.copy(digits = "4821")
        assertEquals("hdfc", listOf(digits, hdfc).forPayment("INR", "4821")?.id)
    }
}
