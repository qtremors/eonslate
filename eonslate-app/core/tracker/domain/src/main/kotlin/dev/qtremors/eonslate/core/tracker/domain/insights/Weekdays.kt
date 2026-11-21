package dev.qtremors.eonslate.core.tracker.domain.insights

import java.time.DayOfWeek
import java.time.LocalDate

/** Totals by weekday, in the week's own order, for spotting which days are busiest. */
object Weekdays {
    fun order(firstDay: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { firstDay.plus(it) }

    /** The sum of [values] on each weekday, starting from [firstDay]; days without any are zero. */
    fun totals(values: Map<LocalDate, Double>, firstDay: DayOfWeek = DayOfWeek.MONDAY): List<Pair<DayOfWeek, Double>> {
        val byDay = values.entries.groupBy({ it.key.dayOfWeek }, { it.value }).mapValues { it.value.sum() }
        return order(firstDay).map { it to (byDay[it] ?: 0.0) }
    }

    /** The busiest weekday, or null when nothing was logged. */
    fun busiest(totals: List<Pair<DayOfWeek, Double>>): DayOfWeek? = totals.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
}
