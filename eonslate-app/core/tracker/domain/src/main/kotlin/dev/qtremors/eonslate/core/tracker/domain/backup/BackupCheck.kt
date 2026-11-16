package dev.qtremors.eonslate.core.tracker.domain.backup

import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors
import dev.qtremors.eonslate.core.tracker.domain.value.FieldConfigJson

/**
 * True for the kinds whose values are whole numbers in `long_value` (scaled numbers, minor
 * units, seconds, ratings, yes/no, epoch days); the others keep text in `text_value`.
 */
val FieldType.storedAsLong: Boolean
    get() = when (this) {
        FieldType.NUMBER, FieldType.MONEY, FieldType.DURATION, FieldType.RATING,
        FieldType.YES_NO, FieldType.DATE -> true
        FieldType.SINGLE_CHOICE, FieldType.MULTI_CHOICE, FieldType.TEXT -> false
    }

/**
 * Checks that a snapshot's rows fit together before anything on the phone is touched. It holds
 * the same rules the database triggers enforce, so a bad file is refused up front with a clear
 * reason instead of failing halfway through a restore.
 */
object BackupCheck {
    /** Every problem found, as short descriptions for logs and tests; empty means it is sound. */
    fun problems(snapshot: BackupSnapshot, currentSchema: Int): List<String> = buildList {
        if (snapshot.schema > currentSchema) add("schema ${snapshot.schema} is newer than $currentSchema")
        if (snapshot.schema < 1) add("schema ${snapshot.schema} is not a real schema")

        duplicates(snapshot.trackers.map { it.id }).forEach { add("tracker $it appears twice") }
        duplicates(snapshot.fields.map { it.id }).forEach { add("field $it appears twice") }
        duplicates(snapshot.entries.map { it.id }).forEach { add("entry $it appears twice") }
        duplicates(snapshot.values.map { it.entryId to it.fieldId }).forEach { add("value $it appears twice") }
        duplicates(snapshot.reminders.map { it.id }).forEach { add("reminder $it appears twice") }

        snapshot.trackers.forEach { t ->
            if (t.name.isBlank() || t.iconName.isBlank()) add("tracker ${t.id} has no name or icon")
            if (!TrackerColors.isValid(t.colorIndex)) add("tracker ${t.id} has color ${t.colorIndex}")
            if (t.updatedAt < t.createdAt) add("tracker ${t.id} was updated before it was made")
        }

        val trackerIds = snapshot.trackers.mapTo(HashSet()) { it.id }
        snapshot.reminders.forEach { r ->
            if (r.trackerId !in trackerIds) add("reminder ${r.id} belongs to no tracker")
            if (r.minuteOfDay !in 0 until 1440) add("reminder ${r.id} has minute ${r.minuteOfDay}")
            if (r.daysMask !in 1..127) add("reminder ${r.id} has days ${r.daysMask}")
        }

        val fields = snapshot.fields.associateBy { it.id }
        snapshot.fields.forEach { f ->
            if (f.trackerId !in trackerIds) add("field ${f.id} belongs to no tracker")
            if (f.name.isBlank()) add("field ${f.id} has no name")
            val type = FieldType.fromStorageKey(f.type)
            val config = FieldConfigJson.decodeOrNull(f.configJson)
            if (type == null || config == null || config.type != type) add("field ${f.id} has a config that does not fit ${f.type}")
        }

        val entries = snapshot.entries.associateBy { it.id }
        snapshot.entries.forEach { e ->
            if (e.trackerId !in trackerIds) add("entry ${e.id} belongs to no tracker")
            if (e.zoneId.isBlank()) add("entry ${e.id} has no zone")
            if (e.updatedAt < e.createdAt) add("entry ${e.id} was updated before it was made")
        }

        snapshot.values.forEach { v ->
            val field = fields[v.fieldId]
            val entry = entries[v.entryId]
            val name = "value ${v.entryId}/${v.fieldId}"
            when {
                field == null || entry == null -> add("$name has no field or entry")
                field.trackerId != entry.trackerId -> add("$name joins two trackers")
                v.type != field.type -> add("$name is ${v.type} in a ${field.type} field")
            }
            val type = FieldType.fromStorageKey(v.type)
            when {
                type == null -> add("$name has an unknown type")
                type.storedAsLong && (v.longValue == null || v.textValue != null) -> add("$name is not a whole number")
                !type.storedAsLong && (v.textValue == null || v.longValue != null) -> add("$name is not text")
                type == FieldType.YES_NO && v.longValue !in setOf(0L, 1L) -> add("$name is not yes or no")
                type == FieldType.RATING && (v.longValue ?: 0) < 1 -> add("$name is below one star")
                type == FieldType.DURATION && (v.longValue ?: 0) < 0 -> add("$name is a negative time")
            }
        }
    }

    /** Throws [BackupException] with [BackupProblem.INCONSISTENT] or [BackupProblem.NEWER_VERSION]. */
    fun requireSound(snapshot: BackupSnapshot, currentSchema: Int) {
        if (snapshot.schema > currentSchema) throw BackupException(BackupProblem.NEWER_VERSION)
        val found = problems(snapshot, currentSchema)
        if (found.isNotEmpty()) {
            throw BackupException(BackupProblem.INCONSISTENT, IllegalStateException(found.take(5).joinToString("; ")))
        }
    }

    private fun <T> duplicates(items: List<T>): Set<T> {
        val seen = HashSet<T>()
        return items.filterNotTo(LinkedHashSet()) { seen.add(it) }
    }
}
