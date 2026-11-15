package dev.qtremors.eonslate.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.ValueTextStyle

/** Every open source part of Eonslate with its authors and license; tap one to read it. */
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier, parts: List<OpenSourcePart> = OpenSource.parts) {
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    parts.firstOrNull { it.name == openName }?.let { part -> LicenseSheet(part, onDismiss = { openName = null }) }
    EonslateScaffold(title = stringResource(R.string.licenses_title), onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item(key = "header") { SectionHeader(stringResource(R.string.licenses_section)) }
            itemsIndexed(parts, key = { _, part -> part.name }) { index, part ->
                ConnectedItem(index = index, count = parts.size) {
                    ConnectedRow(
                        title = part.name,
                        supporting = part.authors + " · " + part.license.title,
                        onClick = { openName = part.name },
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseSheet(part: OpenSourcePart, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(part.license) {
        part.license.text?.let { id -> context.resources.openRawResource(id).bufferedReader().use { it.readText() } }
    }
    EonslateBottomSheet(onDismissRequest = onDismiss, title = part.name) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(part.authors + " · " + part.license.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            part.notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (text != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    SelectionContainer {
                        Text(text, style = ValueTextStyle, modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()))
                    }
                }
            }
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { open(context, part.url) }) { Text(stringResource(R.string.licenses_project)) }
        }
    }
}

/** Eonslate's privacy policy, in the app, so it can be read without the internet. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val sections = listOf(
        R.string.privacy_collect_title to R.string.privacy_collect,
        R.string.privacy_offline_title to R.string.privacy_offline,
        R.string.privacy_where_title to R.string.privacy_where,
        R.string.privacy_permissions_title to R.string.privacy_permissions,
        R.string.privacy_source_title to R.string.privacy_source,
    )
    EonslateScaffold(title = stringResource(R.string.privacy_title), onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sections.forEach { (title, body) ->
                item(key = title) {
                    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
