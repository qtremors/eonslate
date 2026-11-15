package dev.qtremors.eonslate.feature.trackereditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.ui.FieldSpec
import dev.qtremors.eonslate.core.tracker.ui.FieldTypes
import dev.qtremors.eonslate.core.tracker.ui.TemplateCategory
import dev.qtremors.eonslate.core.tracker.ui.TemplateSpec
import dev.qtremors.eonslate.core.tracker.ui.Templates
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.InlineSearchField
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.SectionHeader

/**
 * Where a new tracker starts: a blank slate first, then every ready-made template on its shelf.
 * A template opens a preview of its fields first; using it opens the builder already filled in,
 * so anything can still be changed before saving. Search filters by name, description and shelf.
 */
@Composable
fun TemplateGalleryScreen(
    onPick: (templateId: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    templates: List<TemplateSpec> = Templates.all,
) {
    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val shown = remember(query, templates) { Templates.search(query, context::getString, templates) }
    templates.firstOrNull { it.id == previewId }?.let { spec ->
        TemplatePreview(
            spec = spec,
            onUse = {
                previewId = null
                onPick(spec.id)
            },
            onDismiss = { previewId = null },
        )
    }
    EonslateScaffold(title = stringResource(R.string.gallery_title), onBack = onBack, modifier = modifier) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                InlineSearchField(query = query, onQueryChange = { query = it }, placeholder = stringResource(R.string.gallery_search))
            }
            item(key = "blank") {
                TrackerSlate(
                    name = stringResource(R.string.gallery_blank),
                    iconName = "AutoAwesome",
                    colorIndex = 16,
                    key = "blank",
                    supporting = stringResource(R.string.gallery_blank_about),
                    onClick = { onPick(null) },
                )
            }
            if (shown.isEmpty()) {
                item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.gallery_none_title),
                        message = stringResource(R.string.gallery_none_message),
                    )
                }
            }
            TemplateCategory.entries.forEach { category ->
                val shelf = shown.filter { it.category == category }
                if (shelf.isEmpty()) return@forEach
                item(key = "header-" + category.name, span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(category.title))
                }
                items(shelf, key = { it.id }) { spec ->
                TrackerSlate(
                    name = stringResource(spec.name),
                    iconName = spec.iconName,
                    colorIndex = spec.colorIndex,
                    key = spec.id,
                    supporting = stringResource(spec.description) + "\n" +
                        pluralStringResource(R.plurals.gallery_fields, spec.fields.size, spec.fields.size),
                    onClick = { previewId = spec.id },
                )
                }
            }
            item(key = "note", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.gallery_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** What a template holds, field by field, before it is used. */
@Composable
private fun TemplatePreview(spec: TemplateSpec, onUse: () -> Unit, onDismiss: () -> Unit) {
    val resources = LocalContext.current.resources
    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(spec.name)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(spec.description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            ConnectedGroup {
                spec.fields.forEach { field ->
                    val type = fieldType(field)
                    val details = buildList {
                        add(stringResource(FieldTypes.labelRes(type)))
                        if (field is FieldSpec.Number) field.unit?.let { add(stringResource(it)) }
                        if (field is FieldSpec.Choice) add(field.options.joinToString(", ") { option -> resources.getString(option.label) })
                        if (field.required) add(stringResource(R.string.editor_required_short))
                    }
                    ConnectedRow(
                        title = stringResource(field.name),
                        supporting = details.joinToString(" · "),
                        leading = { IconBadge(icon = FieldTypes.icon(type), key = type.storageKey) },
                    )
                }
            }
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = onUse,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.gallery_use)) }
        }
    }
}

private fun fieldType(field: FieldSpec): FieldType = when (field) {
    is FieldSpec.Number -> FieldType.NUMBER
    is FieldSpec.Money -> FieldType.MONEY
    is FieldSpec.Duration -> FieldType.DURATION
    is FieldSpec.Rating -> FieldType.RATING
    is FieldSpec.YesNo -> FieldType.YES_NO
    is FieldSpec.Choice -> if (field.multiple) FieldType.MULTI_CHOICE else FieldType.SINGLE_CHOICE
    is FieldSpec.Text -> FieldType.TEXT
    is FieldSpec.Date -> FieldType.DATE
}
