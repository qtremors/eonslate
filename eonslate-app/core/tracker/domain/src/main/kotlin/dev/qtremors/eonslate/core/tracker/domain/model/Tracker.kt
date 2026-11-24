package dev.qtremors.eonslate.core.tracker.domain.model

/**
 * Something the user keeps a record of: a habit, a mood, a workout, spending, or anything else.
 * A tracker owns an ordered list of [fields]; every [Entry] logged to it fills some of them.
 *
 * @property iconName the name of a rounded Material icon, resolved by the UI. Unknown names fall
 * back to a default icon, so an old backup never breaks the list.
 * @property colorIndex an index into the badge palette in `:core:ui`, below [TrackerColors.COUNT].
 * @property position the order on the home list, smallest first.
 * @property templateId the ready-made tracker an older version created it from, if any. Nothing
 * sets it now; it is kept so trackers and backups from then round-trip unchanged.
 * @property tags short labels for filtering Home, already normalized by [Tags.normalize].
 * @property schedule the days it is due, which Today, progress and streaks follow.
 * @property createdAt epoch milliseconds when the tracker was created.
 * @property updatedAt epoch milliseconds of the last change to the tracker or its fields.
 */
data class Tracker(
    val id: String,
    val name: String,
    val description: String = "",
    val iconName: String = DEFAULT_ICON,
    val colorIndex: Int = 0,
    val fields: List<FieldDefinition> = emptyList(),
    val archived: Boolean = false,
    val position: Int = 0,
    val templateId: String? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val tags: List<String> = emptyList(),
    /** When it is due; every day unless set. */
    val schedule: Schedule = Schedule.DAILY,
    /** When it is due again after a time or an amount since it was last logged. */
    val intervals: List<Interval> = emptyList(),
) {
    /** The field with [fieldId], or null when it is not part of this tracker. */
    fun field(fieldId: String): FieldDefinition? = fields.firstOrNull { it.id == fieldId }

    /**
     * A new tracker with the same look and fields but none of the entries: fresh ids for the
     * tracker and every field (ids are database keys), the same choice options (their ids only
     * need to be unique within a field), not archived, and placed at [position].
     */
    fun duplicate(newId: () -> String, name: String, now: Long, position: Int): Tracker {
        val id = newId()
        return copy(
            id = id,
            name = name.take(NAME_MAX_LENGTH),
            fields = fields.map { it.copy(id = newId(), trackerId = id) },
            archived = false,
            position = position,
            createdAt = now,
            updatedAt = now,
        )
    }

    companion object {
        const val DEFAULT_ICON = "AutoAwesome"
        const val NAME_MAX_LENGTH = 60
        const val DESCRIPTION_MAX_LENGTH = 280
        const val MAX_FIELDS = 24
    }
}

/** The number of colors a tracker can pick from. Matches the badge palette in `:core:ui`. */
object TrackerColors {
    const val COUNT = 17

    fun isValid(index: Int): Boolean = index in 0 until COUNT
}

/** A tracker with what the home list shows about it, read in one query. */
data class TrackerSummary(
    val tracker: Tracker,
    val entryCount: Int,
    /** Epoch milliseconds of the latest entry, or null when nothing is logged yet. */
    val lastEntryAt: Long?,
)
