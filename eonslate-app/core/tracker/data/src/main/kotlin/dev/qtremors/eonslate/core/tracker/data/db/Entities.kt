package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** One tracker. Its fields and entries hang off it and are deleted with it. */
@Entity(
    tableName = "trackers",
    indices = [Index(value = ["archived", "position"])],
)
data class TrackerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    @ColumnInfo(name = "icon_name") val iconName: String,
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    val archived: Boolean,
    val position: Int,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** A JSON array of tag strings; added in schema 3. */
    @ColumnInfo(name = "tags", defaultValue = "[]") val tags: String = "[]",
)

/**
 * One field of a tracker. [type] repeats the `kind` inside [configJson] as a plain column, so
 * triggers and queries can check it without parsing JSON.
 */
@Entity(
    tableName = "tracker_fields",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tracker_id", "position"])],
)
data class TrackerFieldEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tracker_id") val trackerId: String,
    val name: String,
    val type: String,
    @ColumnInfo(name = "config_json") val configJson: String,
    val required: Boolean,
    val position: Int,
)

/** One logged entry. [zoneId] keeps the day it happened on stable after travelling. */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tracker_id", "occurred_at"])],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tracker_id") val trackerId: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    val note: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * One filled field of an entry, stored exactly. Whole-number kinds (number, money, duration,
 * rating, yes/no, date) use [longValue]; text and choice kinds use [textValue]. A multi choice
 * holds its option ids as a JSON array. Integrity triggers keep the right column filled for
 * each [type] and the type equal to its field's.
 */
@Entity(
    tableName = "entry_values",
    primaryKeys = ["entry_id", "field_id"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackerFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["field_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["field_id"])],
)
data class EntryValueEntity(
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "field_id") val fieldId: String,
    val type: String,
    @ColumnInfo(name = "long_value") val longValue: Long?,
    @ColumnInfo(name = "text_value") val textValue: String?,
)

/** A reminder to log [trackerId]; [daysMask] has Monday at bit 0. Deleted with its tracker. */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tracker_id"])],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tracker_id") val trackerId: String,
    @ColumnInfo(name = "minute_of_day") val minuteOfDay: Int,
    @ColumnInfo(name = "days_mask") val daysMask: Int,
    val enabled: Boolean,
    @ColumnInfo(name = "skip_if_logged") val skipIfLogged: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** A tracker read together with its fields. */
data class TrackerWithFields(
    @Embedded val tracker: TrackerEntity,
    @Relation(parentColumn = "id", entityColumn = "tracker_id")
    val fields: List<TrackerFieldEntity>,
)

/** A tracker with its fields and what the home list shows about its entries. */
data class TrackerSummaryRow(
    @Embedded val tracker: TrackerEntity,
    @Relation(parentColumn = "id", entityColumn = "tracker_id")
    val fields: List<TrackerFieldEntity>,
    @ColumnInfo(name = "entry_count") val entryCount: Int,
    @ColumnInfo(name = "last_entry_at") val lastEntryAt: Long?,
)

/** When an entry happened, read without its values. */
data class EntryTimeRow(
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
)

/** When an entry happened and which tracker it belongs to. */
data class TrackerEntryTimeRow(
    @ColumnInfo(name = "tracker_id") val trackerId: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
)

/** An entry read together with its values. */
data class EntryWithValues(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entry_id")
    val values: List<EntryValueEntity>,
)
