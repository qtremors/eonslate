package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.ui.label
import dev.qtremors.eonslate.core.tracker.ui.message
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.ToggleRow

private val DECIMAL_CHOICES = listOf(0, 1, 2, 3)
private val RATING_CHOICES = listOf(3, 5, 7, 10)

/** Creates or edits one field: name, type, the type's own settings, and whether it is required. */
@Composable
internal fun FieldSheetContent(
    sheet: FieldSheet,
    originalType: FieldType?,
    errors: List<ValidationError>,
    limits: LimitProblems,
    viewModel: TrackerEditorViewModel,
) {
    val draft = sheet.draft
    fun errorOf(predicate: (ValidationError) -> Boolean) = errors.firstOrNull(predicate)

    EonslateBottomSheet(
        onDismissRequest = viewModel::closeSheet,
        title = stringResource(if (sheet.isNew) R.string.sheet_title_new else R.string.sheet_title_edit),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = draft.name,
                onValueChange = { name -> viewModel.updateSheet { it.copy(name = name) } },
                label = stringResource(R.string.sheet_name),
                placeholder = stringResource(R.string.sheet_name_placeholder),
                error = errorOf {
                    it is ValidationError.FieldNameBlank || it is ValidationError.FieldNameTooLong || it is ValidationError.DuplicateFieldName
                }?.message(),
                maxLength = FieldDefinition.NAME_MAX_LENGTH + 10,
            )

            Text(stringResource(R.string.sheet_type), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            ConnectedChoices(
                options = FieldType.entries,
                selected = draft.type,
                label = { it.label() },
                onSelect = viewModel::setFieldType,
            )
            if (originalType != null && originalType != draft.type) {
                Text(
                    text = stringResource(R.string.sheet_type_change_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            TypeSettings(draft = draft, errors = errors, limits = limits, viewModel = viewModel)

            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.sheet_required),
                    supporting = stringResource(R.string.sheet_required_hint),
                    checked = draft.required,
                    onCheckedChange = { on -> viewModel.updateSheet { it.copy(required = on) } },
                    icon = Icons.Rounded.PriorityHigh,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (!sheet.isNew) {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { viewModel.removeField(draft.id) }) {
                        Text(stringResource(R.string.sheet_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(shapes = ButtonDefaults.shapes(), onClick = viewModel::commitSheet) {
                    Text(stringResource(if (sheet.isNew) R.string.sheet_add else R.string.sheet_done))
                }
            }
        }
    }
}

@Composable
private fun TypeSettings(
    draft: FieldDraft,
    errors: List<ValidationError>,
    limits: LimitProblems,
    viewModel: TrackerEditorViewModel,
) {
    when (draft.type) {
        FieldType.NUMBER -> NumberSettings(draft, errors, limits, viewModel)
        FieldType.MONEY -> FormField(
            value = draft.currencyCode,
            onValueChange = { code -> viewModel.updateSheet { it.copy(currencyCode = code.uppercase()) } },
            label = stringResource(R.string.sheet_currency),
            supporting = stringResource(R.string.sheet_currency_hint),
            error = errors.firstOrNull { it is ValidationError.UnknownCurrency }?.message(),
            capitalization = KeyboardCapitalization.Characters,
            maxLength = 3,
        )
        FieldType.RATING -> Setting(stringResource(R.string.sheet_rating_scale)) {
            ConnectedChoices(
                options = RATING_CHOICES,
                selected = draft.ratingMax.takeIf { it in RATING_CHOICES } ?: 5,
                label = { stringResource(R.string.editor_rating_of, it) },
                onSelect = { max -> viewModel.updateSheet { it.copy(ratingMax = max) } },
            )
        }
        FieldType.SINGLE_CHOICE, FieldType.MULTI_CHOICE -> OptionsEditor(draft, errors, viewModel)
        FieldType.TEXT -> ConnectedGroup {
            ToggleRow(
                title = stringResource(R.string.sheet_text_multiline),
                supporting = stringResource(R.string.sheet_text_multiline_hint),
                checked = draft.multiline,
                onCheckedChange = { on -> viewModel.updateSheet { it.copy(multiline = on) } },
                icon = Icons.AutoMirrored.Rounded.Notes,
            )
        }
        FieldType.DURATION -> Hint(stringResource(R.string.sheet_duration_hint))
        FieldType.YES_NO -> Hint(stringResource(R.string.sheet_yes_no_hint))
        FieldType.DATE -> Hint(stringResource(R.string.sheet_date_hint))
    }
}

@Composable
private fun NumberSettings(draft: FieldDraft, errors: List<ValidationError>, limits: LimitProblems, viewModel: TrackerEditorViewModel) {
    val limitError = if (draft.decimals == 0) stringResource(R.string.sheet_limit_whole)
    else pluralStringResource(R.plurals.sheet_limit_invalid, draft.decimals, draft.decimals)
    FormField(
        value = draft.unit,
        onValueChange = { unit -> viewModel.updateSheet { it.copy(unit = unit) } },
        label = stringResource(R.string.sheet_unit),
        placeholder = stringResource(R.string.sheet_unit_placeholder),
        error = errors.firstOrNull { it is ValidationError.UnitTooLong }?.message(),
        capitalization = KeyboardCapitalization.None,
        maxLength = FieldConfig.Number.UNIT_MAX_LENGTH + 4,
    )
    Setting(stringResource(R.string.sheet_decimals)) {
        ConnectedChoices(
            options = DECIMAL_CHOICES,
            selected = draft.decimals.takeIf { it in DECIMAL_CHOICES } ?: 0,
            label = { it.toString() },
            onSelect = { d -> viewModel.updateSheet { it.copy(decimals = d) } },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(
            value = draft.min,
            onValueChange = { v -> viewModel.updateSheet { it.copy(min = v) } },
            label = stringResource(R.string.sheet_min),
            error = if (limits.minInvalid) limitError else errors.firstOrNull { it is ValidationError.MinAboveMax }?.message(),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
        FormField(
            value = draft.max,
            onValueChange = { v -> viewModel.updateSheet { it.copy(max = v) } },
            label = stringResource(R.string.sheet_max),
            error = if (limits.maxInvalid) limitError else null,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OptionsEditor(draft: FieldDraft, errors: List<ValidationError>, viewModel: TrackerEditorViewModel) {
    Text(stringResource(R.string.sheet_choices), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    errors.firstOrNull { it is ValidationError.NoOptions || it is ValidationError.TooManyOptions }?.let {
        Text(it.message(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    draft.options.forEachIndexed { index, option: ChoiceOption ->
        val error = errors.firstOrNull {
            (it is ValidationError.OptionLabelBlank && it.optionId == option.id) ||
                (it is ValidationError.OptionLabelTooLong && it.optionId == option.id) ||
                (it is ValidationError.DuplicateOptionLabel && it.label.equals(option.label.trim(), ignoreCase = true))
        }
        FormField(
            value = option.label,
            onValueChange = { viewModel.setOptionLabel(option.id, it) },
            label = stringResource(R.string.sheet_choice_n, index + 1),
            error = error?.message(),
            maxLength = ChoiceOption.LABEL_MAX_LENGTH + 10,
            trailingIcon = {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { viewModel.removeOption(option.id) }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.sheet_choice_remove, index + 1))
                }
            },
        )
    }
    if (draft.options.size < FieldConfig.MAX_OPTIONS) {
        FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::addOption) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(stringResource(R.string.sheet_choice_add), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun Setting(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
