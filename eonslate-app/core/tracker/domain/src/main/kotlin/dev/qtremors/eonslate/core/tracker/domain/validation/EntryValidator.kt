package dev.qtremors.eonslate.core.tracker.domain.validation

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.choiceOptions
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import java.time.DateTimeException
import java.time.ZoneId

/**
 * Checks an [Entry] against the [Tracker] it belongs to: every value must match its field's
 * type and rules, required fields must be filled, and an entry must say something, either a
 * value or a note.
 */
object EntryValidator {

    fun validate(entry: Entry, tracker: Tracker): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        if (entry.id.isBlank()) errors += ValidationError.EntryIdBlank
        if (entry.trackerId != tracker.id) errors += ValidationError.EntryTrackerMismatch
        if (!isValidZone(entry.zoneId)) errors += ValidationError.ZoneIdInvalid
        if (entry.note.length > Entry.NOTE_MAX_LENGTH) errors += ValidationError.NoteTooLong(Entry.NOTE_MAX_LENGTH)
        if (entry.updatedAt < entry.createdAt) errors += ValidationError.UpdatedBeforeCreated
        if (entry.values.isEmpty() && entry.note.isBlank()) errors += ValidationError.EntryEmpty

        entry.values.forEach { (fieldId, value) ->
            val field = tracker.field(fieldId)
            if (field == null) {
                errors += ValidationError.UnknownField(fieldId)
            } else {
                errors += validateValue(field, value)
            }
        }
        tracker.fields
            .filter { it.required && it.id !in entry.values }
            .forEach { errors += ValidationError.RequiredFieldMissing(it.id) }

        return ValidationResult.of(errors)
    }

    /** The problems with [value] for [field], or an empty list when it fits. */
    fun validateValue(field: FieldDefinition, value: FieldValue): List<ValidationError> {
        val id = field.id
        if (value.type != field.type) return listOf(ValidationError.ValueTypeMismatch(id))
        val errors = mutableListOf<ValidationError>()
        when (val config = field.config) {
            is FieldConfig.Number -> {
                val scaled = (value as FieldValue.Number).scaled
                if (config.min != null && scaled < config.min) errors += ValidationError.BelowMin(id, config.min)
                if (config.max != null && scaled > config.max) errors += ValidationError.AboveMax(id, config.max)
            }
            is FieldConfig.Money -> Unit
            is FieldConfig.Duration -> {
                val seconds = (value as FieldValue.Duration).seconds
                if (seconds < 0) errors += ValidationError.NegativeDuration(id)
                if (config.maxSeconds != null && seconds > config.maxSeconds) {
                    errors += ValidationError.AboveMax(id, config.maxSeconds)
                }
            }
            is FieldConfig.Rating -> if ((value as FieldValue.Rating).value !in 1..config.max) {
                errors += ValidationError.RatingOutOfRange(id, config.max)
            }
            FieldConfig.YesNo -> Unit
            is FieldConfig.SingleChoice -> {
                val optionId = (value as FieldValue.SingleChoice).optionId
                if (config.choiceOptions.none { it.id == optionId }) errors += ValidationError.UnknownOption(id, optionId)
            }
            is FieldConfig.MultiChoice -> {
                val known = config.choiceOptions.mapTo(mutableSetOf()) { it.id }
                (value as FieldValue.MultiChoice).optionIds
                    .filterNot(known::contains)
                    .sorted()
                    .forEach { errors += ValidationError.UnknownOption(id, it) }
            }
            is FieldConfig.Text -> {
                val text = (value as FieldValue.Text).text
                if (text.isBlank()) errors += ValidationError.TextBlank(id)
                if (text.length > config.maxLength) errors += ValidationError.TextTooLong(id, config.maxLength)
                if (!config.multiline && text.any { it == '\n' || it == '\r' }) errors += ValidationError.TextHasLineBreak(id)
            }
            FieldConfig.Date -> if (!EpochDays.isValid((value as FieldValue.Date).epochDay)) {
                errors += ValidationError.DateOutOfRange(id)
            }
        }
        return errors
    }

    private fun isValidZone(zoneId: String): Boolean = try {
        ZoneId.of(zoneId)
        true
    } catch (_: DateTimeException) {
        false
    }
}
