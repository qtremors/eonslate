package dev.qtremors.eonslate.core.tracker.domain.validation

import dev.qtremors.eonslate.core.tracker.domain.entry
import dev.qtremors.eonslate.core.tracker.domain.everyTypeTracker
import dev.qtremors.eonslate.core.tracker.domain.field
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.validValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryValidatorTest {
    private val tracker = everyTypeTracker()

    private fun errorsFor(fieldId: String, value: FieldValue) =
        EntryValidator.validateValue(tracker.field(fieldId)!!, value)

    @Test
    fun entryWithEveryValueIsValid() {
        assertEquals(ValidationResult.Valid, EntryValidator.validate(entry(validValues()), tracker))
    }

    @Test
    fun noteAloneIsEnoughButNothingIsNot() {
        assertTrue(EntryValidator.validate(entry(note = "Rest day"), tracker).isValid)
        assertTrue(ValidationError.EntryEmpty in EntryValidator.validate(entry(note = "  "), tracker).errorList)
    }

    @Test
    fun entryMustMatchItsTrackerAndZone() {
        val found = EntryValidator.validate(
            entry(note = "x").copy(trackerId = "other", zoneId = "Mars/Olympus", id = ""),
            tracker,
        ).errorList
        assertTrue(ValidationError.EntryTrackerMismatch in found)
        assertTrue(ValidationError.ZoneIdInvalid in found)
        assertTrue(ValidationError.EntryIdBlank in found)
    }

    @Test
    fun overlongNoteIsRejected() {
        val note = "x".repeat(Entry.NOTE_MAX_LENGTH + 1)
        assertTrue(ValidationError.NoteTooLong(Entry.NOTE_MAX_LENGTH) in EntryValidator.validate(entry(note = note), tracker).errorList)
    }

    @Test
    fun unknownFieldsAndMissingRequiredFieldsAreRejected() {
        val strict = tracker.copy(fields = tracker.fields + field("must", FieldConfig.YesNo, required = true))
        val found = EntryValidator.validate(entry(mapOf("ghost" to FieldValue.YesNo(true))), strict).errorList
        assertTrue(ValidationError.UnknownField("ghost") in found)
        assertTrue(ValidationError.RequiredFieldMissing("must") in found)
    }

    @Test
    fun valueOfWrongTypeIsRejected() {
        assertEquals(listOf(ValidationError.ValueTypeMismatch("weight")), errorsFor("weight", FieldValue.YesNo(true)))
    }

    @Test
    fun numberRangeIsInScaledUnits() {
        assertEquals(listOf(ValidationError.BelowMin("weight", 0)), errorsFor("weight", FieldValue.Number(-1)))
        assertEquals(listOf(ValidationError.AboveMax("weight", 5_000)), errorsFor("weight", FieldValue.Number(5_001)))
        assertTrue(errorsFor("weight", FieldValue.Number(5_000)).isEmpty())
    }

    @Test
    fun durationMustBeNonNegativeAndUnderMax() {
        assertEquals(listOf(ValidationError.NegativeDuration("slept")), errorsFor("slept", FieldValue.Duration(-1)))
        assertEquals(listOf(ValidationError.AboveMax("slept", 86_400)), errorsFor("slept", FieldValue.Duration(86_401)))
    }

    @Test
    fun ratingMustBeOnScale() {
        assertEquals(listOf(ValidationError.RatingOutOfRange("stars", 5)), errorsFor("stars", FieldValue.Rating(0)))
        assertEquals(listOf(ValidationError.RatingOutOfRange("stars", 5)), errorsFor("stars", FieldValue.Rating(6)))
    }

    @Test
    fun choicesMustBeKnownOptions() {
        assertEquals(listOf(ValidationError.UnknownOption("mood", "meh")), errorsFor("mood", FieldValue.SingleChoice("meh")))
        assertEquals(
            listOf(ValidationError.UnknownOption("tags", "a"), ValidationError.UnknownOption("tags", "z")),
            errorsFor("tags", FieldValue.MultiChoice(setOf("z", "good", "a"))),
        )
        assertTrue(errorsFor("tags", FieldValue.MultiChoice(emptySet())).isEmpty())
    }

    @Test
    fun textRulesAreChecked() {
        assertEquals(listOf(ValidationError.TextBlank("note")), errorsFor("note", FieldValue.Text("  ")))
        assertEquals(listOf(ValidationError.TextTooLong("note", 20)), errorsFor("note", FieldValue.Text("x".repeat(21))))
        assertEquals(listOf(ValidationError.TextHasLineBreak("note")), errorsFor("note", FieldValue.Text("a\nb")))
    }

    @Test
    fun multilineTextAllowsLineBreaks() {
        val multiline = field("long", FieldConfig.Text(multiline = true))
        assertTrue(EntryValidator.validateValue(multiline, FieldValue.Text("a\r\nb")).isEmpty())
    }

    @Test
    fun dateMustBeRepresentable() {
        assertEquals(listOf(ValidationError.DateOutOfRange("day")), errorsFor("day", FieldValue.Date(Long.MAX_VALUE)))
    }
}
