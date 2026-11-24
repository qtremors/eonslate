package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** How many of the due days, or weeks for a weekly target, were done since [since]. */
data class Consistency(val done: Int, val due: Int, val since: LocalDate, val inWeeks: Boolean) {
    val share: Float get() = if (due == 0) 0f else done.toFloat() / due
    val percent: Int get() = (share * 100).toInt()
}

object ConsistencyOf {
    /**
     * Since [start], the due days that were [done]. Today counts only once done, so a day still
     * running never lowers it. A weekly target counts weeks met, and this week only once met.
     * As needed is never due, so it has none.
     */
    fun of(schedule: Schedule, done: Set<LocalDate>, start: LocalDate, today: LocalDate, weekStart: DayOfWeek = DayOfWeek.MONDAY): Consistency? {
        if (schedule.isAsNeeded || start.isAfter(today)) return null
        if (schedule.isWeekly) {
            fun weekOf(day: LocalDate) = day.with(TemporalAdjusters.previousOrSame(weekStart))
            val perWeek = done.filter { !it.isBefore(start) }.groupingBy(::weekOf).eachCount()
            var week = weekOf(start)
            var due = 0
            var met = 0
            while (!week.isAfter(today)) {
                val ok = (perWeek[week] ?: 0) >= schedule.timesPerWeek
                if (week != weekOf(today) || ok) {
                    due++
                    if (ok) met++
                }
                week = week.plusWeeks(1)
            }
            return Consistency(met, due, start, inWeeks = true)
        }
        var day = start
        var due = 0
        var met = 0
        while (!day.isAfter(today)) {
            if (day.dayOfWeek in schedule.days && (day != today || day in done)) {
                due++
                if (day in done) met++
            }
            day = day.plusDays(1)
        }
        return Consistency(met, due, start, inWeeks = false)
    }
}

/** Heat map shades: each day with a value goes to one of four steps by quartile, so one big day does not wash out the rest. */
object HeatLevels {
    const val STEPS = 4

    fun of(values: Map<LocalDate, Double>): Map<LocalDate, Int> {
        val positive = values.filterValues { it > 0 }
        if (positive.isEmpty()) return emptyMap()
        val sorted = positive.values.sorted()
        // Every day the same, such as one entry a day: each one is a full day, not a faint one.
        if (sorted.first() == sorted.last()) return positive.mapValues { STEPS }
        fun cut(q: Double) = sorted[((sorted.size - 1) * q).toInt()]
        val bounds = listOf(cut(0.25), cut(0.5), cut(0.75))
        return positive.mapValues { (_, v) -> 1 + bounds.count { v > it } }
    }
}
