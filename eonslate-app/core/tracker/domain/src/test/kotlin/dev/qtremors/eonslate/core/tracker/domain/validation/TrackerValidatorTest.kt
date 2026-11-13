package dev.qtremors.eonslate.core.tracker.domain.validation

import dev.qtremors.eonslate.core.tracker.domain.NOW
import dev.qtremors.eonslate.core.tracker.domain.TRACKER_ID
import dev.qtremors.eonslate.core.tracker.domain.everyTypeTracker
import dev.qtremors.eonslate.core.tracker.domain.field
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerValidatorTest {

    private fun errors(tracker: Tracker) = TrackerValidator.validate(tracker).errorList

    @Test
    fun trackerWithEveryFieldTypeIsValid() {
        assertEquals(ValidationResult.Valid, TrackerValidator.validate(everyTypeTracker()))
    }

    @Test
    fun blankAndOverlongNamesAreRejected() {
        assertTrue(ValidationError.TrackerNameBlank in errors(everyTypeTracker().copy(name = "   ")))
        assertTrue(
            ValidationError.TrackerNameTooLong(Tracker.NAME_MAX_LENGTH) in
                errors(everyTypeTracker().copy(name = "x".repeat(Tracker.NAME_MAX_LENGTH + 1)))
        )
    }

    @Test
    fun nameIsMeasuredTrimmed() {
        val padded = "  " + "x".repeat(Tracker.NAME_MAX_LENGTH) + "  "
        assertTrue(TrackerValidator.validate(everyTypeTracker().copy(name = padded)).isValid)
    }

    @Test
    fun trackerNeedsBetweenOneAndMaxFields() {
        assertTrue(ValidationError.NoFields in errors(everyTypeTracker().copy(fields = emptyList())))
        val many = (0..Tracker.MAX_FIELDS).map { field("f$it", FieldConfig.YesNo) }
        assertTrue(ValidationError.TooManyFields(Tracker.MAX_FIELDS) in errors(everyTypeTracker().copy(fields = many)))
    }

    @Test
    fun colorIndexMustBeInPalette() {
        assertTrue(errors(everyTypeTracker().copy(colorIndex = TrackerColors.COUNT)).any { it is ValidationError.ColorIndexOutOfRange })
        assertTrue(errors(everyTypeTracker().copy(colorIndex = -1)).any { it is ValidationError.ColorIndexOutOfRange })
        assertTrue(TrackerValidator.validate(everyTypeTracker().copy(colorIndex = TrackerColors.COUNT - 1)).isValid)
    }

    @Test
    fun updatedBeforeCreatedIsRejected() {
        assertTrue(ValidationError.UpdatedBeforeCreated in errors(everyTypeTracker().copy(updatedAt = NOW - 1)))
    }

    @Test
    fun duplicateFieldIdsAndNamesAreRejected() {
        val tracker = everyTypeTracker().copy(
            fields = listOf(
                field("a", FieldConfig.YesNo, name = "Weight"),
                field("a", FieldConfig.YesNo, name = "Other"),
                field("b", FieldConfig.YesNo, name = " weight "),
            )
        )
        val found = errors(tracker)
        assertTrue(ValidationError.DuplicateFieldId("a") in found)
        assertTrue(ValidationError.DuplicateFieldName("b", "weight") in found)
    }

    @Test
    fun fieldOfAnotherTrackerIsRejected() {
        val stray = field("x", FieldConfig.YesNo).copy(trackerId = "other")
        assertTrue(ValidationError.FieldTrackerMismatch("x") in errors(everyTypeTracker().copy(fields = listOf(stray))))
    }

    @Test
    fun numberConfigRulesAreChecked() {
        val bad = field("n", FieldConfig.Number(unit = "x".repeat(17), decimals = 7, min = 10, max = 5))
        val found = TrackerValidator.validateField(bad)
        assertTrue(ValidationError.DecimalsOutOfRange("n", FieldConfig.Number.MAX_DECIMALS) in found)
        assertTrue(ValidationError.UnitTooLong("n", FieldConfig.Number.UNIT_MAX_LENGTH) in found)
        assertTrue(ValidationError.MinAboveMax("n") in found)
    }

    @Test
    fun unknownCurrencyIsRejected() {
        assertEquals(
            listOf(ValidationError.UnknownCurrency("m", "XYZ1")),
            TrackerValidator.validateField(field("m", FieldConfig.Money("XYZ1"))),
        )
        assertTrue(TrackerValidator.validateField(field("m", FieldConfig.Money("JPY"))).isEmpty())
    }

    @Test
    fun durationAndRatingAndTextLimitsAreChecked() {
        assertEquals(
            listOf(ValidationError.MaxDurationNotPositive("d")),
            TrackerValidator.validateField(field("d", FieldConfig.Duration(maxSeconds = 0))),
        )
        assertTrue(
            TrackerValidator.validateField(field("r", FieldConfig.Rating(max = 11)))
                .single() is ValidationError.RatingScaleOutOfRange
        )
        assertTrue(
            TrackerValidator.validateField(field("t", FieldConfig.Text(maxLength = 0)))
                .single() is ValidationError.TextLimitOutOfRange
        )
    }

    @Test
    fun choiceOptionsAreChecked() {
        val options = listOf(
            ChoiceOption("a", "Yes"),
            ChoiceOption("a", "No"),
            ChoiceOption("", "Maybe"),
            ChoiceOption("c", " "),
            ChoiceOption("d", "yes "),
            ChoiceOption("e", "x".repeat(ChoiceOption.LABEL_MAX_LENGTH + 1)),
        )
        val found = TrackerValidator.validateField(field("c", FieldConfig.MultiChoice(options)))
        assertTrue(ValidationError.DuplicateOptionId("c", "a") in found)
        assertTrue(ValidationError.OptionIdBlank("c") in found)
        assertTrue(ValidationError.OptionLabelBlank("c", "c") in found)
        assertTrue(ValidationError.DuplicateOptionLabel("c", "yes") in found)
        assertTrue(ValidationError.OptionLabelTooLong("c", "e", ChoiceOption.LABEL_MAX_LENGTH) in found)
        assertEquals(
            listOf(ValidationError.NoOptions("s")),
            TrackerValidator.validateField(field("s", FieldConfig.SingleChoice(emptyList()))),
        )
    }

    @Test
    fun everyProblemIsReportedAtOnce() {
        val tracker = Tracker(id = "", name = "", iconName = "", colorIndex = 99, createdAt = NOW)
        val found = errors(tracker)
        assertTrue(ValidationError.TrackerIdBlank in found)
        assertTrue(ValidationError.TrackerNameBlank in found)
        assertTrue(ValidationError.IconNameBlank in found)
        assertTrue(ValidationError.NoFields in found)
        assertTrue(found.any { it is ValidationError.ColorIndexOutOfRange })
    }

    @Test
    fun blankFieldNameIsRejected() {
        val found = TrackerValidator.validateField(field("f", FieldConfig.YesNo, name = "  ").copy(trackerId = TRACKER_ID))
        assertEquals(listOf(ValidationError.FieldNameBlank("f")), found)
    }
}
