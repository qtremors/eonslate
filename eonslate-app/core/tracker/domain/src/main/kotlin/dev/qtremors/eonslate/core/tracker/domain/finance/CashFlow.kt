package dev.qtremors.eonslate.core.tracker.domain.finance

import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

/** How far back Home's cash flow looks. */
enum class CashFlowRange { WEEK, MONTH, HALF_YEAR, YEAR, ALL }

/** One bar of the cash flow: a day, month or year, with what came in, went out, and where most of it went. */
data class CashFlowBucket(
    val days: LongRange,
    val income: BigInteger,
    val spending: BigInteger,
    /** The category with the most spending; "" for uncategorized, null when nothing was spent. */
    val topCategory: String?,
    val topAmount: BigInteger,
) {
    val net: BigInteger get() = income - spending
}

/** Income and spending over a [range], split into [buckets] oldest first. Transfers are left out. */
data class CashFlow(val range: CashFlowRange, val step: Step, val buckets: List<CashFlowBucket>) {
    val income: BigInteger get() = buckets.fold(BigInteger.ZERO) { sum, it -> sum + it.income }
    val spending: BigInteger get() = buckets.fold(BigInteger.ZERO) { sum, it -> sum + it.spending }
    val net: BigInteger get() = income - spending

    /** How long each bucket is: days for the week and month, months for longer ranges, years for a long history. */
    enum class Step { DAY, MONTH, YEAR }

    companion object {
        /** All time shows months up to two years, then whole years. */
        const val MAX_MONTHS = 24

        fun of(transactions: List<Transaction>, range: CashFlowRange, today: LocalDate): CashFlow {
            val counted = transactions.filter { it.kind != TransactionKind.TRANSFER }
            val month = YearMonth.from(today)
            val (step, periods) = when (range) {
                CashFlowRange.WEEK -> Step.DAY to days(today, 7)
                CashFlowRange.MONTH -> Step.DAY to days(today, 30)
                CashFlowRange.HALF_YEAR -> Step.MONTH to months(month, 6)
                CashFlowRange.YEAR -> Step.MONTH to months(month, 12)
                CashFlowRange.ALL -> {
                    val first = counted.minOfOrNull { it.epochDay }?.let { YearMonth.from(LocalDate.ofEpochDay(it)) } ?: month
                    val span = (month.year - first.year) * 12 + month.monthValue - first.monthValue + 1
                    if (span <= MAX_MONTHS) Step.MONTH to months(month, span.coerceAtLeast(1))
                    else Step.YEAR to years(first.year, today.year)
                }
            }
            val end = today.toEpochDay()
            return CashFlow(range, step, periods.map { days -> bucket(counted, days.first..minOf(days.last, end)) })
        }

        private fun days(today: LocalDate, count: Int): List<LongRange> =
            (count - 1 downTo 0).map { back -> today.minusDays(back.toLong()).toEpochDay().let { it..it } }

        private fun months(current: YearMonth, count: Int): List<LongRange> =
            (count - 1 downTo 0).map { back ->
                val month = current.minusMonths(back.toLong())
                month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()
            }

        private fun years(first: Int, last: Int): List<LongRange> =
            (first..last).map { LocalDate.of(it, 1, 1).toEpochDay()..LocalDate.of(it, 12, 31).toEpochDay() }

        private fun bucket(transactions: List<Transaction>, days: LongRange): CashFlowBucket {
            var income = BigInteger.ZERO
            var spending = BigInteger.ZERO
            val byCategory = mutableMapOf<String, BigInteger>()
            for (t in transactions) {
                if (t.epochDay !in days) continue
                val amount = BigInteger.valueOf(t.minor)
                if (t.kind == TransactionKind.INCOME) income += amount
                else {
                    spending += amount
                    val key = t.category.trim()
                    byCategory[key] = byCategory.getOrDefault(key, BigInteger.ZERO) + amount
                }
            }
            val top = byCategory.maxByOrNull { it.value }
            return CashFlowBucket(days, income, spending, top?.key, top?.value ?: BigInteger.ZERO)
        }
    }
}
