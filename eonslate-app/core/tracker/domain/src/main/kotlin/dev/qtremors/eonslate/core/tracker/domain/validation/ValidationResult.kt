package dev.qtremors.eonslate.core.tracker.domain.validation

/** The outcome of checking a tracker or an entry. [Invalid] lists every problem at once. */
sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(val errors: List<ValidationError>) : ValidationResult {
        init {
            require(errors.isNotEmpty()) { "Invalid needs at least one error" }
        }
    }

    val isValid: Boolean get() = this is Valid

    /** The errors, or an empty list when valid. */
    val errorList: List<ValidationError> get() = (this as? Invalid)?.errors.orEmpty()

    companion object {
        fun of(errors: List<ValidationError>): ValidationResult =
            if (errors.isEmpty()) Valid else Invalid(errors)
    }
}

/**
 * One problem with a tracker or an entry. Each carries what the UI needs to point at the cause
 * (usually a field id) and to word the message, but never the message itself, which belongs in
 * string resources.
 */
sealed interface ValidationError {
    // Tracker

    data object TrackerIdBlank : ValidationError
    data object TrackerNameBlank : ValidationError
    data class TrackerNameTooLong(val max: Int) : ValidationError
    data class DescriptionTooLong(val max: Int) : ValidationError
    data object IconNameBlank : ValidationError
    data class ColorIndexOutOfRange(val index: Int, val count: Int) : ValidationError
    data object NoFields : ValidationError
    data class TooManyFields(val max: Int) : ValidationError
    data object UpdatedBeforeCreated : ValidationError

    // Fields

    data object FieldIdBlank : ValidationError
    data class DuplicateFieldId(val fieldId: String) : ValidationError
    data class FieldTrackerMismatch(val fieldId: String) : ValidationError
    data class FieldNameBlank(val fieldId: String) : ValidationError
    data class FieldNameTooLong(val fieldId: String, val max: Int) : ValidationError
    data class DuplicateFieldName(val fieldId: String, val name: String) : ValidationError
    data class DecimalsOutOfRange(val fieldId: String, val max: Int) : ValidationError
    data class UnitTooLong(val fieldId: String, val max: Int) : ValidationError
    data class MinAboveMax(val fieldId: String) : ValidationError
    data class UnknownCurrency(val fieldId: String, val code: String) : ValidationError
    data class MaxDurationNotPositive(val fieldId: String) : ValidationError
    data class RatingScaleOutOfRange(val fieldId: String, val min: Int, val max: Int) : ValidationError
    data class NoOptions(val fieldId: String) : ValidationError
    data class TooManyOptions(val fieldId: String, val max: Int) : ValidationError
    data class OptionIdBlank(val fieldId: String) : ValidationError
    data class DuplicateOptionId(val fieldId: String, val optionId: String) : ValidationError
    data class OptionLabelBlank(val fieldId: String, val optionId: String) : ValidationError
    data class OptionLabelTooLong(val fieldId: String, val optionId: String, val max: Int) : ValidationError
    data class DuplicateOptionLabel(val fieldId: String, val label: String) : ValidationError
    data class TextLimitOutOfRange(val fieldId: String, val max: Int) : ValidationError

    // Entry

    data object EntryIdBlank : ValidationError
    data object TrackerNotFound : ValidationError
    data object EntryTrackerMismatch : ValidationError
    data object ZoneIdInvalid : ValidationError
    data class NoteTooLong(val max: Int) : ValidationError
    data object EntryEmpty : ValidationError
    data class UnknownField(val fieldId: String) : ValidationError
    data class RequiredFieldMissing(val fieldId: String) : ValidationError
    data class ValueTypeMismatch(val fieldId: String) : ValidationError
    data class BelowMin(val fieldId: String, val min: Long) : ValidationError
    data class AboveMax(val fieldId: String, val max: Long) : ValidationError
    data class NegativeDuration(val fieldId: String) : ValidationError
    data class RatingOutOfRange(val fieldId: String, val max: Int) : ValidationError
    data class UnknownOption(val fieldId: String, val optionId: String) : ValidationError
    data class TextBlank(val fieldId: String) : ValidationError
    data class TextTooLong(val fieldId: String, val max: Int) : ValidationError
    data class TextHasLineBreak(val fieldId: String) : ValidationError
    data class DateOutOfRange(val fieldId: String) : ValidationError
}
