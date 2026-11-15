package dev.qtremors.eonslate.core.tracker.ui

import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.TrackerValidator
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatesTest {
    // Resources are not loaded in a plain unit test, so each resource reads as its own id.
    private val text: (Int) -> String = { "s$it" }
    private var counter = 0
    private val ids = IdGenerator { "id${++counter}" }

    @Test
    fun everyTemplateMakesAValidTracker() {
        Templates.all.forEach { spec ->
            val tracker = Templates.instantiate(spec, text, ids, currency = "INR", now = 10)
            assertEquals(spec.id, TrackerValidator.validate(tracker).errorList, emptyList<Any>())
            assertEquals(spec.id, tracker.templateId)
        }
    }

    @Test
    fun templateIdsAreUniqueAndStable() {
        val ids = Templates.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
        assertTrue(ids.all { it.matches(Regex("[a-z_]+")) })
        assertEquals("expenses", Templates.byId("expenses")!!.id)
        assertNull(Templates.byId("nope"))
    }

    @Test
    fun templatesUseKnownIconsAndPigments() {
        Templates.all.forEach { spec ->
            assertTrue(spec.id, spec.iconName in TrackerIcons.all)
            assertTrue(spec.id, TrackerColors.isValid(spec.colorIndex))
        }
    }

    @Test
    fun eachTrackerGetsFreshIdsButOptionsKeepTheirs() {
        val spec = Templates.byId("mood")!!
        val first = Templates.instantiate(spec, text, ids, "INR", 0)
        val second = Templates.instantiate(spec, text, ids, "INR", 0)
        val allIds = listOf(first.id, second.id) + first.fields.map { it.id } + second.fields.map { it.id }
        assertEquals(allIds.toSet().size, allIds.size)
        val options = (first.fields[1].config as FieldConfig.MultiChoice).options.map { it.id }
        assertEquals(options, (second.fields[1].config as FieldConfig.MultiChoice).options.map { it.id })
        assertTrue(first.fields.all { it.trackerId == first.id })
    }

    @Test
    fun moneyUsesTheGivenCurrencyAndUnitsAreWords() {
        val expenses = Templates.instantiate(Templates.byId("expenses")!!, text, ids, "JPY", 0)
        assertEquals(FieldConfig.Money("JPY"), expenses.fields.first().config)
        val workout = Templates.instantiate(Templates.byId("workout")!!, text, ids, "INR", 0)
        val distance = workout.fields.first { it.config is FieldConfig.Number }.config as FieldConfig.Number
        assertEquals("s${R.string.template_unit_km}", distance.unit)
        assertEquals(2, distance.decimals)
    }

    @Test
    fun bloodPressureHasTwoRequiredReadings() {
        val bp = Templates.instantiate(Templates.byId("blood_pressure")!!, text, ids, "INR", 0)
        assertEquals(listOf(true, true, false), bp.fields.map { it.required })
        assertTrue(bp.fields.all { it.config is FieldConfig.Number })
    }

    @Test
    fun everyShelfHoldsSomething() {
        TemplateCategory.entries.forEach { category ->
            assertTrue(category.name, Templates.all.any { it.category == category })
        }
    }

    @Test
    fun searchMatchesEveryWordInNameOrDescription() {
        val words = mapOf(
            R.string.template_water to "Water",
            R.string.template_water_about to "Glasses through the day",
            R.string.template_mood to "Mood",
            R.string.template_mood_about to "How you feel, and what is behind it",
        )
        val read: (Int) -> String = { words[it] ?: "" }
        val pool = listOf(Templates.byId("water")!!, Templates.byId("mood")!!)
        assertEquals(listOf("water"), Templates.search("GLASSES", read, pool).map { it.id })
        assertEquals(listOf("mood"), Templates.search("feel behind", read, pool).map { it.id })
        assertEquals(emptyList<String>(), Templates.search("feel water", read, pool).map { it.id })
        assertEquals(2, Templates.search("  ", read, pool).size)
    }

    @Test
    fun searchIgnoresAccents() {
        val read: (Int) -> String = { if (it == R.string.template_caffeine) "Café" else "" }
        assertEquals(listOf("caffeine"), Templates.search("cafe", read, listOf(Templates.byId("caffeine")!!)).map { it.id })
    }

    @Test
    fun everyTemplateNeedsSomethingInEachEntry() {
        // A required field means an entry from a template always says something.
        Templates.all.forEach { spec -> assertTrue(spec.id, spec.fields.any { it.required }) }
    }

    @Test
    fun thereAreTwentyTemplates() {
        assertEquals(20, Templates.all.size)
    }

    @Test
    fun aHabitIsASingleYesNoForOneTapLogging() {
        val habit = Templates.instantiate(Templates.byId("habit")!!, text, ids, "INR", 0)
        assertEquals(listOf(FieldConfig.YesNo), habit.fields.map { it.config })
    }
}
