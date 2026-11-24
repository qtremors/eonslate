package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How often a deposit's interest joins it and starts earning too. [SIMPLE] pays on the principal alone. */
@Serializable
enum class Compounding(val storageKey: String, val perYear: Int) {
    @SerialName("monthly") MONTHLY("monthly", 12),
    @SerialName("quarterly") QUARTERLY("quarterly", 4),
    @SerialName("half_yearly") HALF_YEARLY("half_yearly", 2),
    @SerialName("yearly") YEARLY("yearly", 1),
    @SerialName("simple") SIMPLE("simple", 0);

    companion object {
        fun fromKey(key: String?): Compounding = entries.firstOrNull { it.storageKey == key } ?: QUARTERLY
    }
}

/**
 * A fixed deposit's terms, kept on its account. The account holds the principal; interest is
 * worked out from these terms and recorded once, when the deposit is closed.
 */
@Serializable
data class Deposit(
    @SerialName("principal_minor") val principalMinor: Long,
    /** The yearly rate in hundredths of a percent: 7.10% is 710. */
    @SerialName("rate_bp") val rateBp: Int,
    @SerialName("start_epoch_day") val startEpochDay: Long,
    @SerialName("maturity_epoch_day") val maturityEpochDay: Long,
    val compounding: Compounding = Compounding.QUARTERLY,
    /** The day it was paid out; null while it runs. */
    @SerialName("closed_epoch_day") val closedEpochDay: Long? = null,
) {
    val isOpen: Boolean get() = closedEpochDay == null

    fun isValid(): Boolean =
        principalMinor > 0 && rateBp in 1..MAX_RATE_BP &&
            startEpochDay in Transaction.DATES && maturityEpochDay in Transaction.DATES && maturityEpochDay > startEpochDay &&
            (closedEpochDay == null || closedEpochDay in startEpochDay..Transaction.DATES.last)

    companion object {
        /** 50% a year; anything above is a typo. */
        const val MAX_RATE_BP = 5_000
    }
}

/** Where a running or closed deposit stands on a day. */
data class DepositStatus(
    val account: Account,
    val deposit: Deposit,
    /** What its account holds now: the principal until it is closed, nothing after. */
    val balance: BigInteger,
    /** What it is worth on the day asked about, principal and interest so far; once closed, what it paid out. */
    val worth: BigInteger,
    /** What it pays out at maturity. */
    val atMaturity: BigInteger,
    /** Days to maturity; zero or less once it has matured. */
    val daysLeft: Long,
) {
    val principal: BigInteger get() = BigInteger.valueOf(deposit.principalMinor)
    val earned: BigInteger get() = worth - principal
    val matured: Boolean get() = daysLeft <= 0
    val open: Boolean get() = deposit.isOpen

    /** How far through its term it is, from 0 to 1. */
    val fraction: Float
        get() {
            val term = deposit.maturityEpochDay - deposit.startEpochDay
            return ((term - daysLeft).toDouble() / term).coerceIn(0.0, 1.0).toFloat()
        }
}

object Deposits {
    private val MATH = MathContext.DECIMAL128
    private val DAYS_A_YEAR = BigDecimal(365)
    private val BASIS = BigDecimal(10_000)

    /**
     * What [deposit] is worth on [day], rounded to the minor unit. Nothing is earned before it
     * starts and nothing after it matures. Interest compounds over each whole period from the
     * start; days past the last whole period earn simple interest on the compounded amount, the
     * way banks count a part period.
     */
    fun valueOn(deposit: Deposit, day: Long): BigInteger {
        val start = LocalDate.ofEpochDay(deposit.startEpochDay)
        val end = LocalDate.ofEpochDay(day.coerceIn(deposit.startEpochDay, deposit.maturityEpochDay))
        val rate = BigDecimal(deposit.rateBp).divide(BASIS, MATH)
        val principal = BigDecimal(deposit.principalMinor)
        val perYear = deposit.compounding.perYear
        val amount = if (perYear == 0) {
            principal.multiply(BigDecimal.ONE + part(rate, ChronoUnit.DAYS.between(start, end)), MATH)
        } else {
            val months = 12 / perYear
            var periods = ChronoUnit.MONTHS.between(start, end) / months
            // Month ends: 31 Jan plus a month is 28 Feb, which a plain month count misses.
            if (!start.plusMonths((periods + 1) * months).isAfter(end)) periods++
            val lastPeriod = start.plusMonths(periods * months)
            val grown = (BigDecimal.ONE + rate.divide(BigDecimal(perYear), MATH)).pow(periods.toInt(), MATH)
            principal.multiply(grown, MATH).multiply(BigDecimal.ONE + part(rate, ChronoUnit.DAYS.between(lastPeriod, end)), MATH)
        }
        return amount.setScale(0, RoundingMode.HALF_UP).toBigIntegerExact()
    }

    private fun part(rate: BigDecimal, days: Long): BigDecimal = rate.multiply(BigDecimal(days), MATH).divide(DAYS_A_YEAR, MATH)

    /** Every deposit in [ledger] on [day]: running ones by maturity, then closed ones, latest first. */
    fun status(ledger: Ledger, day: Long): List<DepositStatus> {
        val balances = ledger.balances()
        // A closed deposit is worth what actually came out of it, whatever the terms said.
        val paidOut = ledger.transactions.filter { it.kind == TransactionKind.TRANSFER }
            .groupBy { it.accountId }
            .mapValues { (_, out) -> out.fold(BigInteger.ZERO) { sum, t -> sum + BigInteger.valueOf(t.minor) } }
        return ledger.accounts.mapNotNull { account ->
            val deposit = account.deposit ?: return@mapNotNull null
            val closed = deposit.closedEpochDay
            DepositStatus(
                account, deposit,
                balance = balances[account.id] ?: BigInteger.ZERO,
                worth = if (closed == null) valueOn(deposit, day) else paidOut[account.id] ?: valueOn(deposit, closed),
                atMaturity = valueOn(deposit, deposit.maturityEpochDay),
                daysLeft = deposit.maturityEpochDay - day,
            )
        }.sortedWith(
            compareBy<DepositStatus> { !it.open }
                .thenBy { if (it.open) it.deposit.maturityEpochDay else -(it.deposit.closedEpochDay ?: 0) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.account.name },
        )
    }

    /**
     * A new deposit [account] and what funds it. Paid [from] another account, it starts empty
     * and a transfer of the principal on its first day fills it; otherwise the principal is its
     * opening balance, as for money already there before the app knew of it.
     */
    fun opening(account: Account, from: Account?, transferId: String): Pair<Account, List<Transaction>> {
        val deposit = requireNotNull(account.deposit)
        if (from == null) return account.copy(openingMinor = deposit.principalMinor) to emptyList()
        val transfer = Transaction(
            transferId, from.id, TransactionKind.TRANSFER, deposit.principalMinor, deposit.startEpochDay,
            destinationId = account.id,
        )
        return account.copy(openingMinor = 0) to listOf(transfer)
    }

    /**
     * What closes [status]'s deposit on [day]: [received] paid [into] an account, with the gap
     * from what it holds recorded on the deposit first, as interest when more came back and as
     * charges when less did. Null when it cannot close that way.
     */
    fun closing(
        status: DepositStatus,
        received: Long,
        into: Account,
        day: Long,
        ids: () -> String,
        interestNote: String,
        chargesNote: String,
    ): Pair<Account, List<Transaction>>? {
        val account = status.account
        val deposit = status.deposit
        if (!deposit.isOpen || received <= 0 || into.id == account.id || into.currency != account.currency ||
            into.deposit != null || day < deposit.startEpochDay || day !in Transaction.DATES
        ) return null
        val gap = BigInteger.valueOf(received) - status.balance
        if (gap.bitLength() >= Long.SIZE_BITS) return null
        val settle = when (gap.signum()) {
            1 -> Transaction(ids(), account.id, TransactionKind.INCOME, gap.toLong(), day, note = interestNote)
            -1 -> Transaction(ids(), account.id, TransactionKind.EXPENSE, gap.negate().toLong(), day, note = chargesNote)
            else -> null
        }
        val payout = Transaction(ids(), account.id, TransactionKind.TRANSFER, received, day, destinationId = into.id)
        return account.copy(deposit = deposit.copy(closedEpochDay = day)) to listOfNotNull(settle, payout)
    }
}
