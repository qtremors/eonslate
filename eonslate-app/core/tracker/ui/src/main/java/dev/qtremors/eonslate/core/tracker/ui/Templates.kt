package dev.qtremors.eonslate.core.tracker.ui

import androidx.annotation.StringRes
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator

/** One choice of a template field. [id] is stable, so trackers from one template share option ids. */
data class ChoiceSpec(val id: String, @param:StringRes val label: Int)

/** A field of a template, named by a string resource so templates follow the device language. */
sealed interface FieldSpec {
    @get:StringRes val name: Int
    val required: Boolean

    data class Number(
        @param:StringRes override val name: Int,
        @param:StringRes val unit: Int? = null,
        val decimals: Int = 0,
        val min: Long? = null,
        override val required: Boolean = false,
    ) : FieldSpec

    /** Money in the device's currency. */
    data class Money(@param:StringRes override val name: Int, override val required: Boolean = false) : FieldSpec

    data class Duration(@param:StringRes override val name: Int, override val required: Boolean = false) : FieldSpec

    data class Rating(@param:StringRes override val name: Int, val max: Int = 5, override val required: Boolean = false) : FieldSpec

    data class YesNo(@param:StringRes override val name: Int, override val required: Boolean = false) : FieldSpec

    data class Choice(
        @param:StringRes override val name: Int,
        val options: List<ChoiceSpec>,
        val multiple: Boolean = false,
        override val required: Boolean = false,
    ) : FieldSpec

    data class Text(@param:StringRes override val name: Int, val multiline: Boolean = false, override val required: Boolean = false) : FieldSpec

    data class Date(@param:StringRes override val name: Int, override val required: Boolean = false) : FieldSpec
}

/** The shelves the gallery groups templates on, in the order they are shown. */
enum class TemplateCategory(@param:StringRes val title: Int) {
    HEALTH(R.string.template_category_health),
    MIND(R.string.template_category_mind),
    ACTIVITY(R.string.template_category_activity),
    MONEY_TIME(R.string.template_category_money_time),
}

/** A ready-made tracker. [id] is stored on trackers made from it, so it never changes. */
data class TemplateSpec(
    val id: String,
    @param:StringRes val name: Int,
    @param:StringRes val description: Int,
    val iconName: String,
    val colorIndex: Int,
    val fields: List<FieldSpec>,
    val category: TemplateCategory,
)

object Templates {
    val all: List<TemplateSpec> = listOf(
        TemplateSpec(
            id = "expenses", name = R.string.template_expenses, description = R.string.template_expenses_about,
            iconName = "Payments", colorIndex = 1,
            fields = listOf(
                FieldSpec.Money(R.string.template_field_amount, required = true),
                FieldSpec.Choice(
                    R.string.template_field_category,
                    listOf(
                        ChoiceSpec("food", R.string.template_option_food),
                        ChoiceSpec("transport", R.string.template_option_transport),
                        ChoiceSpec("shopping", R.string.template_option_shopping),
                        ChoiceSpec("bills", R.string.template_option_bills),
                        ChoiceSpec("fun", R.string.template_option_fun),
                        ChoiceSpec("other", R.string.template_option_other),
                    ),
                ),
            ),
            category = TemplateCategory.MONEY_TIME,
        ),
        TemplateSpec(
            id = "habit", name = R.string.template_habit, description = R.string.template_habit_about,
            iconName = "Star", colorIndex = 2,
            fields = listOf(FieldSpec.YesNo(R.string.template_field_done, required = true)),
            category = TemplateCategory.MIND,
        ),
        TemplateSpec(
            id = "mood", name = R.string.template_mood, description = R.string.template_mood_about,
            iconName = "Mood", colorIndex = 5,
            fields = listOf(
                FieldSpec.Rating(R.string.template_field_mood, required = true),
                FieldSpec.Choice(
                    R.string.template_field_feelings,
                    listOf(
                        ChoiceSpec("calm", R.string.template_option_calm),
                        ChoiceSpec("happy", R.string.template_option_happy),
                        ChoiceSpec("grateful", R.string.template_option_grateful),
                        ChoiceSpec("tired", R.string.template_option_tired),
                        ChoiceSpec("anxious", R.string.template_option_anxious),
                        ChoiceSpec("stressed", R.string.template_option_stressed),
                    ),
                    multiple = true,
                ),
            ),
            category = TemplateCategory.MIND,
        ),
        TemplateSpec(
            id = "workout", name = R.string.template_workout, description = R.string.template_workout_about,
            iconName = "FitnessCenter", colorIndex = 8,
            fields = listOf(
                FieldSpec.Choice(
                    R.string.template_field_activity,
                    listOf(
                        ChoiceSpec("run", R.string.template_option_run),
                        ChoiceSpec("walk", R.string.template_option_walk),
                        ChoiceSpec("cycle", R.string.template_option_cycle),
                        ChoiceSpec("strength", R.string.template_option_strength),
                        ChoiceSpec("yoga", R.string.template_option_yoga),
                        ChoiceSpec("swim", R.string.template_option_swim),
                    ),
                ),
                FieldSpec.Duration(R.string.template_field_time, required = true),
                FieldSpec.Number(R.string.template_field_distance, unit = R.string.template_unit_km, decimals = 2, min = 0),
                FieldSpec.Rating(R.string.template_field_effort),
            ),
            category = TemplateCategory.ACTIVITY,
        ),
        TemplateSpec(
            id = "reading", name = R.string.template_reading, description = R.string.template_reading_about,
            iconName = "Book", colorIndex = 6,
            fields = listOf(
                FieldSpec.Number(R.string.template_field_pages, min = 0, required = true),
                FieldSpec.Text(R.string.template_field_book),
                FieldSpec.Duration(R.string.template_field_time),
            ),
            category = TemplateCategory.MIND,
        ),
        TemplateSpec(
            id = "water", name = R.string.template_water, description = R.string.template_water_about,
            iconName = "LocalDrink", colorIndex = 3,
            fields = listOf(FieldSpec.Number(R.string.template_field_glasses, min = 0, required = true)),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "sleep", name = R.string.template_sleep, description = R.string.template_sleep_about,
            iconName = "Bedtime", colorIndex = 14,
            fields = listOf(
                FieldSpec.Duration(R.string.template_field_slept, required = true),
                FieldSpec.Rating(R.string.template_field_quality),
            ),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "medication", name = R.string.template_medication, description = R.string.template_medication_about,
            iconName = "Medication", colorIndex = 13,
            fields = listOf(
                FieldSpec.Text(R.string.template_field_medicine, required = true),
                FieldSpec.Number(R.string.template_field_dose, unit = R.string.template_unit_mg, decimals = 1, min = 0),
            ),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "weight", name = R.string.template_weight, description = R.string.template_weight_about,
            iconName = "Favorite", colorIndex = 9,
            fields = listOf(FieldSpec.Number(R.string.template_field_weight, unit = R.string.template_unit_kg, decimals = 1, min = 0, required = true)),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "meditation", name = R.string.template_meditation, description = R.string.template_meditation_about,
            iconName = "SelfImprovement", colorIndex = 7,
            fields = listOf(FieldSpec.Duration(R.string.template_field_time, required = true)),
            category = TemplateCategory.MIND,
        ),
        TemplateSpec(
            id = "journal", name = R.string.template_journal, description = R.string.template_journal_about,
            iconName = "AutoAwesome", colorIndex = 10,
            fields = listOf(
                FieldSpec.Text(R.string.template_field_entry, multiline = true, required = true),
                FieldSpec.Rating(R.string.template_field_day),
            ),
            category = TemplateCategory.MIND,
        ),
        TemplateSpec(
            id = "blood_pressure", name = R.string.template_blood_pressure, description = R.string.template_blood_pressure_about,
            iconName = "Favorite", colorIndex = 13,
            fields = listOf(
                FieldSpec.Number(R.string.template_field_systolic, unit = R.string.template_unit_mmhg, min = 0, required = true),
                FieldSpec.Number(R.string.template_field_diastolic, unit = R.string.template_unit_mmhg, min = 0, required = true),
                FieldSpec.Number(R.string.template_field_pulse, unit = R.string.template_unit_bpm, min = 0),
            ),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "caffeine", name = R.string.template_caffeine, description = R.string.template_caffeine_about,
            iconName = "LocalDrink", colorIndex = 4,
            fields = listOf(
                FieldSpec.Choice(
                    R.string.template_field_drink,
                    listOf(
                        ChoiceSpec("coffee", R.string.template_option_coffee),
                        ChoiceSpec("tea", R.string.template_option_tea),
                        ChoiceSpec("energy", R.string.template_option_energy),
                        ChoiceSpec("other", R.string.template_option_other),
                    ),
                    required = true,
                ),
                FieldSpec.Number(R.string.template_field_cups, min = 0),
            ),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "steps", name = R.string.template_steps, description = R.string.template_steps_about,
            iconName = "FitnessCenter", colorIndex = 12,
            fields = listOf(FieldSpec.Number(R.string.template_field_steps, min = 0, required = true)),
            category = TemplateCategory.ACTIVITY,
        ),
        TemplateSpec(
            id = "study", name = R.string.template_study, description = R.string.template_study_about,
            iconName = "Book", colorIndex = 0,
            fields = listOf(
                FieldSpec.Text(R.string.template_field_subject, required = true),
                FieldSpec.Duration(R.string.template_field_time, required = true),
                FieldSpec.Rating(R.string.template_field_focus),
            ),
            category = TemplateCategory.MONEY_TIME,
        ),
        TemplateSpec(
            id = "savings", name = R.string.template_savings, description = R.string.template_savings_about,
            iconName = "Payments", colorIndex = 15,
            fields = listOf(FieldSpec.Money(R.string.template_field_saved, required = true)),
            category = TemplateCategory.MONEY_TIME,
        ),
        TemplateSpec(
            id = "gratitude", name = R.string.template_gratitude, description = R.string.template_gratitude_about,
            iconName = "Favorite", colorIndex = 5,
            fields = listOf(FieldSpec.Text(R.string.template_field_grateful_for, multiline = true, required = true)),
            category = TemplateCategory.MIND,
        ),
        TemplateSpec(
            id = "symptoms", name = R.string.template_symptoms, description = R.string.template_symptoms_about,
            iconName = "Medication", colorIndex = 11,
            fields = listOf(
                FieldSpec.Choice(
                    R.string.template_field_symptoms,
                    listOf(
                        ChoiceSpec("headache", R.string.template_option_headache),
                        ChoiceSpec("fatigue", R.string.template_option_fatigue),
                        ChoiceSpec("nausea", R.string.template_option_nausea),
                        ChoiceSpec("pain", R.string.template_option_pain),
                        ChoiceSpec("fever", R.string.template_option_fever),
                        ChoiceSpec("cough", R.string.template_option_cough),
                    ),
                    multiple = true,
                    required = true,
                ),
                FieldSpec.Rating(R.string.template_field_severity),
            ),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "cycle", name = R.string.template_cycle, description = R.string.template_cycle_about,
            iconName = "Favorite", colorIndex = 6,
            fields = listOf(
                FieldSpec.Choice(
                    R.string.template_field_flow,
                    listOf(
                        ChoiceSpec("spotting", R.string.template_option_spotting),
                        ChoiceSpec("light", R.string.template_option_light),
                        ChoiceSpec("medium", R.string.template_option_medium),
                        ChoiceSpec("heavy", R.string.template_option_heavy),
                    ),
                    required = true,
                ),
                FieldSpec.Rating(R.string.template_field_cramps),
            ),
            category = TemplateCategory.HEALTH,
        ),
        TemplateSpec(
            id = "screen_time", name = R.string.template_screen_time, description = R.string.template_screen_time_about,
            iconName = "Timer", colorIndex = 16,
            fields = listOf(FieldSpec.Duration(R.string.template_field_screen_time, required = true)),
            category = TemplateCategory.MONEY_TIME,
        ),
    )

    fun byId(id: String): TemplateSpec? = all.firstOrNull { it.id == id }

    /**
     * The templates whose name or description contains every word of [query], ignoring case and
     * accents; all of them for a blank query. [text] reads a string resource.
     */
    fun search(query: String, text: (Int) -> String, templates: List<TemplateSpec> = all): List<TemplateSpec> {
        val words = fold(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return templates
        return templates.filter { spec ->
            val haystack = fold(text(spec.name) + " " + text(spec.description) + " " + text(spec.category.title))
            words.all { it in haystack }
        }
    }

    private fun fold(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    /**
     * A new tracker from [spec], in the device's words ([text] reads a string resource) and
     * currency. Every tracker and field gets a fresh id; choice options keep the template's ids.
     */
    fun instantiate(spec: TemplateSpec, text: (Int) -> String, ids: IdGenerator, currency: String, now: Long, position: Int = 0): Tracker {
        val trackerId = ids.newId()
        return Tracker(
            id = trackerId,
            name = text(spec.name),
            description = text(spec.description),
            iconName = spec.iconName,
            colorIndex = spec.colorIndex,
            fields = spec.fields.mapIndexed { index, field ->
                FieldDefinition(
                    id = ids.newId(),
                    trackerId = trackerId,
                    name = text(field.name),
                    config = config(field, text, currency),
                    required = field.required,
                    position = index,
                )
            },
            position = position,
            templateId = spec.id,
            createdAt = now,
        )
    }

    private fun config(field: FieldSpec, text: (Int) -> String, currency: String): FieldConfig = when (field) {
        is FieldSpec.Number -> FieldConfig.Number(unit = field.unit?.let(text), decimals = field.decimals, min = field.min)
        is FieldSpec.Money -> FieldConfig.Money(currency)
        is FieldSpec.Duration -> FieldConfig.Duration()
        is FieldSpec.Rating -> FieldConfig.Rating(field.max)
        is FieldSpec.YesNo -> FieldConfig.YesNo
        is FieldSpec.Choice -> field.options.map { ChoiceOption(it.id, text(it.label)) }
            .let { if (field.multiple) FieldConfig.MultiChoice(it) else FieldConfig.SingleChoice(it) }
        is FieldSpec.Text -> FieldConfig.Text(multiline = field.multiline)
        is FieldSpec.Date -> FieldConfig.Date
    }
}
