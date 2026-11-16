package dev.qtremors.eonslate.feature.trackerdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.ui.FieldTypes
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.rememberValueWords
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.ValueTextStyle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Everything one entry holds: when it happened (with its zone when that differs from the phone's),
 * every filled field in the tracker's order, the note as selectable text, then what can be done.
 */
@Composable
internal fun EntrySheet(
    entry: Entry,
    tracker: Tracker,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    deviceZone: ZoneId = ZoneId.systemDefault(),
) {
    val words = rememberValueWords()
    val colors = MaterialTheme.colorScheme
    val zoned = entry.zoned()
    val whenText = zoned.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))
    val zoneNote = if (zoned.zone.normalized() != deviceZone.normalized()) zoned.zone.id else null
    val filled = tracker.fields.mapNotNull { field -> entry.values[field.id]?.let { field to it } }

    EonslateBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(whenText, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                if (zoneNote != null) {
                    Text(
                        stringResource(R.string.detail_sheet_zone, zoneNote),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primary,
                    )
                }
            }
            if (filled.isNotEmpty()) {
                Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filled.forEach { (field, value) ->
                            Row {
                                IconBadge(icon = FieldTypes.icon(field.type), key = field.type.storageKey, size = 32.dp, iconSize = 18.dp)
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(field.name, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                                    SelectionContainer {
                                        Text(
                                            ValueFormat.format(field, value, words, Locale.getDefault()),
                                            style = ValueTextStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                                            color = colors.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (entry.note.isNotBlank()) {
                Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.detail_sheet_note), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        SelectionContainer {
                            Text(entry.note, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                        }
                    }
                }
            }
            ConnectedGroup {
                ConnectedRow(
                    title = stringResource(R.string.detail_entry_edit),
                    leading = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.onSurfaceVariant) },
                    onClick = onEdit,
                )
                ConnectedRow(
                    title = stringResource(R.string.detail_entry_delete),
                    leading = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
                    onClick = onDelete,
                )
            }
        }
    }
}
