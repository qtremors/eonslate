package dev.qtremors.eonslate.feature.trackerdetail

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.EntryTime
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.ValueWords
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

/** The calendar day an entry belongs to: the day in the zone it was logged in, not today's zone. */
fun Entry.day(): LocalDate = zoned().toLocalDate()

fun Entry.zoned(): ZonedDateTime = zonedAt(occurredAt, zoneId)

fun EntryTime.day(): LocalDate = zonedAt(occurredAt, zoneId).toLocalDate()

private fun zonedAt(millis: Long, zoneId: String): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()))

/**
 * Every instant that can fall on [day] in some zone: zones run from 14 hours behind UTC to 14
 * ahead, so the day's own start and end widened by 14 hours each way. Entries read for this
 * window are then filtered by [Entry.day].
 */
fun dayWindow(day: LocalDate): LongRange {
    val start = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val widen = 14L * 3_600_000L
    return (start - widen) until (start + 86_400_000L + widen)
}

/** Entries of one day, newest first. */
data class DayGroup(val day: LocalDate, val entries: List<Entry>)

object DetailModels {

    /** Groups [entries] by their own day, newest day first, keeping each day's entries newest first. */
    fun groupByDay(entries: List<Entry>): List<DayGroup> =
        entries
            .groupBy { it.day() }
            .toSortedMap(compareByDescending { it })
            .map { (day, list) -> DayGroup(day, list.sortedWith(compareByDescending<Entry> { it.occurredAt }.thenByDescending { it.createdAt })) }

    /** How many entries fell on each day. */
    fun countsByDay(times: List<EntryTime>): Map<LocalDate, Int> = times.groupingBy { it.day() }.eachCount()

    /**
     * The days shown for [month], in weeks starting on [firstDay]. Days outside the month are
     * null, so every row has seven cells and the first of the month sits under its weekday.
     */
    fun monthGrid(month: YearMonth, firstDay: DayOfWeek): List<List<LocalDate?>> {
        val first = month.atDay(1)
        val lead = Math.floorMod(first.dayOfWeek.value - firstDay.value, 7)
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val padded = cells + List(Math.floorMod(-cells.size, 7)) { null }
        return padded.chunked(7)
    }

    /** The weekdays in display order, starting on [firstDay]. */
    fun weekdays(firstDay: DayOfWeek): List<DayOfWeek> = (0 until 7).map { firstDay.plus(it.toLong()) }

    /**
     * One line saying what an entry holds, in field order, such as "72.5 kg · Morning".
     * Up to [limit] values are shown; an entry with only a note shows the note.
     */
    fun summary(entry: Entry, tracker: Tracker, words: ValueWords, locale: Locale = Locale.getDefault(), limit: Int = 3, datePattern: String? = null, dateLocale: Locale = locale): String {
        val values = tracker.fields.mapNotNull { field ->
            entry.values[field.id]?.let { ValueFormat.format(field, it, words, locale, datePattern, dateLocale) }
        }
        return values.take(limit).joinToString(" · ").ifEmpty { entry.note.lineSequence().firstOrNull().orEmpty() }
    }

    /** Totals for the header: all entries, and those in the last seven days counting today. */
    fun totals(times: List<EntryTime>, today: LocalDate): Totals {
        val weekStart = today.minusDays(6)
        val days = times.map { it.day() }
        return Totals(
            all = times.size,
            lastSevenDays = days.count { it in weekStart..today },
            activeDays = days.distinct().size,
        )
    }

    data class Totals(val all: Int, val lastSevenDays: Int, val activeDays: Int)
}
