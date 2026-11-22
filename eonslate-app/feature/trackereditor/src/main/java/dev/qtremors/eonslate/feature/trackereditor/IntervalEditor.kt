package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.ui.res.pluralStringResource
import java.time.DayOfWeek
import dev.qtremors.eonslate.core.ui.components.IconBadge
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.Interval
import dev.qtremors.eonslate.core.tracker.domain.model.IntervalUnit
import dev.qtremors.eonslate.core.tracker.domain.value.ScaledDecimal
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import kotlinx.serialization.Serializable

/**
 * One interval as the editor holds it: what counts as the job, and after how long or how much
 * it is due again. Text stays as typed until the tracker is built.
 *
 * @property job "fieldId/optionId" for logs that picked that choice, or empty for any log.
 */
@Serializable
data class IntervalDraft(
    val id: String,
    val job: String = "",
    val timeOn: Boolean = true,
    val every: String = "1",
    val unit: IntervalUnit = IntervalUnit.MONTH,
    val amountOn: Boolean = false,
    val amountFieldId: String = "",
    val amount: String = "",
    /** The usual amount a day, such as 30 km; empty to learn it from the logs. */
    val perDay: String = "",
    /** The weekdays the amount grows on, as a mask; every day by default. */
    val countDays: Int = Interval.ALL_DAYS,
)

/** The interval to save, or null when it is not complete. Amounts use the number field's decimals. */
internal fun IntervalDraft.build(fields: List<FieldDraft>): Interval? {
    val choice = job.split('/').takeIf { it.size == 2 }
    val every = if (timeOn) every.trim().toIntOrNull()?.takeIf { it in 1..Interval.MAX_EVERY } else null
    val field = fields.firstOrNull { it.id == amountFieldId && it.type == FieldType.NUMBER }
    val amount = if (amountOn && field != null) ScaledDecimal.parse(amount, field.decimals)?.takeIf { it > 0 } else null
    val perDay = if (amount != null && perDay.isNotBlank()) ScaledDecimal.parse(perDay, field!!.decimals)?.takeIf { it > 0 } ?: return null else null
    if (amount != null && countDays !in 1..Interval.ALL_DAYS) return null
    if ((timeOn && every == null) || (amountOn && amount == null) || (every == null && amount == null)) return null
    return Interval(
        id = id,
        choiceFieldId = choice?.get(0),
        optionId = choice?.get(1),
        every = every,
        unit = unit,
        amountFieldId = field?.id.takeIf { amount != null },
        amount = amount,
        perDay = perDay,
        countDays = countDays.takeIf { amount != null && it != Interval.ALL_DAYS },
    )
}

internal fun Interval.toDraft(fields: List<FieldDraft>): IntervalDraft = IntervalDraft(
    id = id,
    job = if (choiceFieldId != null && optionId != null) "$choiceFieldId/$optionId" else "",
    timeOn = every != null,
    every = (every ?: 1).toString(),
    unit = unit,
    amountOn = amount != null,
    amountFieldId = amountFieldId.orEmpty(),
    amount = amount?.let { a -> ScaledDecimal.format(a, fields.firstOrNull { it.id == amountFieldId }?.decimals ?: 0) }.orEmpty(),
    perDay = perDay?.let { a -> ScaledDecimal.format(a, fields.firstOrNull { it.id == amountFieldId }?.decimals ?: 0) }.orEmpty(),
    countDays = countDays ?: Interval.ALL_DAYS,
)

/** The jobs an interval can follow: any log, or one option of a single choice field. */
private data class Job(val key: String, val label: String)

@Composable
private fun jobs(fields: List<FieldDraft>): List<Job> =
    listOf(Job("", stringResource(R.string.interval_any_log))) + fields.filter { it.type == FieldType.SINGLE_CHOICE }.flatMap { field ->
        field.options.filter { it.label.isNotBlank() }.map { Job("${field.id}/${it.id}", it.label.trim()) }
    }

@Composable
private fun unitLabel(unit: IntervalUnit): String = stringResource(
    when (unit) {
        IntervalUnit.DAY -> R.string.interval_days
        IntervalUnit.WEEK -> R.string.interval_weeks
        IntervalUnit.MONTH -> R.string.interval_months
        IntervalUnit.YEAR -> R.string.interval_years
    },
)

/** "Oil change · 3000 km" or "Any log · 6 months", as the row reads. */
@Composable
private fun summary(draft: IntervalDraft, fields: List<FieldDraft>): String {
    val job = jobs(fields).firstOrNull { it.key == draft.job }?.label ?: stringResource(R.string.interval_any_log)
    val parts = buildList {
        if (draft.timeOn) add("${draft.every.trim()} ${unitLabel(draft.unit).lowercase()}")
        if (draft.amountOn) add("${draft.amount.trim()} ${fields.firstOrNull { it.id == draft.amountFieldId }?.unit?.trim().orEmpty()}".trim())
    }
    return job + " · " + parts.joinToString(stringResource(R.string.interval_or))
}

/** A part's interval without the job: "Every 3000 km · 30 km a day", "Every 6 months". */
@Composable
private fun partSummary(draft: IntervalDraft, fields: List<FieldDraft>): String {
    val unit = fields.firstOrNull { it.id == draft.amountFieldId }?.unit?.trim().orEmpty()
    val after = buildList {
        if (draft.timeOn) add("${draft.every.trim()} ${unitLabel(draft.unit).lowercase()}")
        if (draft.amountOn) add("${draft.amount.trim()} $unit".trim())
    }.joinToString(stringResource(R.string.interval_or))
    val days = Integer.bitCount(draft.countDays).takeIf { draft.amountOn && draft.countDays != Interval.ALL_DAYS }
    val pace = draft.perDay.trim().takeIf { draft.amountOn && it.isNotEmpty() }?.let { amount ->
        val perDay = stringResource(R.string.interval_per_day_short, "$amount $unit".trim())
        days?.let { perDay + ", " + pluralStringResource(R.plurals.interval_days_a_week, it, it) } ?: perDay
    }
    return listOfNotNull(stringResource(R.string.interval_every_value, after), pace).joinToString(" · ")
}

/** The job keys of a tracker's parts, "fieldId/optionId", with each part's name, in order. */
internal fun partJobs(fields: List<FieldDraft>): List<Pair<String, String>> =
    fields.firstOrNull { it.type == FieldType.SINGLE_CHOICE && it.parts }?.let { field ->
        field.options.filter { it.label.isNotBlank() }.map { "${field.id}/${it.id}" to it.label.trim() }
    }.orEmpty()

/**
 * A tracker's parts, each with when it is due again: tap one to set or change its interval, with
 * that part already chosen. Parts without one show their last log only.
 */
@Composable
internal fun PartIntervals(
    draft: TrackerDraft,
    onSave: (IntervalDraft) -> Unit,
    onRemove: (String) -> Unit,
    newId: () -> String,
    firstDay: DayOfWeek = DayOfWeek.MONDAY,
) {
    val parts = partJobs(draft.fields)
    if (parts.isEmpty()) return
    var editing by remember { mutableStateOf<Pair<IntervalDraft, String>?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.editor_parts_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ConnectedGroup {
            parts.forEachIndexed { index, (key, label) ->
                val interval = draft.intervals.firstOrNull { it.job == key }
                ConnectedItem(index, parts.size) {
                    ConnectedRow(
                        title = label,
                        supporting = when {
                            interval == null -> stringResource(R.string.editor_part_no_interval)
                            interval.build(draft.fields) == null -> stringResource(R.string.interval_incomplete)
                            else -> partSummary(interval, draft.fields)
                        },
                        leading = { IconBadge(if (interval == null) Icons.Rounded.EventBusy else Icons.Rounded.Update, key) },
                        trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                        onClick = { editing = (interval ?: IntervalDraft(id = newId(), job = key)) to label },
                    )
                }
            }
        }
    }
    editing?.let { (current, label) ->
        IntervalSheet(
            initial = current,
            fields = draft.fields,
            isNew = draft.intervals.none { it.id == current.id },
            part = label,
            firstDay = firstDay,
            onSave = {
                onSave(it)
                editing = null
            },
            onRemove = {
                onRemove(current.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/**
 * The As needed tracker's "Due again" list: each interval as a row, and a button to add one.
 * Nothing shows beyond the button until an interval exists.
 */
@Composable
internal fun IntervalList(
    draft: TrackerDraft,
    onSave: (IntervalDraft) -> Unit,
    onRemove: (String) -> Unit,
    newId: () -> String,
    firstDay: DayOfWeek = DayOfWeek.MONDAY,
) {
    var editing by remember { mutableStateOf<IntervalDraft?>(null) }
    // A part's interval is set on its row under Parts, so it is not listed twice.
    val partKeys = partJobs(draft.fields).map { it.first }.toSet()
    val shown = draft.intervals.filter { it.job !in partKeys }
    // A tracker with parts sets its intervals there; this list stays for any it already had.
    if (partKeys.isNotEmpty() && shown.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (shown.isNotEmpty()) {
            Text(stringResource(R.string.interval_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            ConnectedGroup {
                shown.forEachIndexed { index, interval ->
                    ConnectedItem(index, shown.size) {
                        val complete = interval.build(draft.fields) != null
                        ConnectedRow(
                            title = summary(interval, draft.fields),
                            supporting = if (complete) null else stringResource(R.string.interval_incomplete),
                            onClick = { editing = interval },
                        )
                    }
                }
            }
        }
        if (draft.intervals.size < Interval.MAX_COUNT) {
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { editing = IntervalDraft(id = newId()) }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.interval_add), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    editing?.let { current ->
        IntervalSheet(
            initial = current,
            fields = draft.fields,
            isNew = draft.intervals.none { it.id == current.id },
            firstDay = firstDay,
            onSave = {
                onSave(it)
                editing = null
            },
            onRemove = {
                onRemove(current.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalSheet(
    initial: IntervalDraft,
    fields: List<FieldDraft>,
    isNew: Boolean,
    onSave: (IntervalDraft) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    /** The part this interval belongs to; its job is then fixed. */
    part: String? = null,
    firstDay: DayOfWeek = DayOfWeek.MONDAY,
) {
    var d by remember { mutableStateOf(initial) }
    val numbers = fields.filter { it.type == FieldType.NUMBER && it.name.isNotBlank() }
    val title = part?.let { stringResource(R.string.interval_part_title, it) } ?: stringResource(if (isNew) R.string.interval_new else R.string.interval_edit)
    EonslateBottomSheet(onDismissRequest = onDismiss, title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (part == null) Text(stringResource(R.string.interval_for), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            if (part == null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                jobs(fields).forEach { job ->
                    FilterChip(
                        selected = d.job == job.key,
                        onClick = { d = d.copy(job = job.key) },
                        label = { Text(job.label) },
                        shape = MaterialTheme.shapes.medium,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.interval_after_time),
                    checked = d.timeOn,
                    onCheckedChange = { on -> d = d.copy(timeOn = on) },
                    icon = Icons.Rounded.Update,
                )
                if (numbers.isNotEmpty()) {
                    ToggleRow(
                        title = stringResource(R.string.interval_after_amount),
                        supporting = stringResource(R.string.interval_after_amount_hint),
                        checked = d.amountOn,
                        onCheckedChange = { on -> d = d.copy(amountOn = on, amountFieldId = d.amountFieldId.ifEmpty { numbers.first().id }) },
                        icon = Icons.Rounded.Speed,
                    )
                }
            }
            if (d.timeOn) {
                FormField(
                    value = d.every,
                    onValueChange = { d = d.copy(every = it) },
                    label = stringResource(R.string.interval_every),
                    keyboardType = KeyboardType.Number,
                )
                ConnectedChoices(options = IntervalUnit.entries, selected = d.unit, label = { unitLabel(it) }, onSelect = { d = d.copy(unit = it) })
            }
            if (d.amountOn && numbers.isNotEmpty()) {
                if (numbers.size > 1) {
                    ConnectedChoices(
                        options = numbers.map { it.id },
                        selected = d.amountFieldId,
                        label = { id -> numbers.first { it.id == id }.name.trim() },
                        onSelect = { d = d.copy(amountFieldId = it) },
                    )
                }
                FormField(
                    value = d.amount,
                    onValueChange = { d = d.copy(amount = it) },
                    label = stringResource(R.string.interval_amount),
                    supporting = numbers.firstOrNull { it.id == d.amountFieldId }?.unit?.trim()?.ifEmpty { null },
                    keyboardType = KeyboardType.Decimal,
                )
                // Between readings the amount counts on at this pace, so it needs logging only now and then.
                FormField(
                    value = d.perDay,
                    onValueChange = { d = d.copy(perDay = it) },
                    label = stringResource(R.string.interval_per_day),
                    supporting = stringResource(R.string.interval_per_day_hint),
                    suffix = numbers.firstOrNull { it.id == d.amountFieldId }?.unit?.trim()?.ifEmpty { null },
                    keyboardType = KeyboardType.Decimal,
                )
                // Only the days it grows on count, such as workdays for a commute.
                Text(stringResource(R.string.interval_count_days), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    (0L until 7L).map { firstDay.plus(it) }.forEach { day ->
                        val bit = 1 shl (day.value - 1)
                        val on = d.countDays and bit != 0
                        FilterChip(
                            selected = on,
                            onClick = { d = d.copy(countDays = d.countDays xor bit) },
                            label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())) },
                            shape = MaterialTheme.shapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (!isNew) {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = onRemove) {
                        Text(stringResource(R.string.interval_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(shapes = ButtonDefaults.shapes(), enabled = d.build(fields) != null, onClick = { onSave(d) }) {
                    Text(stringResource(R.string.interval_save))
                }
            }
        }
    }
}
