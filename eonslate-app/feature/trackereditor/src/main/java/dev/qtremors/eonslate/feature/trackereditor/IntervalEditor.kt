package dev.qtremors.eonslate.feature.trackereditor

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
)

/** The interval to save, or null when it is not complete. Amounts use the number field's decimals. */
internal fun IntervalDraft.build(fields: List<FieldDraft>): Interval? {
    val choice = job.split('/').takeIf { it.size == 2 }
    val every = if (timeOn) every.trim().toIntOrNull()?.takeIf { it in 1..Interval.MAX_EVERY } else null
    val field = fields.firstOrNull { it.id == amountFieldId && it.type == FieldType.NUMBER }
    val amount = if (amountOn && field != null) ScaledDecimal.parse(amount, field.decimals)?.takeIf { it > 0 } else null
    if ((timeOn && every == null) || (amountOn && amount == null) || (every == null && amount == null)) return null
    return Interval(
        id = id,
        choiceFieldId = choice?.get(0),
        optionId = choice?.get(1),
        every = every,
        unit = unit,
        amountFieldId = field?.id.takeIf { amount != null },
        amount = amount,
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

/**
 * The As needed tracker's "Due again" list: each interval as a row, and a button to add one.
 * Nothing shows beyond the button until an interval exists.
 */
@Composable
internal fun IntervalList(draft: TrackerDraft, onSave: (IntervalDraft) -> Unit, onRemove: (String) -> Unit, newId: () -> String) {
    var editing by remember { mutableStateOf<IntervalDraft?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (draft.intervals.isNotEmpty()) {
            Text(stringResource(R.string.interval_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            ConnectedGroup {
                draft.intervals.forEachIndexed { index, interval ->
                    ConnectedItem(index, draft.intervals.size) {
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
) {
    var d by remember { mutableStateOf(initial) }
    val numbers = fields.filter { it.type == FieldType.NUMBER && it.name.isNotBlank() }
    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(if (isNew) R.string.interval_new else R.string.interval_edit)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.interval_for), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
