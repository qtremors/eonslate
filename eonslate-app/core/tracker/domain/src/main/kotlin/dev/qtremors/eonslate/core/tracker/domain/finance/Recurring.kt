package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class PaymentInterval {
    @SerialName("weekly") WEEKLY,
    @SerialName("monthly") MONTHLY,
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
) {
    fun isValid(accountIds: Set<String>): Boolean = id.isNotBlank() && accountId in accountIds &&
        minor > 0 && note.length <= Transaction.NOTE_LIMIT && anchorDay in 1..31 &&
        (nextEpochDay == null || nextEpochDay in Transaction.DATES)

    fun transactionOn(epochDay: Long) =
        Transaction("recurring:${id.length}:$id:$epochDay", accountId, TransactionKind.EXPENSE, minor, epochDay, note)

    /** Keep the original day after a short month, for example Jan 31 → Feb 28 → Mar 31. */
    fun after(epochDay: Long): Long? {
        val date = LocalDate.ofEpochDay(epochDay)
        val next = when (interval) {
            PaymentInterval.WEEKLY -> date.plusWeeks(1)
            PaymentInterval.MONTHLY -> date.plusMonths(1).let { it.withDayOfMonth(minOf(anchorDay, it.lengthOfMonth())) }
        }
        return next.toEpochDay().takeIf { it in Transaction.DATES }
    }
}
