package dev.qtremors.eonslate.feature.entry

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.value.DurationSeconds
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.domain.value.ScaledDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What a person has entered for one field so far. One shape serves every type: typed fields use
 * [text] (and [text2] for a duration's minutes), choices use [choices], yes/no and ratings use
 * [number], and dates use [day]. Empty means "not filled in", which is different from a value.
 */
@Serializable
data class FieldInput(
    val text: String = "",
    val text2: String = "",
    val choices: Set<String> = emptySet(),
    /** Yes/no as 1 or 0, a rating, or a duration's leftover seconds; null when unset. */
    val number: Int? = null,
    val day: Long? = null,
    /** True once a multi choice field was touched, so "none of these" can be an answer. */
    val touched: Boolean = false,
)

/** An entry as the log screen edits it. [occurredAt] and [zoneId] say when it happened. */
@Serializable
data class EntryDraft(
    val id: String,
    val trackerId: String,
    val occurredAt: Long,
    val zoneId: String,
    val note: String = "",
    val inputs: Map<String, FieldInput> = emptyMap(),
    /** Null for an entry that was never saved. */
    val createdAt: Long? = null,
) {
    val isNew: Boolean get() = createdAt == null
}

/** The field's input read as an exact value; [Empty] when nothing was entered. */
sealed interface ReadInput {
    data object Empty : ReadInput
    data object Unreadable : ReadInput
    data class Value(val value: FieldValue) : ReadInput
}

object EntryInputs {

    /** Reads [input] for [field] without rounding: too many decimals is unreadable, not rounded. */
    fun read(field: FieldDefinition, input: FieldInput?): ReadInput {
        input ?: return ReadInput.Empty
        return when (val config = field.config) {
            is FieldConfig.Number -> readText(input.text) { ScaledDecimal.parse(it, config.decimals)?.let(FieldValue::Number) }
            is FieldConfig.Money -> readText(input.text) { MoneyAmount.parse(it, config.currencyCode)?.let(FieldValue::Money) }
            is FieldConfig.Duration -> readDuration(input)
            is FieldConfig.Rating -> input.number?.let { ReadInput.Value(FieldValue.Rating(it)) } ?: ReadInput.Empty
            FieldConfig.YesNo -> input.number?.let { ReadInput.Value(FieldValue.YesNo(it != 0)) } ?: ReadInput.Empty
            is FieldConfig.SingleChoice -> input.choices.firstOrNull()?.let { ReadInput.Value(FieldValue.SingleChoice(it)) } ?: ReadInput.Empty
            is FieldConfig.MultiChoice ->
                if (input.choices.isEmpty() && !input.touched) ReadInput.Empty
                else ReadInput.Value(FieldValue.MultiChoice(input.choices))
            is FieldConfig.Text -> if (input.text.isBlank()) ReadInput.Empty else ReadInput.Value(FieldValue.Text(input.text.trim()))
            FieldConfig.Date -> input.day?.let { ReadInput.Value(FieldValue.Date(it)) } ?: ReadInput.Empty
        }
    }

    private inline fun readText(text: String, parse: (String) -> FieldValue?): ReadInput =
        if (text.isBlank()) ReadInput.Empty else parse(text)?.let(ReadInput::Value) ?: ReadInput.Unreadable

    private fun readDuration(input: FieldInput): ReadInput {
        if (input.text.isBlank() && input.text2.isBlank()) return ReadInput.Empty
        val hours = input.text.trim().ifEmpty { "0" }.toLongOrNull()
        val minutes = input.text2.trim().ifEmpty { "0" }.toLongOrNull()
        if (hours == null || minutes == null || minutes !in 0..59) return ReadInput.Unreadable
        return DurationSeconds.of(hours = hours, minutes = minutes, seconds = (input.number ?: 0).toLong())?.let { ReadInput.Value(FieldValue.Duration(it)) } ?: ReadInput.Unreadable
    }

    /** The input that shows [value] for [field], for editing a logged entry. */
    fun toInput(field: FieldDefinition, value: FieldValue): FieldInput = when (value) {
        is FieldValue.Number -> FieldInput(text = ScaledDecimal.format(value.scaled, (field.config as? FieldConfig.Number)?.decimals ?: 0))
        is FieldValue.Money -> FieldInput(text = MoneyAmount.format(value.minor, (field.config as? FieldConfig.Money)?.currencyCode ?: "USD"))
        is FieldValue.Duration -> {
            val parts = DurationSeconds.split(value.seconds)
            // Seconds are not shown, but they ride along so editing never loses them.
            FieldInput(text = parts.hours.toString(), text2 = parts.minutes.toString(), number = parts.seconds.takeIf { it > 0 })
        }
        is FieldValue.Rating -> FieldInput(number = value.value)
        is FieldValue.YesNo -> FieldInput(number = if (value.value) 1 else 0)
        is FieldValue.SingleChoice -> FieldInput(choices = setOf(value.optionId))
        is FieldValue.MultiChoice -> FieldInput(choices = value.optionIds, touched = true)
        is FieldValue.Text -> FieldInput(text = value.text)
        is FieldValue.Date -> FieldInput(day = value.epochDay)
    }

    fun draftOf(entry: Entry, tracker: Tracker): EntryDraft = EntryDraft(
        id = entry.id,
        trackerId = entry.trackerId,
        occurredAt = entry.occurredAt,
        zoneId = entry.zoneId,
        note = entry.note,
        inputs = entry.values.mapNotNull { (id, value) -> tracker.field(id)?.let { id to toInput(it, value) } }.toMap(),
        createdAt = entry.createdAt,
    )
}

/** The entry to save, or the fields whose input could not be read. */
data class EntryBuild(val entry: Entry?, val unreadable: Set<String>)

fun EntryDraft.build(tracker: Tracker, now: Long): EntryBuild {
    val values = mutableMapOf<String, FieldValue>()
    val unreadable = mutableSetOf<String>()
    tracker.fields.forEach { field ->
        when (val read = EntryInputs.read(field, inputs[field.id])) {
            ReadInput.Empty -> Unit
            ReadInput.Unreadable -> unreadable += field.id
            is ReadInput.Value -> values[field.id] = read.value
        }
    }
    if (unreadable.isNotEmpty()) return EntryBuild(null, unreadable)
    val created = createdAt ?: now
    return EntryBuild(
        Entry(
            id = id,
            trackerId = trackerId,
            occurredAt = occurredAt,
            zoneId = zoneId,
            note = note.trim(),
            values = values,
            createdAt = created,
            updatedAt = maxOf(now, created),
        ),
        emptySet(),
    )
}

internal val EntryJson = Json { ignoreUnknownKeys = true }
