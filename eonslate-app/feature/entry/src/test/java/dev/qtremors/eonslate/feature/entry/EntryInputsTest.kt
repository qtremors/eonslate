package dev.qtremors.eonslate.feature.entry

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryInputsTest {
    private val options = listOf(ChoiceOption("a", "A"), ChoiceOption("b", "B"))
    private val fields = listOf(
        FieldDefinition("n", "t", "Weight", FieldConfig.Number("kg", 1)),
        FieldDefinition("m", "t", "Spent", FieldConfig.Money("JPY")),
        FieldDefinition("d", "t", "Slept", FieldConfig.Duration()),
        FieldDefinition("r", "t", "Mood", FieldConfig.Rating(5)),
        FieldDefinition("y", "t", "Done", FieldConfig.YesNo),
        FieldDefinition("s", "t", "One", FieldConfig.SingleChoice(options)),
        FieldDefinition("c", "t", "Many", FieldConfig.MultiChoice(options)),
        FieldDefinition("x", "t", "Note", FieldConfig.Text()),
        FieldDefinition("z", "t", "Day", FieldConfig.Date),
    )
    private val tracker = Tracker(id = "t", name = "T", fields = fields, createdAt = 0)
    private fun field(id: String) = tracker.field(id)!!

    @Test
    fun emptyInputsAreNotValues() {
        fields.forEach { assertEquals(it.id, ReadInput.Empty, EntryInputs.read(it, FieldInput())) }
        fields.forEach { assertEquals(it.id, ReadInput.Empty, EntryInputs.read(it, null)) }
    }

    @Test
    fun typedValuesAreReadExactly() {
        assertEquals(ReadInput.Value(FieldValue.Number(725)), EntryInputs.read(field("n"), FieldInput(text = "72.5")))
        assertEquals(ReadInput.Value(FieldValue.Money(-1_200)), EntryInputs.read(field("m"), FieldInput(text = "-1,200")))
        assertEquals(ReadInput.Value(FieldValue.Duration(23_400)), EntryInputs.read(field("d"), FieldInput(text = "6", text2 = "30")))
        assertEquals(ReadInput.Value(FieldValue.Duration(1_800)), EntryInputs.read(field("d"), FieldInput(text2 = "30")))
        assertEquals(ReadInput.Value(FieldValue.Text("hi")), EntryInputs.read(field("x"), FieldInput(text = "  hi ")))
    }

    @Test
    fun tooPreciseOrBrokenInputIsUnreadable() {
        assertEquals(ReadInput.Unreadable, EntryInputs.read(field("n"), FieldInput(text = "72.55")))
        assertEquals(ReadInput.Unreadable, EntryInputs.read(field("m"), FieldInput(text = "12.5")))
        assertEquals(ReadInput.Unreadable, EntryInputs.read(field("d"), FieldInput(text = "1", text2 = "75")))
        assertEquals(ReadInput.Unreadable, EntryInputs.read(field("n"), FieldInput(text = "abc")))
    }

    @Test
    fun choicesYesNoRatingsAndDates() {
        assertEquals(ReadInput.Value(FieldValue.Rating(4)), EntryInputs.read(field("r"), FieldInput(number = 4)))
        assertEquals(ReadInput.Value(FieldValue.YesNo(false)), EntryInputs.read(field("y"), FieldInput(number = 0)))
        assertEquals(ReadInput.Value(FieldValue.SingleChoice("b")), EntryInputs.read(field("s"), FieldInput(choices = setOf("b"))))
        assertEquals(ReadInput.Value(FieldValue.Date(20_402)), EntryInputs.read(field("z"), FieldInput(day = 20_402)))
    }

    @Test
    fun touchedEmptyMultiChoiceIsAnAnswer() {
        assertEquals(ReadInput.Empty, EntryInputs.read(field("c"), FieldInput()))
        assertEquals(ReadInput.Value(FieldValue.MultiChoice(emptySet())), EntryInputs.read(field("c"), FieldInput(touched = true)))
    }

    @Test
    fun everyLoggedValueRoundTripsThroughItsInput() {
        val values = mapOf(
            "n" to FieldValue.Number(-15),
            "m" to FieldValue.Money(98_765),
            "d" to FieldValue.Duration(3_725),
            "r" to FieldValue.Rating(2),
            "y" to FieldValue.YesNo(true),
            "s" to FieldValue.SingleChoice("a"),
            "c" to FieldValue.MultiChoice(setOf("a", "b")),
            "x" to FieldValue.Text("line"),
            "z" to FieldValue.Date(-3),
        )
        val entry = Entry(id = "e", trackerId = "t", occurredAt = 9, zoneId = "UTC", values = values, createdAt = 5)
        val draft = EntryInputs.draftOf(entry, tracker)
        val rebuilt = draft.build(tracker, now = 50).entry!!
        assertEquals(values, rebuilt.values)
        assertEquals(5L, rebuilt.createdAt)
        assertEquals(50L, rebuilt.updatedAt)
    }

    @Test
    fun buildReportsEveryUnreadableField() {
        val draft = EntryDraft(
            id = "e", trackerId = "t", occurredAt = 0, zoneId = "UTC",
            inputs = mapOf("n" to FieldInput(text = "1.25"), "m" to FieldInput(text = "x"), "y" to FieldInput(number = 1)),
        )
        val build = draft.build(tracker, now = 0)
        assertNull(build.entry)
        assertEquals(setOf("n", "m"), build.unreadable)
    }

    @Test
    fun draftSurvivesJson() {
        val draft = EntryDraft("e", "t", 1, "Asia/Kolkata", "n", mapOf("c" to FieldInput(choices = setOf("a"), touched = true)))
        assertEquals(draft, EntryJson.decodeFromString(EntryDraft.serializer(), EntryJson.encodeToString(EntryDraft.serializer(), draft)))
    }
}
