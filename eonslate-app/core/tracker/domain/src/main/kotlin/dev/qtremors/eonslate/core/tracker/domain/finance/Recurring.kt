package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Serializable
enum class PaymentInterval {
    @SerialName("hourly") HOURLY,
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
    /** When it began; set from the first payment and kept through edits. */
    @SerialName("start_epoch_day") val startEpochDay: Long? = null,
    /** Notes beside the name. */
    val details: String = "",
    /** A service id ("service:netflix") or a custom image ("file:<hash>"); null shows a letter tile. */
    val icon: String? = null,
    val color: Int? = null,
    @SerialName("next_minute") val nextMinute: Int? = null,
) {
    val nextOccurrence: LocalDateTime?
        get() = nextEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay().plusMinutes((nextMinute ?: 0).toLong()) }

    internal val transactionIdPrefix: String get() = "recurring:${id.length}:$id:"

    fun isValid(accountIds: Set<String>): Boolean = id.isNotBlank() && accountId in accountIds &&
        minor > 0 && note.length <= Transaction.NOTE_LIMIT && anchorDay in 1..31 &&
        kind != TransactionKind.TRANSFER && every in 1..MAX_INTERVAL && category.length <= Transaction.CATEGORY_LIMIT &&
        (nextEpochDay == null || nextEpochDay in Transaction.DATES) &&
        (startEpochDay == null || startEpochDay in Transaction.DATES) && details.length <= DETAILS_LIMIT &&
        (color == null || color in 0..0xFFFFFF) &&
        if (interval == PaymentInterval.HOURLY && nextEpochDay != null) nextMinute != null && nextMinute in 0..1439
        else nextMinute == null

    fun transactionOn(epochDay: Long, minute: Int? = null): Transaction {
        val occurrence = if (minute == null) "$epochDay" else "$epochDay:$minute"
        return Transaction(transactionIdPrefix + occurrence, accountId, kind, minor, epochDay, note, category = category)
    }

    /** Keep the original day after a short month, for example Jan 31 → Feb 28 → Mar 31. */
    fun after(epochDay: Long): Long? =
        after(LocalDate.ofEpochDay(epochDay).atStartOfDay())?.toLocalDate()?.toEpochDay()

    fun after(occurrence: LocalDateTime, postedThrough: LocalDateTime = occurrence): LocalDateTime? {
        val next = if (interval == PaymentInterval.HOURLY) {
            // As in the legacy ledger, automatic postings cover at most one charge per day.
            val limit = minOf(postedThrough, occurrence.toLocalDate().atTime(LocalTime.MAX))
            val steps = ChronoUnit.HOURS.between(occurrence, limit) / every + 1
            occurrence.plusHours(steps * every)
        } else interval.advance(occurrence.toLocalDate(), every.toLong(), anchorDay).atStartOfDay()
        return next.takeIf { it.toLocalDate().toEpochDay() in Transaction.DATES }
    }

    companion object {
        const val MAX_INTERVAL = 10_000
        const val DETAILS_LIMIT = 500
    }
}

internal fun PaymentInterval.advance(date: LocalDate, count: Long, anchor: Int): LocalDate = when (this) {
    PaymentInterval.HOURLY -> date.plusDays((count + 23) / 24)
    PaymentInterval.DAILY -> date.plusDays(count)
    PaymentInterval.WEEKLY -> date.plusWeeks(count)
    PaymentInterval.MONTHLY -> date.plusMonths(count).withAnchor(anchor)
    PaymentInterval.YEARLY -> date.plusYears(count).withAnchor(anchor)
}

private fun LocalDate.withAnchor(anchor: Int): LocalDate = withDayOfMonth(minOf(anchor, lengthOfMonth()))
