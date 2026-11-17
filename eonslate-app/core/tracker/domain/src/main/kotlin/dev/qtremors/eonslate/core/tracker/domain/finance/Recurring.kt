package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class PaymentInterval {
    @SerialName("daily") DAILY,
    @SerialName("weekly") WEEKLY,
    @SerialName("monthly") MONTHLY,
    @SerialName("yearly") YEARLY,
}

@Serializable
data class RecurringPayment(
    val id: String,
    @SerialName("account_id") val accountId: String,
    val minor: Long,
    val note: String,
    val interval: PaymentInterval,
    @SerialName("anchor_day") val anchorDay: Int,
    @SerialName("next_epoch_day") val nextEpochDay: Long?,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val every: Int = 1,
    val paused: Boolean = false,
    val variable: Boolean = false,
    val category: String = "",
) {
    fun isValid(accountIds: Set<String>): Boolean = id.isNotBlank() && accountId in accountIds &&
        minor > 0 && note.length <= Transaction.NOTE_LIMIT && anchorDay in 1..31 &&
        kind != TransactionKind.TRANSFER && every in 1..MAX_INTERVAL && category.length <= Transaction.CATEGORY_LIMIT &&
        (nextEpochDay == null || nextEpochDay in Transaction.DATES)

    fun transactionOn(epochDay: Long) =
        Transaction("recurring:${id.length}:$id:$epochDay", accountId, kind, minor, epochDay, note, category = category)

    /** Keep the original day after a short month, for example Jan 31 → Feb 28 → Mar 31. */
    fun after(epochDay: Long): Long? {
        val date = LocalDate.ofEpochDay(epochDay)
        val next = interval.advance(date, every.toLong(), anchorDay)
        return next.toEpochDay().takeIf { it in Transaction.DATES }
    }

    companion object {
        const val MAX_INTERVAL = 365
    }
}

internal fun PaymentInterval.advance(date: LocalDate, count: Long, anchor: Int): LocalDate = when (this) {
    PaymentInterval.DAILY -> date.plusDays(count)
    PaymentInterval.WEEKLY -> date.plusWeeks(count)
    PaymentInterval.MONTHLY -> date.plusMonths(count).withAnchor(anchor)
    PaymentInterval.YEARLY -> date.plusYears(count).withAnchor(anchor)
}

private fun LocalDate.withAnchor(anchor: Int): LocalDate = withDayOfMonth(minOf(anchor, lengthOfMonth()))
