package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import java.time.LocalDate

/**
 * Where one part of a tracker stands, such as a bike's oil: when it was last logged, how many
 * times, and what comes next by its set interval or, without one, its learned rhythm.
 */
data class PartStatus(
    val optionId: String,
    val label: String,
    val last: LocalDate?,
    val count: Int,
    val interval: IntervalStatus?,
    val rhythm: Rhythm?,
) {
    val due: Boolean get() = interval?.due == true
}

/** A tracker whose single choice is marked as parts shows each option as a sub tracker. */
object Parts {
    /** The single choice field that holds the parts, if the tracker has one. */
    fun field(tracker: Tracker): FieldDefinition? =
        tracker.fields.firstOrNull { (it.config as? FieldConfig.SingleChoice)?.parts == true }

    /** Every part in the order its options are set, with what is known about each. */
    fun of(tracker: Tracker, entries: List<Entry>, today: LocalDate): List<PartStatus> {
        val field = field(tracker) ?: return emptyList()
        val options = (field.config as FieldConfig.SingleChoice).options
        val statuses = Intervals.status(tracker, entries, today).filter { it.interval.choiceFieldId == field.id }
        val rhythms = Intervals.learned(tracker, entries, today)
        val byOption = entries.groupBy { (it.values[field.id] as? FieldValue.SingleChoice)?.optionId }
        return options.map { option ->
            val logs = byOption[option.id].orEmpty()
            val mine = statuses.filter { it.interval.optionId == option.id }
            PartStatus(
                optionId = option.id,
                label = option.label,
                last = logs.maxOfOrNull { it.localDay() },
                count = logs.size,
                interval = Intervals.next(mine) ?: mine.firstOrNull(),
                rhythm = rhythms.firstOrNull { it.optionId == option.id },
            )
        }
    }
}
