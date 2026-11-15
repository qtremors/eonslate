package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.math.BigDecimal
import java.time.LocalDate

/** The best a field has reached and the day it happened. */
data class Record(val value: BigDecimal, val day: LocalDate)

object Records {

    /**
     * The record for a numeric field: the highest single value of a plain number (the heaviest
     * lift, the most pages), and the highest day of money or time, which add up over a day (the
     * longest night of sleep, the biggest spending day). Ties go to the earliest day, since that
     * is when the record was set. Null when nothing is logged.
     */
    fun best(field: FieldDefinition, entries: List<Entry>): Record? {
        val dated = entries.sortedBy { it.occurredAt }.map { it.localDay() to it }
        val candidates: List<Pair<LocalDate, BigDecimal>> = when (val config = field.config) {
            is FieldConfig.Number -> dated.mapNotNull { (day, e) ->
                (e.values[field.id] as? FieldValue.Number)?.let { day to BigDecimal.valueOf(it.scaled, config.decimals) }
            }
            is FieldConfig.Money -> {
                val digits = MoneyAmount.fractionDigits(config.currencyCode)
                daySums(dated) { (it.values[field.id] as? FieldValue.Money)?.let { v -> BigDecimal.valueOf(v.minor, digits) } }
            }
            is FieldConfig.Duration -> daySums(dated) { (it.values[field.id] as? FieldValue.Duration)?.let { v -> BigDecimal.valueOf(v.seconds) } }
            else -> emptyList()
        }
        var best: Pair<LocalDate, BigDecimal>? = null
        candidates.forEach { candidate ->
            if (best == null || candidate.second > best!!.second) best = candidate
        }
        return best?.let { Record(it.second, it.first) }
    }

    private fun daySums(dated: List<Pair<LocalDate, Entry>>, value: (Entry) -> BigDecimal?): List<Pair<LocalDate, BigDecimal>> =
        dated.groupBy({ it.first }, { it.second })
            .mapNotNull { (day, list) -> list.mapNotNull(value).takeIf { it.isNotEmpty() }?.let { day to it.reduce(BigDecimal::add) } }
            .sortedBy { it.first }
}

/** How many active trackers have something logged today, for the line on Home. */
data class TodayProgress(val logged: Int, val total: Int)

object Today {
    fun progress(trackerIds: Collection<String>, dayOf: Map<String, List<LocalDate>>, today: LocalDate): TodayProgress =
        TodayProgress(logged = trackerIds.count { id -> dayOf[id].orEmpty().any { it == today } }, total = trackerIds.size)
}
