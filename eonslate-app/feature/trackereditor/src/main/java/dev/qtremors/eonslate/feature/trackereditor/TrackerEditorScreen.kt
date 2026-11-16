package dev.qtremors.eonslate.feature.trackereditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.time.UuidIdGenerator
import dev.qtremors.eonslate.core.tracker.ui.FieldTypes
import dev.qtremors.eonslate.core.tracker.ui.Templates
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.tracker.ui.message
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.MainActionButton
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.components.ToggleRow

@Composable
fun TrackerEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = onDone,
    viewModel: TrackerEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel.templateId, state.loading) {
        val spec = viewModel.templateId?.let(Templates::byId) ?: return@LaunchedEffect
        if (state.loading) return@LaunchedEffect
        viewModel.applyTemplate { currency ->
            Templates.instantiate(spec = spec, text = context::getString, ids = UuidIdGenerator, currency = currency, now = 0)
        }
    }
    LaunchedEffect(state.done) { if (state.done) (if (state.deleted) onDeleted else onDone)() }

    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val leave = { if (state.dirty) confirmDiscard = true else onDone() }
    BackHandler(enabled = state.dirty && state.sheet == null) { confirmDiscard = true }

    val draft = state.draft
    EonslateScaffold(
        title = stringResource(if (draft?.isNew != false) R.string.editor_title_new else R.string.editor_title_edit),
        onBack = leave,
        modifier = modifier,
        floatingActionButton = {
            if (draft != null) {
                MainActionButton(
                    text = stringResource(R.string.editor_save),
                    icon = Icons.Rounded.Check,
                    onClick = viewModel::save,
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.missing || draft == null -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(R.string.editor_missing_title),
                message = stringResource(R.string.editor_missing_message),
                actionLabel = stringResource(R.string.editor_missing_action),
                onAction = onDone,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> EditorContent(
                state = state,
                draft = draft,
                viewModel = viewModel,
                onDelete = { confirmDelete = true },
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
            )
        }
    }

    state.sheet?.let { sheet ->
        FieldSheetContent(
            sheet = sheet,
            originalType = state.originalTypes[sheet.draft.id],
            errors = state.sheetErrors,
            limits = state.sheetLimits,
            viewModel = viewModel,
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            icon = Icons.Rounded.Warning,
            title = stringResource(R.string.editor_discard_title),
            message = stringResource(R.string.editor_discard_message),
            confirmLabel = stringResource(R.string.editor_discard_confirm),
            onConfirm = {
                confirmDiscard = false
                onDone()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            icon = Icons.Rounded.DeleteForever,
            title = stringResource(R.string.editor_delete_title, draft?.name?.trim().orEmpty()),
            message = stringResource(R.string.editor_delete_message),
            confirmLabel = stringResource(R.string.editor_delete_confirm),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun EditorContent(
    state: EditorState,
    draft: TrackerDraft,
    viewModel: TrackerEditorViewModel,
    onDelete: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val trackerErrors = state.trackerErrors
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "preview") {
            TrackerSlate(
                name = draft.name.trim().ifEmpty { stringResource(R.string.editor_preview_name) },
                iconName = draft.iconName,
                colorIndex = draft.colorIndex,
                key = draft.id,
                supporting = if (draft.fields.isEmpty()) stringResource(R.string.editor_preview_no_fields)
                else draft.fields.joinToString(" · ") { it.name.trim() },
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item(key = "details") {
            SectionHeader(stringResource(R.string.editor_section_details))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    value = draft.name,
                    onValueChange = viewModel::setName,
                    label = stringResource(R.string.editor_name),
                    error = trackerErrors.firstOrNull {
                        it is ValidationError.TrackerNameBlank ||
                            it is ValidationError.TrackerNameTooLong
                    }?.message(),
                    maxLength = Tracker.NAME_MAX_LENGTH + 10,
                )
                FormField(
                    value = draft.description,
                    onValueChange = viewModel::setDescription,
                    label = stringResource(R.string.editor_description),
                    supporting = stringResource(R.string.editor_description_hint),
                    singleLine = false,
                    minLines = 2,
                    maxLength = Tracker.DESCRIPTION_MAX_LENGTH,
                )
            }
        }

        item(key = "icon") {
            SectionHeader(stringResource(R.string.editor_section_icon))
            IconPicker(selected = draft.iconName, colorIndex = draft.colorIndex, onSelect = viewModel::setIcon)
        }

        item(key = "color") {
            SectionHeader(stringResource(R.string.editor_section_color))
            PigmentPicker(selected = draft.colorIndex, onSelect = viewModel::setColor)
        }

        item(key = "fields-header") {
            SectionHeader(
                text = stringResource(R.string.editor_section_fields),
                trailing = {
                    Text(
                        text = pluralStringResource(R.plurals.editor_field_count, draft.fields.size, draft.fields.size, Tracker.MAX_FIELDS),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        itemsIndexed(draft.fields, key = { _, f -> "field-" + f.id }) { index, field ->
            ConnectedItem(index = index, count = draft.fields.size, modifier = Modifier.animateItem()) {
                FieldRow(
                    field = field,
                    trackerId = draft.id,
                    index = index,
                    count = draft.fields.size,
                    error = state.errorsFor(field.id).firstOrNull()?.message()
                        ?: state.badLimits[field.id]?.let { stringResource(R.string.editor_bad_limit) },
                    viewModel = viewModel,
                )
            }
        }
        item(key = "add-field") {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (trackerErrors.any { it == ValidationError.NoFields }) {
                    Text(
                        text = stringResource(R.string.editor_needs_field),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    )
                }
                if (draft.fields.size < Tracker.MAX_FIELDS) {
                    FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::addField) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(stringResource(R.string.editor_add_field), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        if (!draft.isNew) {
            item(key = "manage") {
                SectionHeader(stringResource(R.string.editor_section_manage))
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.editor_archive),
                        checked = draft.archived,
                        onCheckedChange = viewModel::setArchived,
                        icon = Icons.Rounded.Archive,
                    )
                    ConnectedRow(
                        title = stringResource(R.string.editor_delete),
                        leading = {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: FieldDraft,
    trackerId: String,
    index: Int,
    count: Int,
    error: String?,
    viewModel: TrackerEditorViewModel,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    val built = field.build(trackerId).field
    Box {
        ConnectedRow(
            title = field.name.trim(),
            supporting = error ?: built?.let { fieldSummary(it) },
            leading = { IconBadge(icon = FieldTypes.icon(field.type), key = field.type.storageKey) },
            trailing = if (error != null) {
                { Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            } else null,
            onClick = { viewModel.editField(field.id) },
            onLongClick = { menu = true },
        )
        EonslateMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            groups = listOf(
                listOf(MenuAction(stringResource(R.string.editor_field_edit), Icons.Rounded.Edit) { viewModel.editField(field.id) }),
                listOf(
                    MenuAction(stringResource(R.string.editor_field_up), Icons.Rounded.KeyboardArrowUp, enabled = index > 0) {
                        viewModel.moveField(field.id, -1)
                    },
                    MenuAction(stringResource(R.string.editor_field_down), Icons.Rounded.KeyboardArrowDown, enabled = index < count - 1) {
                        viewModel.moveField(field.id, 1)
                    },
                ),
                listOf(MenuAction(stringResource(R.string.editor_field_remove), Icons.Rounded.Delete, destructive = true) {
                    viewModel.removeField(field.id)
                }),
            ),
        )
    }
}

/** One line describing a field's type and rules, such as "Number · kg · 1 decimal · Required". */
@Composable
internal fun fieldSummary(field: FieldDefinition): String {
    val parts = mutableListOf(stringResource(FieldTypes.labelRes(field.type)))
    when (val c = field.config) {
        is FieldConfig.Number -> {
            c.unit?.let(parts::add)
            if (c.decimals > 0) parts += pluralStringResource(R.plurals.editor_decimals, c.decimals, c.decimals)
        }
        is FieldConfig.Money -> parts += c.currencyCode
        is FieldConfig.Rating -> parts += stringResource(R.string.editor_rating_of, c.max)
        is FieldConfig.SingleChoice ->
            parts += pluralStringResource(R.plurals.editor_choice_count, c.options.size, c.options.size)
        is FieldConfig.MultiChoice ->
            parts += pluralStringResource(R.plurals.editor_choice_count, c.options.size, c.options.size)
        is FieldConfig.Text ->
            if (c.multiline) parts += stringResource(R.string.editor_text_multiline_short)
        else -> Unit
    }
    if (field.required) parts += stringResource(R.string.editor_required_short)
    return parts.joinToString(" · ")
}
