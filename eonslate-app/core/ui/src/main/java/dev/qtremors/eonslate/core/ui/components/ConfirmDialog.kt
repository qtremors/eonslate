package dev.qtremors.eonslate.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.ui.R

/**
 * Asks before something that cannot be taken back: an icon, the question as the title, one
 * sentence on what happens, a filled button naming the action, and Cancel.
 */
@Composable
fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError)
                } else ButtonDefaults.buttonColors(),
                onClick = {
                    haptics.confirm()
                    onConfirm()
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
