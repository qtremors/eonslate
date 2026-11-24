package dev.qtremors.eonslate.feature.entry

import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.ui.FieldTypes
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import java.util.Currency

private const val DAY_MILLIS = 86_400_000L

/**
 * One field of the log screen: its name with the type's tile, then the input for its type.
 * [error] is shown under the input; [onClear] empties an input that has no text box to clear.
 */
@Composable
internal fun FieldInputCard(
    field: FieldDefinition,
    input: FieldInput?,
    error: String?,
    onChange: ((FieldInput) -> FieldInput) -> Unit,
    onClear: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = FieldTypes.icon(field.type), key = field.type.storageKey, size = 32.dp, iconSize = 18.dp)
                Text(
                    text = field.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                if (field.required) {
                    Text(stringResource(R.string.log_required), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
                val clearable = input != null && when (field.config) {
                    is FieldConfig.Rating, FieldConfig.YesNo, is FieldConfig.SingleChoice,
                    is FieldConfig.MultiChoice, FieldConfig.Date -> true
                    else -> false
                }
                if (clearable) {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = onClear) { Text(stringResource(R.string.log_clear)) }
                }
            }
            FieldInputBody(field, input ?: FieldInput(), onChange)
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldInputBody(field: FieldDefinition, input: FieldInput, onChange: ((FieldInput) -> FieldInput) -> Unit) {
    val haptics = rememberAppHaptics()
    when (val config = field.config) {
        is FieldConfig.Number -> FormField(
            value = input.text,
            onValueChange = { t -> onChange { it.copy(text = t) } },
            label = stringResource(R.string.log_value),
            suffix = config.unit?.takeIf { it.isNotBlank() },
            keyboardType = if (config.decimals > 0 || (config.min ?: -1) < 0) KeyboardType.Decimal else KeyboardType.Number,
        )
        is FieldConfig.Money -> FormField(
            value = input.text,
            onValueChange = { t -> onChange { it.copy(text = t) } },
            label = stringResource(R.string.log_amount),
            supporting = stringResource(R.string.log_amount_hint),
            suffix = currencySymbol(config.currencyCode),
            keyboardType = if (MoneyAmount.fractionDigits(config.currencyCode) > 0) KeyboardType.Decimal else KeyboardType.Number,
        )
        is FieldConfig.Duration -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(
                value = input.text,
                onValueChange = { t -> onChange { it.copy(text = t.filter(Char::isDigit)) } },
                label = stringResource(R.string.log_hours),
                keyboardType = KeyboardType.Number,
                maxLength = 5,
                modifier = Modifier.weight(1f),
            )
            FormField(
                value = input.text2,
                onValueChange = { t -> onChange { it.copy(text2 = t.filter(Char::isDigit)) } },
                label = stringResource(R.string.log_minutes),
                keyboardType = KeyboardType.Number,
                maxLength = 2,
                modifier = Modifier.weight(1f),
            )
        }
        is FieldConfig.Rating -> ConnectedChoices(
            options = (1..config.max).toList(),
            selected = input.number ?: -1,
            label = { it.toString() },
            onSelect = { v -> onChange { it.copy(number = v) } },
        )
        FieldConfig.YesNo -> ConnectedChoices(
            options = listOf(1, 0),
            selected = input.number ?: -1,
            label = { stringResource(if (it == 1) R.string.log_yes else R.string.log_no) },
            onSelect = { v -> onChange { it.copy(number = v) } },
        )
        is FieldConfig.SingleChoice -> ConnectedChoices(
            options = config.options.map { it.id },
            selected = input.choices.firstOrNull() ?: "",
            label = { id -> config.options.first { it.id == id }.label },
            onSelect = { id -> onChange { it.copy(choices = setOf(id)) } },
        )
        is FieldConfig.MultiChoice -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            config.options.forEach { option ->
                val selected = option.id in input.choices
                FilterChip(
                    selected = selected,
                    onClick = {
                        haptics.toggle(!selected)
                        onChange { it.copy(choices = if (selected) it.choices - option.id else it.choices + option.id, touched = true) }
                    },
                    label = { Text(option.label) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                    } else null,
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        is FieldConfig.Text -> FormField(
            value = input.text,
            onValueChange = { t -> onChange { it.copy(text = t) } },
            label = stringResource(R.string.log_text),
            singleLine = !config.multiline,
            minLines = if (config.multiline) 3 else 1,
            maxLength = config.maxLength + 20,
        )
        FieldConfig.Date -> DateInput(input.day) { day -> onChange { it.copy(day = day) } }
    }
}

@Composable
private fun DateInput(day: Long?, onPick: (Long) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { open = true }) {
        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
        Spacer(Modifier.padding(start = 8.dp))
        Text(day?.let { displayDate(it) } ?: stringResource(R.string.log_pick_date))
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day?.times(DAY_MILLIS))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    state.selectedDateMillis?.let { onPick(Math.floorDiv(it, DAY_MILLIS)) }
                    open = false
                }) { Text(stringResource(R.string.log_ok)) }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { open = false }) { Text(stringResource(R.string.log_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun currencySymbol(code: String): String {
    val locale = LocalValueDisplay.current.numberLocale
    return runCatching { Currency.getInstance(code).getSymbol(locale) }.getOrDefault(code)
}
