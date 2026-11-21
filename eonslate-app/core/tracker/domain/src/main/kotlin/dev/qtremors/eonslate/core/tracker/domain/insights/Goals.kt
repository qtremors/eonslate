package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Goal
import dev.qtremors.eonslate.core.tracker.domain.model.GoalPeriod
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.goal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * How far one field is toward its goal in the period that holds today.
 *
 * @property total the period's sum, in the field's stored units (scaled numbers, seconds).
 * @property logged whether anything was entered for the field in the period.
 */
data class GoalProgress(val field: FieldDefinition, val goal: Goal, val total: Long, val logged: Boolean) {
    /** Reached: at least the target, or within it once something is logged. */
    val met: Boolean get() = logged && goal.met(total)

    /** The share of the target, 0 to 1, for a meter. */
    val fraction: Float get() = (total.toDouble() / goal.target).toFloat().coerceIn(0f, 1f)
}

/** What goals add up to: today's progress, the days they were met, and days that count as done. */
object Goals {
    /** The fields with a goal, in field order. */
    fun fields(tracker: Tracker): List<FieldDefinition> = tracker.fields.filter { it.config.goal != null }

    private fun amount(value: FieldValue?): Long? = when (value) {
        is FieldValue.Number -> value.scaled
        is FieldValue.Duration -> value.seconds
        else -> null
    }

    /** Each goal's progress in the day or week holding [today]. */
    fun progress(tracker: Tracker, entries: List<Entry>, today: LocalDate, weekStart: DayOfWeek = DayOfWeek.MONDAY): List<GoalProgress> =
        fields(tracker).map { field ->
            val goal = field.config.goal!!
            val from = if (goal.per == GoalPeriod.WEEK) today.with(TemporalAdjusters.previousOrSame(weekStart)) else today
            val inPeriod = entries.filter { val day = it.localDay(); !day.isBefore(from) && !day.isAfter(today) }
            val amounts = inPeriod.mapNotNull { amount(it.values[field.id]) }
            GoalProgress(field, goal, amounts.sum(), amounts.isNotEmpty())
        }

    /** The days a daily goal of [field] was met. */
    fun metDays(field: FieldDefinition, entries: List<Entry>): Set<LocalDate> {
        val goal = field.config.goal ?: return emptySet()
        return entries.groupBy { it.localDay() }
            .filterValues { day -> day.mapNotNull { amount(it.values[field.id]) }.let { it.isNotEmpty() && goal.met(it.sum()) } }
            .keys
    }

    /** The first days of the weeks a weekly goal of [field] was met. */
    fun metWeeks(field: FieldDefinition, entries: List<Entry>, weekStart: DayOfWeek = DayOfWeek.MONDAY): Set<LocalDate> {
        val goal = field.config.goal ?: return emptySet()
        return entries.groupBy { it.localDay().with(TemporalAdjusters.previousOrSame(weekStart)) }
            .filterValues { week -> week.mapNotNull { amount(it.values[field.id]) }.let { it.isNotEmpty() && goal.met(it.sum()) } }
            .keys
    }

    /**
     * The days that count as done for streaks and Today: with daily goals, the days every one was
     * met; without them, every day with an entry. Weekly goals do not decide a single day.
     */
    fun doneDays(tracker: Tracker, entries: List<Entry>): Set<LocalDate> {
        val daily = fields(tracker).filter { it.config.goal?.per == GoalPeriod.DAY }
        val logged = entries.mapTo(HashSet()) { it.localDay() }
        if (daily.isEmpty()) return logged
        return daily.map { metDays(it, entries) }.reduce { all, next -> all intersect next }
    }
}
