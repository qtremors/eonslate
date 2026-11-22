package dev.qtremors.eonslate.core.tracker.data.db

import dev.qtremors.eonslate.core.tracker.domain.finance.Share
import dev.qtremors.eonslate.core.tracker.domain.model.Interval
import dev.qtremors.eonslate.core.tracker.domain.backup.storedAsLong
import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tags
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.value.FieldConfigJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Which column of `entry_values` holds each kind, and how values move in and out of it. */
internal object ValueColumns {
    private val optionIds = ListSerializer(String.serializer())

    /** One rule for the database, its triggers and backup checks: [storedAsLong]. */
    fun usesLong(type: FieldType): Boolean = type.storedAsLong

    fun toEntity(entryId: String, fieldId: String, value: FieldValue): EntryValueEntity {
        val long: Long? = when (value) {
            is FieldValue.Number -> value.scaled
            is FieldValue.Money -> value.minor
            is FieldValue.Duration -> value.seconds
            is FieldValue.Rating -> value.value.toLong()
            is FieldValue.YesNo -> if (value.value) 1L else 0L
            is FieldValue.Date -> value.epochDay
            else -> null
        }
        val text: String? = when (value) {
            is FieldValue.SingleChoice -> value.optionId
            // Sorted, so the same selection always stores the same text.
            is FieldValue.MultiChoice -> Json.encodeToString(optionIds, value.optionIds.sorted())
            is FieldValue.Text -> value.text
            else -> null
        }
        return EntryValueEntity(entryId, fieldId, value.type.storageKey, long, text)
    }

    /** The value in [row], or null when its type is unknown or its columns do not fit it. */
    fun toDomain(row: EntryValueEntity): FieldValue? {
        val type = FieldType.fromStorageKey(row.type) ?: return null
        return if (usesLong(type)) {
            val long = row.longValue ?: return null
            when (type) {
                FieldType.NUMBER -> FieldValue.Number(long)
                FieldType.MONEY -> FieldValue.Money(long)
                FieldType.DURATION -> FieldValue.Duration(long)
                FieldType.RATING -> FieldValue.Rating(long.toInt())
                FieldType.YES_NO -> FieldValue.YesNo(long != 0L)
                FieldType.DATE -> FieldValue.Date(long)
                else -> null
            }
        } else {
            val text = row.textValue ?: return null
            when (type) {
                FieldType.SINGLE_CHOICE -> FieldValue.SingleChoice(text)
                FieldType.MULTI_CHOICE -> runCatching { Json.decodeFromString(optionIds, text) }
                    .getOrNull()?.let { FieldValue.MultiChoice(it.toSet()) }
                FieldType.TEXT -> FieldValue.Text(text)
                else -> null
            }
        }
    }
}

internal fun Tracker.toEntity(): TrackerEntity = TrackerEntity(
    id = id,
    name = name.trim(),
    description = description.trim(),
    iconName = iconName,
    colorIndex = colorIndex,
    archived = archived,
    position = position,
    templateId = templateId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = TagColumn.encode(Tags.normalize(tags)),
    scheduleDays = schedule.daysMask,
    timesPerWeek = schedule.timesPerWeek,
    intervals = IntervalColumn.encode(intervals),
)

/** Tags are kept as a JSON array of strings in one column. */
/** Intervals are kept as a JSON array in one column; unreadable text reads as none. */
internal object IntervalColumn {
    private val list = ListSerializer(Interval.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(intervals: List<Interval>): String = json.encodeToString(list, intervals)

    fun decode(text: String): List<Interval> = runCatching { json.decodeFromString(list, text) }.getOrDefault(emptyList())
}

/** A transaction's shares with people, kept as JSON beside it. */
internal object ShareColumn {
    private val list = ListSerializer(Share.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(shares: List<Share>): String = json.encodeToString(list, shares)

    fun decode(text: String): List<Share> = runCatching { json.decodeFromString(list, text) }.getOrDefault(emptyList())
}

internal object TagColumn {
    private val list = ListSerializer(String.serializer())

    fun encode(tags: List<String>): String = Json.encodeToString(list, tags)

    fun decode(text: String): List<String> = runCatching { Json.decodeFromString(list, text) }.getOrDefault(emptyList())
}

/** Fields are stored in list order, so [FieldDefinition.position] is always the index. */
internal fun Tracker.fieldEntities(): List<TrackerFieldEntity> = fields.mapIndexed { index, field ->
    TrackerFieldEntity(
        id = field.id,
        trackerId = id,
        name = field.name.trim(),
        type = field.type.storageKey,
        configJson = FieldConfigJson.encode(field.config),
        required = field.required,
        position = index,
    )
}

/**
 * The domain field in [this] row, or null when its config cannot be read by this version.
 * Such a field is left out rather than failing the whole tracker.
 */
internal fun TrackerFieldEntity.toDomain(): FieldDefinition? {
    val config = FieldConfigJson.decodeOrNull(configJson) ?: return null
    if (config.type.storageKey != type) return null
    return FieldDefinition(
        id = id,
        trackerId = trackerId,
        name = name,
        config = config,
        required = required,
        position = position,
    )
}

internal fun TrackerEntity.toDomain(fields: List<TrackerFieldEntity>): Tracker = Tracker(
    id = id,
    name = name,
    description = description,
    iconName = iconName,
    colorIndex = colorIndex,
    fields = fields.sortedWith(compareBy(TrackerFieldEntity::position, TrackerFieldEntity::id))
        .mapNotNull(TrackerFieldEntity::toDomain),
    archived = archived,
    position = position,
    templateId = templateId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = TagColumn.decode(tags),
    schedule = Schedule.of(scheduleDays, timesPerWeek),
    intervals = IntervalColumn.decode(intervals),
)

internal fun TrackerWithFields.toDomain(): Tracker = tracker.toDomain(fields)

internal fun TrackerSummaryRow.toDomain(): TrackerSummary = TrackerSummary(
    tracker = tracker.toDomain(fields),
    entryCount = entryCount,
    lastEntryAt = lastEntryAt,
)

internal fun Entry.toEntity(): EntryEntity = EntryEntity(
    id = id,
    trackerId = trackerId,
    occurredAt = occurredAt,
    zoneId = zoneId,
    note = note.trim(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Entry.valueEntities(): List<EntryValueEntity> =
    values.map { (fieldId, value) -> ValueColumns.toEntity(id, fieldId, value) }

internal fun EntryWithValues.toDomain(): Entry = Entry(
    id = entry.id,
    trackerId = entry.trackerId,
    occurredAt = entry.occurredAt,
    zoneId = entry.zoneId,
    note = entry.note,
    values = values.mapNotNull { row -> ValueColumns.toDomain(row)?.let { row.fieldId to it } }.toMap(),
    createdAt = entry.createdAt,
    updatedAt = entry.updatedAt,
)
