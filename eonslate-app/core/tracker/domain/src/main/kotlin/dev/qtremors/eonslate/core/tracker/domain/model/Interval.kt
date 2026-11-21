package dev.qtremors.eonslate.core.tracker.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The unit of an interval's time. */
@Serializable
enum class IntervalUnit {
    @SerialName("day") DAY,
    @SerialName("week") WEEK,
    @SerialName("month") MONTH,
    @SerialName("year") YEAR,
}

/**
 * When something is due again after it was last logged: after a time, after a number field has
 * grown by an amount (an odometer), or whichever comes first when both are set.
 *
 * @property choiceFieldId with [optionId], only logs that picked this option count, so one
 *   tracker can hold several jobs on their own clocks (an oil change, a battery). Null: any log.
 * @property every with [unit], the time after the last log; null when not timed.
 * @property amountFieldId with [amount], a number field and how much it grows before it is due,
 *   in the field's stored units. The latest value of the field in any entry is the current one.
 */
@Serializable
data class Interval(
    val id: String,
    @SerialName("choice_field_id") val choiceFieldId: String? = null,
    @SerialName("option_id") val optionId: String? = null,
    val every: Int? = null,
    val unit: IntervalUnit = IntervalUnit.DAY,
    @SerialName("amount_field_id") val amountFieldId: String? = null,
    val amount: Long? = null,
) {
    val timed: Boolean get() = every != null
    val metered: Boolean get() = amountFieldId != null && amount != null

    /** Readable against [fields]: something to wait for, positive, and pointing at real fields. */
    fun isValid(fields: List<FieldDefinition>): Boolean {
        if (!timed && !metered) return false
        if (every != null && every !in 1..MAX_EVERY) return false
        if (amount != null && amount <= 0) return false
        if ((choiceFieldId == null) != (optionId == null)) return false
        if (choiceFieldId != null) {
            val config = fields.firstOrNull { it.id == choiceFieldId }?.config as? FieldConfig.SingleChoice ?: return false
            if (config.options.none { it.id == optionId }) return false
        }
        if (amountFieldId != null && fields.firstOrNull { it.id == amountFieldId }?.config !is FieldConfig.Number) return false
        return true
    }

    companion object {
        const val MAX_EVERY = 1000
        const val MAX_COUNT = 12
    }
}
