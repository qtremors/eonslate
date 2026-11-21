package dev.qtremors.eonslate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.qtremors.eonslate.core.ui.components.Avatar
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.NavigationRow
import dev.qtremors.eonslate.core.ui.components.PressableCard

/** Where each row of the profile leads. */
class ProfileLinks(
    val settings: () -> Unit,
    val personalization: () -> Unit,
    val categories: () -> Unit,
    val archived: () -> Unit,
    val backup: () -> Unit,
    val privacy: () -> Unit,
    val about: () -> Unit,
)

/**
 * The profile, opened from the picture in the top bar: the person's name, then settings and
 * the places that are visited now and then rather than every day.
 */
@Composable
fun ProfileDialog(name: String, links: ProfileLinks, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Each row closes the profile on its way to its page.
    fun go(to: () -> Unit): () -> Unit = {
        onDismiss()
        to()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.app_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.profile_close))
                    }
                }
                PressableCard(Modifier.fillMaxWidth(), color = colors.surfaceBright, onClick = go(links.personalization)) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Avatar(name, size = 56.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                name.ifBlank { stringResource(R.string.profile_no_name) },
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(stringResource(R.string.profile_local), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.profile_edit), tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
                ConnectedGroup {
                    NavigationRow(stringResource(R.string.tab_settings), Icons.Rounded.Settings, "settings", go(links.settings))
                    NavigationRow(stringResource(R.string.profile_personalization), Icons.Rounded.Palette, "personalization", go(links.personalization))
                    NavigationRow(stringResource(R.string.profile_categories), Icons.AutoMirrored.Rounded.Label, "categories", go(links.categories))
                    NavigationRow(stringResource(R.string.profile_archived), Icons.Rounded.Inventory2, "archived", go(links.archived))
                    NavigationRow(stringResource(R.string.profile_backup), Icons.Rounded.SettingsBackupRestore, "backup", go(links.backup))
                }
                ConnectedGroup {
                    NavigationRow(stringResource(R.string.profile_privacy), Icons.Rounded.Shield, "privacy", go(links.privacy))
                    NavigationRow(stringResource(R.string.profile_about), Icons.Rounded.Info, "about", go(links.about))
                }
            }
        }
    }
}
