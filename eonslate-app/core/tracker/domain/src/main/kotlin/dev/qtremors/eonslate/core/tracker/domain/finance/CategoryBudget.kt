package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Serializable
data class CategoryBudget(
    val id: String,
    @SerialName("account_id") val accountId: String,
    val category: String,
    val minor: Long,
    val interval: PaymentInterval,
    val every: Int,
    @SerialName("start_epoch_day") val startEpochDay: Long,
    val name: String = "",
    val icon: String? = null,
    val color: Int? = null,
    /** Counts the category in every account in this budget's currency, not just [accountId]. */
    @SerialName("all_accounts") val allAccounts: Boolean = false,
) {
    val title: String get() = name.ifBlank { category }

    fun isValid(accountIds: Set<String>): Boolean =
        id.isNotBlank() && accountId in accountIds && category.isNotBlank() &&
            category.length <= Transaction.CATEGORY_LIMIT && minor > 0 &&
            every in 1..RecurringPayment.MAX_INTERVAL && startEpochDay in Transaction.DATES &&
            name.length <= NAME_LIMIT && (icon == null || icon.isNotBlank() && icon.length <= 64) &&
            (color == null || color in 0..0xFFFFFF)

    companion object {
        const val NAME_LIMIT = 500

        /** From this share of the limit spent, a budget is close to running out. */
        const val WARNING = 0.8f
    }

    fun periodOn(day: LocalDate): LongRange? {
        val anchor = LocalDate.ofEpochDay(startEpochDay)
        if (day < anchor) return null
        // Spending is counted by day, so an hourly budget lasts its hours rounded up to whole days.
        if (interval == PaymentInterval.HOURLY) {
            val days = (every + 23L) / 24
            val start = anchor.toEpochDay() + ChronoUnit.DAYS.between(anchor, day) / days * days
            return start until start + days
        }
        val elapsed = when (interval) {
            PaymentInterval.HOURLY -> error("Handled above")
            PaymentInterval.DAILY -> ChronoUnit.DAYS.between(anchor, day)
            PaymentInterval.WEEKLY -> ChronoUnit.WEEKS.between(anchor, day)
            PaymentInterval.MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(day))
            PaymentInterval.YEARLY -> (day.year - anchor.year).toLong()
        }
        val cycle = elapsed / every
        val candidate = interval.advance(anchor, cycle * every, anchor.dayOfMonth)
        val startCycle = if (candidate > day) cycle - 1 else cycle
        val start = interval.advance(anchor, startCycle * every, anchor.dayOfMonth)
        val end = interval.advance(anchor, (startCycle + 1) * every, anchor.dayOfMonth)
        return start.toEpochDay() until end.toEpochDay()
    }
}
