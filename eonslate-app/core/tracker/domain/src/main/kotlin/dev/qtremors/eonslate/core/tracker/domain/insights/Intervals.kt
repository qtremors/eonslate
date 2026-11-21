package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Interval
import dev.qtremors.eonslate.core.tracker.domain.model.IntervalUnit
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Where one interval stands today.
 *
 * @property label the job's name: the choice option's label, or null when any log counts.
 * @property last the day it was last done; null when never logged.
 * @property lastAmount the number field's value on that log, such as the odometer then.
 * @property dueOn the day it is due again, for a timed interval.
 * @property dueAt the number it is due at, for a metered one; [current] is the latest reading.
 */
data class IntervalStatus(
    val interval: Interval,
    val label: String?,
    val last: LocalDate?,
    val lastAmount: Long?,
    val dueOn: LocalDate?,
    val dueAt: Long?,
    val current: Long?,
    val today: LocalDate,
) {
    /** Days until [dueOn]; zero or less once it has come. */
    val daysLeft: Long? get() = dueOn?.let { ChronoUnit.DAYS.between(today, it) }

    /** How much more the field can grow before [dueAt]; zero or less once reached. */
    val amountLeft: Long? get() = if (dueAt != null && current != null) dueAt - current else null

    /** Due once either the day or the amount is reached. Never-logged jobs are not due, only waiting. */
    val due: Boolean get() = last != null && ((daysLeft ?: 1) <= 0 || (amountLeft ?: 1) <= 0)

    private val timeShare: Double?
        get() = daysLeft?.let { left -> left.toDouble() / ChronoUnit.DAYS.between(last ?: today, dueOn).coerceAtLeast(1) }
    private val amountShare: Double?
        get() = amountLeft?.let { left -> left.toDouble() / (interval.amount ?: 1).coerceAtLeast(1) }

    /** How soon, for picking the most pressing: the share of the interval that is left. */
    val urgency: Double get() = listOfNotNull(timeShare, amountShare).minOrNull() ?: Double.MAX_VALUE

    /** True when the amount, not the time, is nearer to due, so it is the one to show. */
    val leadsByAmount: Boolean get() = amountShare?.let { a -> timeShare?.let { a <= it } ?: true } ?: false
}

/** Intervals measured from each job's last log, against the latest reading of a number field. */
object Intervals {
    fun status(tracker: Tracker, entries: List<Entry>, today: LocalDate): List<IntervalStatus> {
        val newestFirst = entries.sortedByDescending { it.occurredAt }
        return tracker.intervals.filter { it.isValid(tracker.fields) }.map { interval ->
            val label = interval.choiceFieldId?.let { fieldId ->
                (tracker.field(fieldId)?.config as? FieldConfig.SingleChoice)?.options?.firstOrNull { it.id == interval.optionId }?.label
            }
            val job = newestFirst.firstOrNull { entry ->
                interval.choiceFieldId == null || (entry.values[interval.choiceFieldId] as? FieldValue.SingleChoice)?.optionId == interval.optionId
            }
            val last = job?.localDay()
            val dueOn = if (interval.every != null && last != null) advance(last, interval.every, interval.unit) else null
            val amountField = interval.amountFieldId
            val lastAmount = amountField?.let { (job?.values?.get(it) as? FieldValue.Number)?.scaled }
            // The newest reading in any entry, such as a fuel log's odometer.
            val current = amountField?.let { id -> newestFirst.firstNotNullOfOrNull { (it.values[id] as? FieldValue.Number)?.scaled } }
            val dueAt = if (interval.amount != null && lastAmount != null) lastAmount + interval.amount else null
            IntervalStatus(interval, label, last, lastAmount, dueOn, dueAt, current, today)
        }
    }

    /** The most pressing interval: a due one first, then the least left. */
    fun next(statuses: List<IntervalStatus>): IntervalStatus? =
        statuses.filter { it.last != null }.minByOrNull { if (it.due) -1.0 else it.urgency }

    /**
     * The rhythm a tracker keeps by itself, learned from its history with no setup: for each job
     * (each option of its first single choice, or the whole tracker without one), the typical
     * days between logs and, where a number field only grows (an odometer), the typical growth.
     * A job needs three logs, two gaps, before it has a rhythm.
     */
    fun learned(tracker: Tracker, entries: List<Entry>, today: LocalDate): List<Rhythm> {
        val choice = tracker.fields.firstOrNull { it.config is FieldConfig.SingleChoice }
        val meter = tracker.fields.firstOrNull { field ->
            field.config is FieldConfig.Number && entries.sortedBy { it.occurredAt }
                .mapNotNull { (it.values[field.id] as? FieldValue.Number)?.scaled }
                .let { readings -> readings.size >= 2 && readings.zipWithNext().all { (a, b) -> b >= a } }
        }
        val jobs: List<Pair<String?, String?>> = (choice?.config as? FieldConfig.SingleChoice)?.options?.map { it.id to it.label }
            ?: listOf(null to null)
        val oldestFirst = entries.sortedBy { it.occurredAt }
        val latestReading = meter?.let { m -> oldestFirst.lastOrNull { it.values[m.id] is FieldValue.Number }?.let { (it.values[m.id] as FieldValue.Number).scaled } }
        return jobs.mapNotNull { (optionId, label) ->
            val logs = oldestFirst.filter { optionId == null || (it.values[choice!!.id] as? FieldValue.SingleChoice)?.optionId == optionId }
            val days = logs.map { it.localDay() }.distinct()
            if (days.size < 3) return@mapNotNull null
            val gap = median(days.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) })
            val readings = meter?.let { m -> logs.mapNotNull { (it.values[m.id] as? FieldValue.Number)?.scaled } }.orEmpty()
            val growth = if (readings.size >= 3) median(readings.zipWithNext { a, b -> b - a }).takeIf { it > 0 } else null
            val last = days.last()
            val lastReading = readings.lastOrNull()
            Rhythm(
                label = label,
                days = gap,
                nextOn = last.plusDays(gap),
                growth = growth,
                meterFieldId = meter?.id.takeIf { growth != null },
                nextAt = if (growth != null && lastReading != null) lastReading + growth else null,
                current = latestReading.takeIf { growth != null },
                today = today,
            )
        }
    }

    private fun median(values: List<Long>): Long = values.sorted().let { it[it.size / 2] }

    fun advance(day: LocalDate, every: Int, unit: IntervalUnit): LocalDate = when (unit) {
        IntervalUnit.DAY -> day.plusDays(every.toLong())
        IntervalUnit.WEEK -> day.plusWeeks(every.toLong())
        IntervalUnit.MONTH -> day.plusMonths(every.toLong())
        IntervalUnit.YEAR -> day.plusYears(every.toLong())
    }
}

/**
 * A rhythm learned from history: the typical [days] between a job's logs and, for a growing
 * number field, the typical [growth] between them. A hint of what is next, never a deadline.
 */
data class Rhythm(
    val label: String?,
    val days: Long,
    val nextOn: LocalDate,
    val growth: Long?,
    val meterFieldId: String?,
    val nextAt: Long?,
    val current: Long?,
    val today: LocalDate,
) {
    val daysLeft: Long get() = ChronoUnit.DAYS.between(today, nextOn)
    val amountLeft: Long? get() = if (nextAt != null && current != null) nextAt - current else null
}
