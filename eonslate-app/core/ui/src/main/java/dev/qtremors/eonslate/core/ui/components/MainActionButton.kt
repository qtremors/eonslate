package dev.qtremors.eonslate.core.ui.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * The page's main action: Save, Log it, New tracker. One per page, in the accent color. Inside
 * the app shell it is shown by the floating toolbar beside the tabs; elsewhere it floats bottom end.
 *
 * The extended button does not merge its label into its own semantics, so screen readers would
 * meet a nameless button. The label is set on the button itself here, once for every screen.
 */
@Composable
fun MainActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    val shell = LocalShellHost.current
    if (shell != null) {
        shell.Offer(PageAction(text, icon, onClick))
        return
    }
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.semantics {
            contentDescription = text
            role = Role.Button
        }
    )
}
