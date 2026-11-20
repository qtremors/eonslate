package dev.qtremors.eonslate.feature.trackereditor

import dev.qtremors.eonslate.core.tracker.domain.model.Schedule
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.Tags
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.value.ScaledDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A field as the builder edits it. Inputs a person types (limits) stay as text until the field is
 * built, so a half-typed "12." is never lost or rejected mid-edit. Settings for every type are
 * kept side by side, so switching the type back and forth does not wipe what was entered.
 */
@Serializable
data class FieldDraft(
    val id: String,
    val name: String = "",
    val type: FieldType = FieldType.NUMBER,
    val required: Boolean = false,
    val unit: String = "",
    val decimals: Int = 0,
    val min: String = "",
    val max: String = "",
    val currencyCode: String = "",
    val ratingMax: Int = 5,
    val multiline: Boolean = false,
    val options: List<ChoiceOption> = emptyList(),
    /** Kept from the stored config, as the builder does not edit them yet. */
    val textMaxLength: Int = FieldConfig.Text.DEFAULT_MAX_LENGTH,
    val maxDurationSeconds: Long? = null,
)

/** Which typed limits of a [FieldDraft] could not be read. */
data class LimitProblems(val minInvalid: Boolean = false, val maxInvalid: Boolean = false) {
    val any: Boolean get() = minInvalid || maxInvalid
}

/** A built field, or the limits that kept it from building. */
data class FieldBuild(val field: FieldDefinition?, val problems: LimitProblems)

fun FieldDraft.build(trackerId: String, position: Int = 0): FieldBuild {
    val minValue = parseLimit(min)
    val maxValue = parseLimit(max)
    val problems = LimitProblems(
        minInvalid = type == FieldType.NUMBER && min.isNotBlank() && minValue == null,
        maxInvalid = type == FieldType.NUMBER && max.isNotBlank() && maxValue == null,
    )
    if (problems.any) return FieldBuild(null, problems)

    val config: FieldConfig = when (type) {
        FieldType.NUMBER -> FieldConfig.Number(
            unit = unit.trim().ifEmpty { null },
            decimals = decimals,
            min = minValue,
            max = maxValue,
        )
        FieldType.MONEY -> FieldConfig.Money(currencyCode.trim().uppercase())
        FieldType.DURATION -> FieldConfig.Duration(maxDurationSeconds)
        FieldType.RATING -> FieldConfig.Rating(ratingMax)
        FieldType.YES_NO -> FieldConfig.YesNo
        FieldType.SINGLE_CHOICE -> FieldConfig.SingleChoice(options.map { it.copy(label = it.label.trim()) })
        FieldType.MULTI_CHOICE -> FieldConfig.MultiChoice(options.map { it.copy(label = it.label.trim()) })
        FieldType.TEXT -> FieldConfig.Text(multiline = multiline, maxLength = textMaxLength)
        FieldType.DATE -> FieldConfig.Date
    }
    return FieldBuild(
        FieldDefinition(
            id = id,
            trackerId = trackerId,
            name = name.trim(),
            config = config,
            required = required,
            position = position,
        ),
        problems,
    )
}

private fun FieldDraft.parseLimit(text: String): Long? =
    if (text.isBlank()) null else ScaledDecimal.parse(text, decimals)

/** The draft that edits [this] field, with limits written back as plain decimals. */
fun FieldDefinition.toDraft(defaultCurrency: String): FieldDraft {
    val base = FieldDraft(id = id, name = name, type = type, required = required, currencyCode = defaultCurrency)
    return when (val c = config) {
        is FieldConfig.Number -> base.copy(
            unit = c.unit.orEmpty(),
            decimals = c.decimals,
            min = c.min?.let { ScaledDecimal.format(it, c.decimals) }.orEmpty(),
            max = c.max?.let { ScaledDecimal.format(it, c.decimals) }.orEmpty(),
        )
        is FieldConfig.Money -> base.copy(currencyCode = c.currencyCode)
        is FieldConfig.Duration -> base.copy(maxDurationSeconds = c.maxSeconds)
        is FieldConfig.Rating -> base.copy(ratingMax = c.max)
        FieldConfig.YesNo, FieldConfig.Date -> base
        is FieldConfig.SingleChoice -> base.copy(options = c.options)
        is FieldConfig.MultiChoice -> base.copy(options = c.options)
        is FieldConfig.Text -> base.copy(multiline = c.multiline, textMaxLength = c.maxLength)
    }
}

/** A tracker as the builder edits it. [createdAt] is null until it is first saved. */
@Serializable
data class TrackerDraft(
    val id: String,
    val name: String = "",
    val description: String = "",
    val iconName: String = Tracker.DEFAULT_ICON,
    val colorIndex: Int = 0,
    val fields: List<FieldDraft> = emptyList(),
    val archived: Boolean = false,
    val position: Int? = null,
    val templateId: String? = null,
    val createdAt: Long? = null,
    /** Tags as typed, comma separated; normalized when built. */
    val tags: String = "",
    val scheduleMode: ScheduleMode = ScheduleMode.DAILY,
    /** The weekdays for [ScheduleMode.DAYS], as a mask, Monday lowest. */
    val scheduleDays: Int = WEEKDAYS_MASK,
    /** Days a week for [ScheduleMode.WEEKLY]. */
    val timesPerWeek: Int = 3,
) {
    val isNew: Boolean get() = createdAt == null

    val schedule: Schedule
        get() = when (scheduleMode) {
            ScheduleMode.DAILY -> Schedule.DAILY
            ScheduleMode.DAYS -> Schedule(Schedule.daysOf(scheduleDays)).takeIf { it.isValid() } ?: Schedule.DAILY
            ScheduleMode.WEEKLY -> Schedule(timesPerWeek = timesPerWeek.coerceIn(1, Schedule.MAX_TIMES_PER_WEEK))
        }
}

/** How a tracker's due days are picked in the editor. */
@Serializable
enum class ScheduleMode { DAILY, DAYS, WEEKLY }

/** Monday to Friday, the first choice when picking days. */
internal const val WEEKDAYS_MASK = 31

fun Tracker.toDraft(defaultCurrency: String): TrackerDraft = TrackerDraft(
    id = id,
    name = name,
    description = description,
    iconName = iconName,
    colorIndex = colorIndex,
    fields = fields.map { it.toDraft(defaultCurrency) },
    archived = archived,
    position = position,
    templateId = templateId,
    createdAt = createdAt,
    tags = tags.joinToString(", "),
    scheduleMode = when {
        schedule.isWeekly -> ScheduleMode.WEEKLY
        schedule.isDaily -> ScheduleMode.DAILY
        else -> ScheduleMode.DAYS
    },
    scheduleDays = if (schedule.isDaily || schedule.isWeekly) WEEKDAYS_MASK else schedule.daysMask,
    timesPerWeek = schedule.timesPerWeek.takeIf { it > 0 } ?: 3,
)

/** The tracker to save, or the ids of fields whose limits could not be read. */
data class TrackerBuild(val tracker: Tracker?, val badLimits: Map<String, LimitProblems>)

fun TrackerDraft.build(now: Long, position: Int): TrackerBuild {
    val built = fields.mapIndexed { index, draft -> draft.id to draft.build(id, index) }
    val bad = built.filter { it.second.problems.any }.associate { it.first to it.second.problems }
    if (bad.isNotEmpty()) return TrackerBuild(null, bad)
    val created = createdAt ?: now
    return TrackerBuild(
        Tracker(
            id = id,
            name = name.trim(),
            description = description.trim(),
            iconName = iconName,
            colorIndex = colorIndex,
            fields = built.map { it.second.field!! },
            archived = archived,
            position = position,
            templateId = templateId,
            createdAt = created,
            updatedAt = maxOf(now, created),
            tags = Tags.normalize(tags.split(',')),
            schedule = schedule,
        ),
        emptyMap(),
    )
}

/** The JSON the drafts are kept in across process death. */
internal val DraftJson = Json { ignoreUnknownKeys = true }
