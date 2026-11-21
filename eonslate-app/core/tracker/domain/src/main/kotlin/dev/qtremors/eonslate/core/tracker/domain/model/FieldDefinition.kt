package dev.qtremors.eonslate.core.tracker.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The kinds of value a tracker field can hold. [storageKey] is what the database stores. */
enum class FieldType(val storageKey: String) {
    NUMBER("number"),
    MONEY("money"),
    DURATION("duration"),
    RATING("rating"),
    YES_NO("yes_no"),
    SINGLE_CHOICE("single_choice"),
    MULTI_CHOICE("multi_choice"),
    TEXT("text"),
    DATE("date");

    companion object {
        private val byKey = entries.associateBy(FieldType::storageKey)

        /** The type stored as [key], or null for a key this version does not know. */
        fun fromStorageKey(key: String): FieldType? = byKey[key]
    }
}

/**
 * One column of a tracker: what it is called, what kind of value it holds, and the rules for
 * that value. The type always comes from [config], so the two can never disagree.
 */
data class FieldDefinition(
    val id: String,
    val trackerId: String,
    val name: String,
    val config: FieldConfig,
    val required: Boolean = false,
    val position: Int = 0,
) {
    val type: FieldType get() = config.type

    companion object {
        const val NAME_MAX_LENGTH = 40
    }
}

/** A choice in a single or multi choice field. [id] stays the same when the label is renamed. */
@Serializable
data class ChoiceOption(
    val id: String,
    val label: String,
) {
    companion object {
        const val LABEL_MAX_LENGTH = 40
    }
}

/**
 * The rules for one field type. Stored as JSON in the database, keyed by the `kind`
 * discriminator, so new optional properties can be added without a schema migration.
 */
@Serializable
sealed interface FieldConfig {
    val type: FieldType

    /**
     * A plain number. Values are stored scaled by 10^[decimals] as a whole number, so 12.5 kg
     * with one decimal is stored as 125. [min] and [max] are in the same scaled units.
     */
    @Serializable
    @SerialName("number")
    data class Number(
        val unit: String? = null,
        val decimals: Int = 0,
        val min: Long? = null,
        val max: Long? = null,
        /** A target for the day's or week's total, in the same scaled units. */
        val goal: Goal? = null,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.NUMBER

        companion object {
            const val MAX_DECIMALS = 6
            const val UNIT_MAX_LENGTH = 16
        }
    }

    /** An amount of money in one ISO 4217 currency, stored in its minor units (cents, paise). */
    @Serializable
    @SerialName("money")
    data class Money(
        val currencyCode: String,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.MONEY
    }

    /** A length of time, stored in whole seconds. */
    @Serializable
    @SerialName("duration")
    data class Duration(
        val maxSeconds: Long? = null,
        /** A target for the day's or week's total time, in seconds. */
        val goal: Goal? = null,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.DURATION
    }

    /** A score from 1 to [max], such as stars or a mood scale. */
    @Serializable
    @SerialName("rating")
    data class Rating(
        val max: Int = 5,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.RATING

        companion object {
            const val MIN_SCALE = 2
            const val MAX_SCALE = 10
        }
    }

    /** A yes or no answer, such as "Did I meditate?". */
    @Serializable
    @SerialName("yes_no")
    data object YesNo : FieldConfig {
        override val type: FieldType get() = FieldType.YES_NO
    }

    /** Exactly one of [options]. */
    @Serializable
    @SerialName("single_choice")
    data class SingleChoice(
        val options: List<ChoiceOption>,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.SINGLE_CHOICE
    }

    /** Any number of [options], including none. */
    @Serializable
    @SerialName("multi_choice")
    data class MultiChoice(
        val options: List<ChoiceOption>,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.MULTI_CHOICE
    }

    /** Free text of up to [maxLength] characters, on one line unless [multiline]. */
    @Serializable
    @SerialName("text")
    data class Text(
        val multiline: Boolean = false,
        val maxLength: Int = DEFAULT_MAX_LENGTH,
    ) : FieldConfig {
        override val type: FieldType get() = FieldType.TEXT

        companion object {
            const val DEFAULT_MAX_LENGTH = 500
            const val LIMIT = 10_000
        }
    }

    /** A calendar date without a time, stored as days since 1970-01-01. */
    @Serializable
    @SerialName("date")
    data object Date : FieldConfig {
        override val type: FieldType get() = FieldType.DATE
    }

    companion object {
        const val MAX_OPTIONS = 50
    }
}

/** The options of a choice field, or an empty list for every other type. */
val FieldConfig.choiceOptions: List<ChoiceOption>
    get() = when (this) {
        is FieldConfig.SingleChoice -> options
        is FieldConfig.MultiChoice -> options
        else -> emptyList()
    }

/** Whether a goal is a day's or a week's total. */
@Serializable
enum class GoalPeriod {
    @SerialName("day") DAY,
    @SerialName("week") WEEK,
}

/**
 * A target for a number or duration field: the total over [per] should reach [target], or stay
 * within it when [atMost], such as "at most 2 coffees a day". [target] is in the field's stored
 * units: scaled numbers, or seconds.
 */
@Serializable
data class Goal(
    val target: Long,
    val per: GoalPeriod = GoalPeriod.DAY,
    @SerialName("at_most") val atMost: Boolean = false,
) {
    fun met(total: Long): Boolean = if (atMost) total <= target else total >= target
}

/** The goal of a number or duration field; null for every other kind. */
val FieldConfig.goal: Goal?
    get() = when (this) {
        is FieldConfig.Number -> goal
        is FieldConfig.Duration -> goal
        else -> null
    }
