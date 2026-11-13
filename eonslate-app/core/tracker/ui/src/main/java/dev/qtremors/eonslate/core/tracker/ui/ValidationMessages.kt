package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError as E

/**
 * The sentence for a validation problem. Messages stand on their own because each is shown
 * under the input that caused it, so they never need to repeat the field's name. The `when` has
 * no else branch on purpose: a new error does not compile until it has words.
 */
@Composable
fun ValidationError.message(): String = when (this) {
    // Problems the screens never let a person cause; seeing one means a bug, not a typo.
    E.TrackerIdBlank, E.FieldIdBlank, E.EntryIdBlank, is E.OptionIdBlank,
    is E.FieldTrackerMismatch, E.EntryTrackerMismatch, is E.DuplicateFieldId, is E.DuplicateOptionId,
    E.UpdatedBeforeCreated, E.ZoneIdInvalid, E.IconNameBlank, is E.ColorIndexOutOfRange,
    is E.UnknownField, is E.ValueTypeMismatch ->
        stringResource(R.string.error_internal)
    E.TrackerNotFound -> stringResource(R.string.error_tracker_not_found)
    E.TrackerNameBlank -> stringResource(R.string.error_tracker_name_blank)
    is E.TrackerNameTooLong -> stringResource(R.string.error_too_long, max)
    is E.DescriptionTooLong -> stringResource(R.string.error_too_long, max)
    E.NoFields -> stringResource(R.string.error_no_fields)
    is E.TooManyFields -> stringResource(R.string.error_too_many_fields, max)
    is E.FieldNameBlank -> stringResource(R.string.error_field_name_blank)
    is E.FieldNameTooLong -> stringResource(R.string.error_too_long, max)
    is E.DuplicateFieldName -> stringResource(R.string.error_duplicate_field_name)
    is E.DecimalsOutOfRange -> stringResource(R.string.error_decimals, max)
    is E.UnitTooLong -> stringResource(R.string.error_too_long, max)
    is E.MinAboveMax -> stringResource(R.string.error_min_above_max)
    is E.UnknownCurrency -> stringResource(R.string.error_unknown_currency)
    is E.MaxDurationNotPositive -> stringResource(R.string.error_max_duration)
    is E.RatingScaleOutOfRange -> stringResource(R.string.error_rating_scale, min, max)
    is E.NoOptions -> stringResource(R.string.error_no_options)
    is E.TooManyOptions -> stringResource(R.string.error_too_many_options, max)
    is E.OptionLabelBlank -> stringResource(R.string.error_option_blank)
    is E.OptionLabelTooLong -> stringResource(R.string.error_too_long, max)
    is E.DuplicateOptionLabel -> stringResource(R.string.error_duplicate_option)
    is E.TextLimitOutOfRange -> stringResource(R.string.error_text_limit, max)
    is E.NoteTooLong -> stringResource(R.string.error_too_long, max)
    E.EntryEmpty -> stringResource(R.string.error_entry_empty)
    is E.RequiredFieldMissing -> stringResource(R.string.error_required)
    is E.BelowMin -> stringResource(R.string.error_below_min)
    is E.AboveMax -> stringResource(R.string.error_above_max)
    is E.NegativeDuration -> stringResource(R.string.error_negative_duration)
    is E.RatingOutOfRange -> stringResource(R.string.error_rating_range, max)
    is E.UnknownOption -> stringResource(R.string.error_unknown_option)
    is E.TextBlank -> stringResource(R.string.error_text_blank)
    is E.TextTooLong -> stringResource(R.string.error_too_long, max)
    is E.TextHasLineBreak -> stringResource(R.string.error_line_break)
    is E.DateOutOfRange -> stringResource(R.string.error_date_range)
}

/** The id of the field [this] problem belongs to, or null for a problem with the whole item. */
val ValidationError.fieldId: String?
    get() = when (this) {
        is E.DuplicateFieldId -> fieldId
        is E.FieldTrackerMismatch -> fieldId
        is E.FieldNameBlank -> fieldId
        is E.FieldNameTooLong -> fieldId
        is E.DuplicateFieldName -> fieldId
        is E.DecimalsOutOfRange -> fieldId
        is E.UnitTooLong -> fieldId
        is E.MinAboveMax -> fieldId
        is E.UnknownCurrency -> fieldId
        is E.MaxDurationNotPositive -> fieldId
        is E.RatingScaleOutOfRange -> fieldId
        is E.NoOptions -> fieldId
        is E.TooManyOptions -> fieldId
        is E.OptionIdBlank -> fieldId
        is E.DuplicateOptionId -> fieldId
        is E.OptionLabelBlank -> fieldId
        is E.OptionLabelTooLong -> fieldId
        is E.DuplicateOptionLabel -> fieldId
        is E.TextLimitOutOfRange -> fieldId
        is E.UnknownField -> fieldId
        is E.RequiredFieldMissing -> fieldId
        is E.ValueTypeMismatch -> fieldId
        is E.BelowMin -> fieldId
        is E.AboveMax -> fieldId
        is E.NegativeDuration -> fieldId
        is E.RatingOutOfRange -> fieldId
        is E.UnknownOption -> fieldId
        is E.TextBlank -> fieldId
        is E.TextTooLong -> fieldId
        is E.TextHasLineBreak -> fieldId
        is E.DateOutOfRange -> fieldId
        else -> null
    }
