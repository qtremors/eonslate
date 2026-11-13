package dev.qtremors.eonslate.core.tracker.domain.validation

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount

/**
 * Checks a [Tracker] and its fields before they are saved. Names are compared trimmed and
 * case-insensitively, so "Weight" and "weight " count as the same field.
 */
object TrackerValidator {

    fun validate(tracker: Tracker): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val name = tracker.name.trim()

        if (tracker.id.isBlank()) errors += ValidationError.TrackerIdBlank
        if (name.isEmpty()) errors += ValidationError.TrackerNameBlank
        if (name.length > Tracker.NAME_MAX_LENGTH) errors += ValidationError.TrackerNameTooLong(Tracker.NAME_MAX_LENGTH)
        if (tracker.description.trim().length > Tracker.DESCRIPTION_MAX_LENGTH) {
            errors += ValidationError.DescriptionTooLong(Tracker.DESCRIPTION_MAX_LENGTH)
        }
        if (tracker.iconName.isBlank()) errors += ValidationError.IconNameBlank
        if (!TrackerColors.isValid(tracker.colorIndex)) {
            errors += ValidationError.ColorIndexOutOfRange(tracker.colorIndex, TrackerColors.COUNT)
        }
        if (tracker.updatedAt < tracker.createdAt) errors += ValidationError.UpdatedBeforeCreated

        when {
            tracker.fields.isEmpty() -> errors += ValidationError.NoFields
            tracker.fields.size > Tracker.MAX_FIELDS -> errors += ValidationError.TooManyFields(Tracker.MAX_FIELDS)
        }

        val seenIds = mutableSetOf<String>()
        val seenNames = mutableSetOf<String>()
        tracker.fields.forEach { field ->
            if (field.id.isBlank()) {
                errors += ValidationError.FieldIdBlank
            } else if (!seenIds.add(field.id)) {
                errors += ValidationError.DuplicateFieldId(field.id)
            }
            if (field.trackerId != tracker.id) errors += ValidationError.FieldTrackerMismatch(field.id)
            val fieldName = field.name.trim()
            if (fieldName.isNotEmpty() && !seenNames.add(fieldName.lowercase())) {
                errors += ValidationError.DuplicateFieldName(field.id, fieldName)
            }
            errors += validateField(field)
        }
        return ValidationResult.of(errors)
    }

    /** The problems with one field on its own, without looking at its siblings. */
    fun validateField(field: FieldDefinition): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val id = field.id
        val name = field.name.trim()
        if (name.isEmpty()) errors += ValidationError.FieldNameBlank(id)
        if (name.length > FieldDefinition.NAME_MAX_LENGTH) {
            errors += ValidationError.FieldNameTooLong(id, FieldDefinition.NAME_MAX_LENGTH)
        }

        when (val config = field.config) {
            is FieldConfig.Number -> {
                if (config.decimals !in 0..FieldConfig.Number.MAX_DECIMALS) {
                    errors += ValidationError.DecimalsOutOfRange(id, FieldConfig.Number.MAX_DECIMALS)
                }
                if ((config.unit?.trim()?.length ?: 0) > FieldConfig.Number.UNIT_MAX_LENGTH) {
                    errors += ValidationError.UnitTooLong(id, FieldConfig.Number.UNIT_MAX_LENGTH)
                }
                if (config.min != null && config.max != null && config.min > config.max) {
                    errors += ValidationError.MinAboveMax(id)
                }
            }
            is FieldConfig.Money -> if (!MoneyAmount.isKnownCurrency(config.currencyCode)) {
                errors += ValidationError.UnknownCurrency(id, config.currencyCode)
            }
            is FieldConfig.Duration -> if (config.maxSeconds != null && config.maxSeconds <= 0) {
                errors += ValidationError.MaxDurationNotPositive(id)
            }
            is FieldConfig.Rating -> if (config.max !in FieldConfig.Rating.MIN_SCALE..FieldConfig.Rating.MAX_SCALE) {
                errors += ValidationError.RatingScaleOutOfRange(id, FieldConfig.Rating.MIN_SCALE, FieldConfig.Rating.MAX_SCALE)
            }
            is FieldConfig.SingleChoice -> errors += validateOptions(id, config.options)
            is FieldConfig.MultiChoice -> errors += validateOptions(id, config.options)
            is FieldConfig.Text -> if (config.maxLength !in 1..FieldConfig.Text.LIMIT) {
                errors += ValidationError.TextLimitOutOfRange(id, FieldConfig.Text.LIMIT)
            }
            FieldConfig.YesNo, FieldConfig.Date -> Unit
        }
        return errors
    }

    private fun validateOptions(fieldId: String, options: List<ChoiceOption>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        when {
            options.isEmpty() -> errors += ValidationError.NoOptions(fieldId)
            options.size > FieldConfig.MAX_OPTIONS -> errors += ValidationError.TooManyOptions(fieldId, FieldConfig.MAX_OPTIONS)
        }
        val ids = mutableSetOf<String>()
        val labels = mutableSetOf<String>()
        options.forEach { option ->
            if (option.id.isBlank()) {
                errors += ValidationError.OptionIdBlank(fieldId)
            } else if (!ids.add(option.id)) {
                errors += ValidationError.DuplicateOptionId(fieldId, option.id)
            }
            val label = option.label.trim()
            when {
                label.isEmpty() -> errors += ValidationError.OptionLabelBlank(fieldId, option.id)
                label.length > ChoiceOption.LABEL_MAX_LENGTH ->
                    errors += ValidationError.OptionLabelTooLong(fieldId, option.id, ChoiceOption.LABEL_MAX_LENGTH)
                !labels.add(label.lowercase()) -> errors += ValidationError.DuplicateOptionLabel(fieldId, label)
            }
        }
        return errors
    }
}
