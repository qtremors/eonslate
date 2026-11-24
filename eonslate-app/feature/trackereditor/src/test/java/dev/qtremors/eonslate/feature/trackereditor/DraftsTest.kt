package dev.qtremors.eonslate.feature.trackereditor

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DraftsTest {
    @Test
    fun numberLimitsAreParsedAtTheFieldsDecimals() {
        val draft = FieldDraft(id = "w", name = " Weight ", unit = " kg ", decimals = 1, min = "0", max = "250.5")
        val field = draft.build("t").field!!
        assertEquals("Weight", field.name)
        assertEquals(FieldConfig.Number(unit = "kg", decimals = 1, min = 0, max = 2_505), field.config)
    }

    @Test
    fun unreadableLimitsAreReportedNotGuessed() {
        val build = FieldDraft(id = "w", decimals = 1, min = "1.25", max = "abc").build("t")
        assertNull(build.field)
        assertEquals(LimitProblems(minInvalid = true, maxInvalid = true), build.problems)
    }

    @Test
    fun limitsOnlyMatterForNumbers() {
        val build = FieldDraft(id = "y", type = FieldType.YES_NO, min = "nonsense").build("t")
        assertEquals(FieldConfig.YesNo, build.field!!.config)
    }

    @Test
    fun everyTypeBuildsItsConfig() {
        val options = listOf(ChoiceOption("a", " A "), ChoiceOption("b", "B"))
        val base = FieldDraft(id = "f", name = "F", currencyCode = "jpy", ratingMax = 10, multiline = true, options = options)
        val configs = FieldType.entries.associateWith { base.copy(type = it).build("t").field!!.config }
        assertEquals(FieldConfig.Money("JPY"), configs[FieldType.MONEY])
        assertEquals(FieldConfig.Rating(10), configs[FieldType.RATING])
        assertEquals(FieldConfig.Text(multiline = true), configs[FieldType.TEXT])
        assertEquals(FieldConfig.SingleChoice(listOf(ChoiceOption("a", "A"), ChoiceOption("b", "B"))), configs[FieldType.SINGLE_CHOICE])
        assertEquals(FieldType.entries.toSet(), configs.values.map { it.type }.toSet())
    }

    @Test
    fun storedFieldsRoundTripThroughTheirDraft() {
        val fields = listOf(
            FieldDefinition("n", "t", "Weight", FieldConfig.Number("kg", 2, min = -150, max = 25_000), required = true),
            FieldDefinition("m", "t", "Spent", FieldConfig.Money("KWD")),
            FieldDefinition("d", "t", "Slept", FieldConfig.Duration(maxSeconds = 3_600)),
            FieldDefinition("r", "t", "Mood", FieldConfig.Rating(7)),
            FieldDefinition("c", "t", "Tags", FieldConfig.MultiChoice(listOf(ChoiceOption("x", "X")))),
            FieldDefinition("x", "t", "Note", FieldConfig.Text(multiline = true, maxLength = 900)),
            FieldDefinition("y", "t", "Done", FieldConfig.YesNo),
            FieldDefinition("z", "t", "Day", FieldConfig.Date),
        )
        fields.forEachIndexed { i, field ->
            val back = field.toDraft("USD").build("t", position = i).field!!
            assertEquals(field.copy(position = i), back)
        }
        assertEquals("-1.50", fields[0].toDraft("USD").min)
    }

    @Test
    fun newTrackerTakesNowAndKeepsItLater() {
        val draft = TrackerDraft(id = "t", name = " Water ", fields = listOf(FieldDraft("g", "Glasses")))
        val first = draft.build(now = 100, position = 3).tracker!!
        assertEquals(100L, first.createdAt)
        assertEquals("Water", first.name)
        assertEquals(3, first.position)

        val again = first.toDraft("USD").build(now = 500, position = 3).tracker!!
        assertEquals(100L, again.createdAt)
        assertEquals(500L, again.updatedAt)
    }

    @Test
    fun trackerWithBadLimitsReportsTheFields() {
        val draft = TrackerDraft(id = "t", fields = listOf(FieldDraft("g", "G"), FieldDraft("w", "W", max = "x")))
        val build = draft.build(0, 0)
        assertNull(build.tracker)
        assertEquals(setOf("w"), build.badLimits.keys)
    }

    @Test
    fun draftSurvivesJson() {
        val draft = TrackerDraft(
            id = "t",
            name = "N",
            fields = listOf(FieldDraft("a", "A", type = FieldType.SINGLE_CHOICE, options = listOf(ChoiceOption("o", "O")))),
            createdAt = 5,
        )
        val text = DraftJson.encodeToString(TrackerDraft.serializer(), draft)
        assertEquals(draft, DraftJson.decodeFromString(TrackerDraft.serializer(), text))
    }

    @Test
    fun paletteHasAPigmentForEveryTrackerColor() {
        assertEquals(TrackerColors.COUNT, BadgePalette.size)
        assertEquals(Tracker.DEFAULT_ICON, dev.qtremors.eonslate.core.ui.components.TrackerIcons.DEFAULT_NAME)
    }

    @Test
    fun defaultCurrencyFollowsTheLocale() {
        assertEquals("INR", TrackerEditorViewModel.defaultCurrencyCode(Locale("en", "IN")))
        assertEquals("JPY", TrackerEditorViewModel.defaultCurrencyCode(Locale.JAPAN))
        assertTrue(TrackerEditorViewModel.defaultCurrencyCode(Locale("en")).length == 3)
    }
}
