package dev.qtremors.eonslate.core.tracker.domain.csv

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.choiceOptions
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.EntryValidator
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.domain.value.ScaledDecimal
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.Writer
import java.time.Instant
import java.time.ZoneId

object TrackerCsv {
    const val MAX_ENTRIES = 20_000

    private val standard = listOf("occurred_at", "zone_id", "note")
    private val strings = ListSerializer(String.serializer())

    fun write(tracker: Tracker, entries: List<Entry>, output: Writer) {
        Csv.writeRow(output, header(tracker))
        entries.forEach { entry ->
            val row = listOf(Instant.ofEpochMilli(entry.occurredAt).toString(), entry.zoneId, Csv.escapeText(entry.note)) +
                tracker.fields.map { field -> entry.values[field.id]?.let { format(it, field.config) }.orEmpty() }
            Csv.writeRow(output, row)
        }
    }

    /** All rows are validated before anything is inserted. The destination tracker supplies the field types. */
    fun read(source: String, tracker: Tracker, now: Long, ids: IdGenerator): List<Entry> {
        val rows = Csv.rows(source).iterator()
        val expected = header(tracker)
        if (!rows.hasNext()) throw CsvException(CsvProblem.COLUMNS)
        val actual = rows.next()
        if (actual.size != expected.size || actual.toSet() != expected.toSet()) throw CsvException(CsvProblem.COLUMNS)
        val order = expected.map(actual::indexOf)
        val entries = mutableListOf<Entry>()
        var rowNumber = 1
        while (rows.hasNext()) {
            val cells = rows.next()
            rowNumber++
            if (cells.size == 1 && cells[0].isBlank()) continue
            if (entries.size >= MAX_ENTRIES) throw CsvException(CsvProblem.TOO_LARGE)
            if (cells.size != order.size) throw CsvException(CsvProblem.COLUMNS, rowNumber)
            val row = order.map(cells::get)
            entries += readEntry(row, tracker, now, ids, rowNumber)
        }
        return entries
    }

    private fun header(tracker: Tracker) = standard + tracker.fields.map { "field: ${it.name}" }

    private fun readEntry(row: List<String>, tracker: Tracker, now: Long, ids: IdGenerator, rowNumber: Int): Entry {
        val time = runCatching { Instant.parse(row[0]).toEpochMilli() }.getOrNull()
            ?: throw CsvException(CsvProblem.ENTRY, rowNumber)
        val zone = runCatching { ZoneId.of(row[1]).id }.getOrNull()
            ?: throw CsvException(CsvProblem.ENTRY, rowNumber)
        val values = buildMap {
            tracker.fields.forEachIndexed { index, field ->
                val cell = row[standard.size + index]
                if (cell.isNotEmpty()) {
                    val value = parse(cell, field.config) ?: throw CsvException(CsvProblem.ENTRY, rowNumber)
                    put(field.id, value)
                }
            }
        }
        val entry = Entry(ids.newId(), tracker.id, time, zone, Csv.unescapeText(row[2]), values, createdAt = now)
        if (!EntryValidator.validate(entry, tracker).isValid) throw CsvException(CsvProblem.ENTRY, rowNumber)
        return entry
    }

    private fun format(value: FieldValue, config: FieldConfig): String = when (value) {
        is FieldValue.Number -> ScaledDecimal.format(value.scaled, (config as FieldConfig.Number).decimals)
        is FieldValue.Money -> MoneyAmount.format(value.minor, (config as FieldConfig.Money).currencyCode)
        is FieldValue.Duration -> value.seconds.toString()
        is FieldValue.Rating -> value.value.toString()
        is FieldValue.YesNo -> value.value.toString()
        is FieldValue.Date -> EpochDays.format(value.epochDay)
        is FieldValue.Text -> Csv.escapeText(value.text)
        is FieldValue.SingleChoice -> Csv.escapeText(config.choiceOptions.first { it.id == value.optionId }.label)
        is FieldValue.MultiChoice -> Json.encodeToString(strings, config.choiceOptions.filter { it.id in value.optionIds }.map { it.label })
    }

    private fun parse(cell: String, config: FieldConfig): FieldValue? = when (config) {
        is FieldConfig.Number -> cell.takeIf(decimal::matches)?.let { ScaledDecimal.parse(it, config.decimals) }?.let(FieldValue::Number)
        is FieldConfig.Money -> cell.takeIf(decimal::matches)?.let { MoneyAmount.parse(it, config.currencyCode) }?.let(FieldValue::Money)
        is FieldConfig.Duration -> cell.toLongOrNull()?.let(FieldValue::Duration)
        is FieldConfig.Rating -> cell.toIntOrNull()?.let(FieldValue::Rating)
        FieldConfig.YesNo -> cell.lowercase().toBooleanStrictOrNull()?.let(FieldValue::YesNo)
        FieldConfig.Date -> EpochDays.parse(cell)?.let(FieldValue::Date)
        is FieldConfig.Text -> FieldValue.Text(Csv.unescapeText(cell))
        is FieldConfig.SingleChoice -> config.options.firstOrNull { it.label == Csv.unescapeText(cell) }?.id?.let(FieldValue::SingleChoice)
        is FieldConfig.MultiChoice -> parseChoices(cell, config)
    }

    private fun parseChoices(cell: String, config: FieldConfig.MultiChoice): FieldValue.MultiChoice? {
        val labels = runCatching { Json.decodeFromString(strings, cell) }.getOrNull() ?: return null
        return FieldValue.MultiChoice(labels.map { label ->
            config.options.firstOrNull { it.label == label }?.id ?: return null
        }.toSet())
    }

    private val decimal = Regex("""[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)""")
}
