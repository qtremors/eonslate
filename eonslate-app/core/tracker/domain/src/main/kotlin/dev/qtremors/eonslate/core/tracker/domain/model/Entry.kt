package dev.qtremors.eonslate.core.tracker.domain.model

/**
 * One record logged to a tracker.
 *
 * @property occurredAt epoch milliseconds of when the thing happened, which can differ from
 * [createdAt] when the user logs it later.
 * @property zoneId the IANA time zone the user was in, so a 23:30 entry stays on the same day
 * after travelling.
 * @property values the filled fields, keyed by field id. Fields left blank are absent.
 */
data class Entry(
    val id: String,
    val trackerId: String,
    val occurredAt: Long,
    val zoneId: String,
    val note: String = "",
    val values: Map<String, FieldValue> = emptyMap(),
    val createdAt: Long,
    val updatedAt: Long = createdAt,
) {
    companion object {
        const val NOTE_MAX_LENGTH = 2_000
    }
}

/**
 * When an entry happened, without its values: enough to count entries per day or week without
 * reading every value of a long history.
 */
data class EntryTime(
    val occurredAt: Long,
    val zoneId: String,
)

/**
 * A value for one field, always held exactly. Nothing is a floating point number: decimals are
 * scaled whole numbers, money is minor units, durations are seconds and dates are epoch days.
 */
sealed interface FieldValue {
    val type: FieldType

    /** A number scaled by 10^decimals of its field, so 12.5 with one decimal is 125. */
    data class Number(val scaled: Long) : FieldValue {
        override val type: FieldType get() = FieldType.NUMBER
    }

    /** An amount in minor units of the field's currency. Negative amounts are allowed. */
    data class Money(val minor: Long) : FieldValue {
        override val type: FieldType get() = FieldType.MONEY
    }

    data class Duration(val seconds: Long) : FieldValue {
        override val type: FieldType get() = FieldType.DURATION
    }

    data class Rating(val value: Int) : FieldValue {
        override val type: FieldType get() = FieldType.RATING
    }

    data class YesNo(val value: Boolean) : FieldValue {
        override val type: FieldType get() = FieldType.YES_NO
    }

    data class SingleChoice(val optionId: String) : FieldValue {
        override val type: FieldType get() = FieldType.SINGLE_CHOICE
    }

    data class MultiChoice(val optionIds: Set<String>) : FieldValue {
        override val type: FieldType get() = FieldType.MULTI_CHOICE
    }

    data class Text(val text: String) : FieldValue {
        override val type: FieldType get() = FieldType.TEXT
    }

    /** Days since 1970-01-01, the same number `java.time.LocalDate.toEpochDay()` gives. */
    data class Date(val epochDay: Long) : FieldValue {
        override val type: FieldType get() = FieldType.DATE
    }
}
