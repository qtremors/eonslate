package dev.qtremors.eonslate.core.tracker.domain.insights

import kotlin.math.roundToLong
import kotlin.math.ceil
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
    /** The day of the newest reading, [current]. */
    val readAt: LocalDate? = null,
    /** How much the number field grows in a day, learned from its readings; null until two are far enough apart. */
    val pace: Double? = null,
    /** The weekdays the reading grows on, as a mask; null for every day. */
    val countDays: Int? = null,
) {
    /**
     * The reading today: the newest one, carried forward at the usual [pace] when it is days old.
     * An odometer logged only at each oil change still counts down between them.
     */
    val estimate: Long?
        get() {
            val reading = current ?: return null
            val perDay = pace ?: return reading
            val days = readAt?.let { Intervals.countedDays(it, today, countDays) } ?: return reading
            return if (days > 0) reading + (perDay * days).roundToLong() else reading
        }

    /** True when what is left comes from the usual pace rather than a reading made today. */
    val estimated: Boolean get() = estimate != current

    /** The day [dueAt] should be reached at the usual pace. */
    val amountDueOn: LocalDate?
        get() {
            val perDay = pace?.takeIf { it > 0 } ?: return null
            val at = dueAt ?: return null
            val reading = current ?: return null
            val from = readAt ?: return null
            return Intervals.afterCountedDays(from, ceil((at - reading) / perDay).toLong().coerceAtLeast(0), countDays)
        }

    /** Days until [dueOn]; zero or less once it has come. */
    val daysLeft: Long? get() = dueOn?.let { ChronoUnit.DAYS.between(today, it) }

    /** How much more the field can grow before [dueAt]; zero or less once reached. */
    val amountLeft: Long? get() = estimate?.let { now -> dueAt?.minus(now) }

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
            val readings = amountField?.let { readings(entries, it) }.orEmpty()
            val current = readings.lastOrNull()?.second
            val dueAt = if (interval.amount != null && lastAmount != null) lastAmount + interval.amount else null
            IntervalStatus(interval, label, last, lastAmount, dueOn, dueAt, current, today, readings.lastOrNull()?.first,
                // The usual daily amount when it is set, else the pace the readings show.
                interval.perDay?.toDouble() ?: pace(readings, today, interval.countDays),
                interval.countDays,
            )
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
        val choice = Parts.field(tracker) ?: tracker.fields.firstOrNull { it.config is FieldConfig.SingleChoice }
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
                optionId = optionId,
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

    /** Each day's last reading of a number field, oldest first. */
    private fun readings(entries: List<Entry>, fieldId: String): List<Pair<LocalDate, Long>> =
        entries.sortedBy { it.occurredAt }
            .mapNotNull { entry -> (entry.values[fieldId] as? FieldValue.Number)?.scaled?.let { entry.localDay() to it } }
            .associate { it }.toList()

    /**
     * How much a growing reading rises in a day: from the oldest to the newest reading of the last
     * two years (or of all time with fewer), once they are at least [PACE_MIN_DAYS] apart.
     */
    private fun pace(readings: List<Pair<LocalDate, Long>>, today: LocalDate, mask: Int?): Double? {
        val recent = readings.filter { it.first >= today.minusYears(2) }.takeIf { it.size >= 2 } ?: readings
        val first = recent.firstOrNull() ?: return null
        val last = recent.last()
        if (ChronoUnit.DAYS.between(first.first, last.first) < PACE_MIN_DAYS || last.second <= first.second) return null
        // Per day it grows on: riding only on workdays makes each of those count for more.
        val days = countedDays(first.first, last.first, mask).takeIf { it > 0 } ?: return null
        return (last.second - first.second).toDouble() / days
    }

    private fun counts(day: LocalDate, mask: Int?): Boolean = mask == null || mask and (1 shl (day.dayOfWeek.value - 1)) != 0

    /** The days after [after] up to and including [through] that fall on [mask]'s weekdays. */
    fun countedDays(after: LocalDate, through: LocalDate, mask: Int?): Long {
        val total = ChronoUnit.DAYS.between(after, through)
        if (total <= 0) return 0
        if (mask == null) return total
        val weeks = total / 7
        var counted = weeks * Integer.bitCount(mask)
        var day = after.plusWeeks(weeks)
        while (day < through) {
            day = day.plusDays(1)
            if (counts(day, mask)) counted++
        }
        return counted
    }

    /** The day [count] of [mask]'s weekdays after [from] have passed. */
    fun afterCountedDays(from: LocalDate, count: Long, mask: Int?): LocalDate {
        if (mask == null || count <= 0) return from.plusDays(count.coerceAtLeast(0))
        val perWeek = Integer.bitCount(mask).coerceAtLeast(1)
        var day = from.plusWeeks((count - 1) / perWeek)
        var left = count - (count - 1) / perWeek * perWeek
        while (left > 0) {
            day = day.plusDays(1)
            if (counts(day, mask)) left--
        }
        return day
    }

    private const val PACE_MIN_DAYS = 14L

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
    val optionId: String? = null,
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
