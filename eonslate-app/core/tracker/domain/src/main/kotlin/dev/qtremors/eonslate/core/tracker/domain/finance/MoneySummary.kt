package dev.qtremors.eonslate.core.tracker.domain.finance

import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

/** What a recurring payment costs in an average month, in minor units. */
fun RecurringPayment.monthlyMinor(): Double {
    val perMonth = when (interval) {
        PaymentInterval.HOURLY -> 30.0 * 24
        PaymentInterval.DAILY -> 365.25 / 12
        PaymentInterval.WEEKLY -> 365.25 / 12 / 7
        PaymentInterval.MONTHLY -> 1.0
        PaymentInterval.YEARLY -> 1.0 / 12
    }
    return minor * perMonth / every.coerceAtLeast(1)
}

/** A budget measured on one day: what was spent in its current period against its limit. */
data class BudgetHealth(val budget: CategoryBudget, val spent: BigInteger, val limit: BigInteger) {
    val over: Boolean get() = spent > limit
    val fraction: Float get() = if (limit.signum() == 0) 0f else spent.toFloat() / limit.toFloat()
}

/**
 * Everything the money overview shows, in one [currency]: accounts in other currencies are left
 * out of the totals rather than mixed in. Amounts are minor units.
 */
data class MoneySummary(
    val currency: String,
    val balance: BigInteger,
    val monthIncome: BigInteger,
    val monthExpense: BigInteger,
    /** Expenses per month, oldest first, ending with [today]'s month. */
    val trend: List<Pair<YearMonth, BigInteger>>,
    /** This month's expenses by category, largest first; blank categories are "". */
    val categories: List<Pair<String, BigInteger>>,
    val budgets: List<BudgetHealth>,
    /** Payments due from today through the next week, soonest first. */
    val dueSoon: List<RecurringPayment>,
    val recent: List<Transaction>,
) {
    val monthNet: BigInteger get() = monthIncome - monthExpense

    companion object {
        const val TREND_MONTHS = 6
        const val RECENT = 5
        const val DUE_DAYS = 7

        /** The summary in [currency], or in the first account's currency when no account uses it. */
        fun of(ledger: Ledger, today: LocalDate, currency: String): MoneySummary? {
            val code = ledger.accounts.firstOrNull { it.currency == currency }?.currency
                ?: ledger.accounts.firstOrNull()?.currency ?: return null
            val ids = ledger.accounts.filter { it.currency == code }.mapTo(HashSet()) { it.id }
            val mine = ledger.transactions.filter { it.accountId in ids }
            val balances = ledger.balances()
            val month = YearMonth.from(today)
            fun inMonth(t: Transaction, m: YearMonth) = LocalDate.ofEpochDay(t.epochDay).let { YearMonth.from(it) == m }
            fun sum(list: Sequence<Transaction>) = list.fold(BigInteger.ZERO) { acc, t -> acc + BigInteger.valueOf(t.minor) }
            val thisMonth = mine.asSequence().filter { inMonth(it, month) }
            return MoneySummary(
                currency = code,
                balance = ids.fold(BigInteger.ZERO) { acc, id -> acc + (balances[id] ?: BigInteger.ZERO) },
                monthIncome = sum(thisMonth.filter { it.kind == TransactionKind.INCOME }),
                monthExpense = sum(thisMonth.filter { it.kind == TransactionKind.EXPENSE }),
                trend = (TREND_MONTHS - 1 downTo 0).map { back ->
                    val m = month.minusMonths(back.toLong())
                    m to sum(mine.asSequence().filter { it.kind == TransactionKind.EXPENSE && inMonth(it, m) })
                },
                categories = thisMonth.filter { it.kind == TransactionKind.EXPENSE }
                    .groupBy { it.category.trim() }
                    .map { (name, list) -> name to sum(list.asSequence()) }
                    .sortedByDescending { it.second },
                budgets = ledger.budgets.filter { it.accountId in ids }.map {
                    BudgetHealth(it, ledger.spendingFor(it, today), BigInteger.valueOf(it.minor))
                }.sortedByDescending { it.fraction },
                dueSoon = ledger.recurring.filter {
                    !it.paused && it.accountId in ids && it.nextEpochDay != null &&
                        it.nextEpochDay in today.toEpochDay()..today.toEpochDay() + DUE_DAYS
                }.sortedBy { it.nextEpochDay },
                recent = mine.sortedWith(compareByDescending<Transaction> { it.epochDay }.thenByDescending { it.id }).take(RECENT),
            )
        }
    }
}
