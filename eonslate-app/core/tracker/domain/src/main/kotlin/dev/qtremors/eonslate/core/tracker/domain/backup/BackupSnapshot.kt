package dev.qtremors.eonslate.core.tracker.domain.backup

import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Everything a backup holds: every tracker (archived ones too), field, entry and value, as the
 * rows the database stores. Rows rather than the domain model, so a restore puts back exactly
 * what was there, ids, positions and exact values included, with nothing re-derived.
 *
 * Settings are not part of a backup: they belong to the phone, and a restore never changes them.
 *
 * @property schema the database version the rows were read from. A restore refuses a newer one.
 *   Older files use empty defaults for the data added in later schemas.
 */
@Serializable
data class BackupSnapshot(
    val schema: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("app_version") val appVersion: String,
    val trackers: List<TrackerRow> = emptyList(),
    val fields: List<FieldRow> = emptyList(),
    val entries: List<EntryRow> = emptyList(),
    val values: List<ValueRow> = emptyList(),
    /** Added in schema 2; a schema 1 file simply has none. */
    val reminders: List<ReminderRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    @SerialName("recurring_payments") val recurring: List<RecurringPayment> = emptyList(),
) {
    /** What a person is shown before they choose to restore it. */
    val summary: BackupSummary
        get() = BackupSummary(
            createdAt = createdAt,
            appVersion = appVersion,
            trackers = trackers.size,
            archived = trackers.count { it.archived },
            entries = entries.size,
            accounts = accounts.size,
            transactions = transactions.size,
        )
}

/** A short description of a backup, or of what is on the phone now. */
data class BackupSummary(
    val createdAt: Long,
    val appVersion: String,
    val trackers: Int,
    val archived: Int,
    val entries: Int,
    val accounts: Int = 0,
    val transactions: Int = 0,
)

@Serializable
data class TrackerRow(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("icon_name") val iconName: String,
    @SerialName("color_index") val colorIndex: Int,
    val archived: Boolean = false,
    val position: Int,
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    /** Added in schema 3. */
    val tags: List<String> = emptyList(),
)

@Serializable
data class FieldRow(
    val id: String,
    @SerialName("tracker_id") val trackerId: String,
    val name: String,
    val type: String,
    @SerialName("config_json") val configJson: String,
    val required: Boolean = false,
    val position: Int,
)

@Serializable
data class EntryRow(
    val id: String,
    @SerialName("tracker_id") val trackerId: String,
    @SerialName("occurred_at") val occurredAt: Long,
    @SerialName("zone_id") val zoneId: String,
    val note: String = "",
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class ReminderRow(
    val id: String,
    @SerialName("tracker_id") val trackerId: String,
    @SerialName("minute_of_day") val minuteOfDay: Int,
    @SerialName("days_mask") val daysMask: Int,
    val enabled: Boolean = true,
    @SerialName("skip_if_logged") val skipIfLogged: Boolean = true,
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class ValueRow(
    @SerialName("entry_id") val entryId: String,
    @SerialName("field_id") val fieldId: String,
    val type: String,
    @SerialName("long_value") val longValue: Long? = null,
    @SerialName("text_value") val textValue: String? = null,
)
