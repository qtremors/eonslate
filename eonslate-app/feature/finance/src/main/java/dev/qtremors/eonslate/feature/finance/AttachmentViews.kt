package dev.qtremors.eonslate.feature.finance

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.qtremors.eonslate.core.tracker.domain.finance.Attachment
import dev.qtremors.eonslate.core.tracker.domain.finance.AttachmentKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Attachments
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.MenuAction
import kotlinx.coroutines.launch

internal fun AttachmentKind.icon() = when (this) {
    AttachmentKind.IMAGE -> Icons.Rounded.Image
    AttachmentKind.PDF -> Icons.Rounded.PictureAsPdf
    AttachmentKind.LINK -> Icons.Rounded.Link
}

/** An attachment's name: its own, else a link's site or the kind. */
@Composable
internal fun Attachment.title(): String = name.ifBlank {
    when (kind) {
        AttachmentKind.LINK -> Attachments.host(target)
        AttachmentKind.IMAGE -> stringResource(R.string.attach_photo_name)
        AttachmentKind.PDF -> stringResource(R.string.attach_pdf_name)
    }
}

/** "2 attachments", for a row's details. */
@Composable
internal fun attachedText(count: Int): String? = count.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.attach_count, it, it) }

/**
 * A transaction's receipts and links in its editor: each opens on a tap and goes with its ×, and
 * Attach adds a photo from the gallery or camera, a PDF, or a link such as the product's page.
 */
@Composable
internal fun AttachmentsSection(attachments: List<Attachment>, onChange: (List<Attachment>) -> Unit, enabled: Boolean, newId: () -> String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var linking by rememberSaveable { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Attachment?>(null) }
    var cannotOpen by rememberSaveable { mutableStateOf(false) }
    val full = attachments.size >= Attachments.PER_TRANSACTION
    val add = { kind: AttachmentKind, hash: String?, name: String? ->
        importing = false
        failed = hash == null
        if (hash != null) onChange(attachments + Attachment(newId(), "", kind, hash, name.orEmpty()))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        importing = true
        scope.launch { add(AttachmentKind.IMAGE, AttachmentFiles.importImage(context, uri), null) }
    }
    var shot by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = shot
        if (!taken || uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch { add(AttachmentKind.IMAGE, AttachmentFiles.importImage(context, uri), null) }
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        importing = true
        scope.launch { add(AttachmentKind.PDF, AttachmentFiles.importPdf(context, uri), AttachmentFiles.displayName(context, uri)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (attachments.isNotEmpty()) ConnectedGroup {
            attachments.forEachIndexed { index, attachment ->
                ConnectedItem(index, attachments.size) {
                    ConnectedRow(
                        title = attachment.title(),
                        supporting = when (attachment.kind) {
                            AttachmentKind.LINK -> attachment.target
                            AttachmentKind.IMAGE -> stringResource(R.string.attach_photo_name)
                            AttachmentKind.PDF -> stringResource(R.string.attach_pdf_name)
                        },
                        leading = { Icon(attachment.kind.icon(), contentDescription = null) },
                        trailing = {
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onChange(attachments - attachment) }, enabled = enabled) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.attach_remove, attachment.title()))
                            }
                        },
                        onClick = {
                            if (attachment.kind == AttachmentKind.IMAGE) viewing = attachment
                            else cannotOpen = !AttachmentFiles.open(context, attachment)
                        },
                    )
                }
            }
        }
        Box {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = { menu = true }, enabled = enabled && !importing && !full) {
                if (importing) LoadingIndicator(Modifier.size(20.dp)) else Icon(Icons.Rounded.AttachFile, contentDescription = null)
                Text(stringResource(R.string.attach_add), modifier = Modifier.padding(start = 8.dp))
            }
            EonslateMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                groups = listOf(
                    listOf(
                        MenuAction(stringResource(R.string.attach_from_photos), Icons.Rounded.Image) {
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        MenuAction(stringResource(R.string.attach_take_photo), Icons.Rounded.PhotoCamera) {
                            AttachmentFiles.cameraUri(context).let {
                                shot = it
                                camera.launch(it)
                            }
                        },
                        MenuAction(stringResource(R.string.attach_pdf), Icons.Rounded.PictureAsPdf) { pdf.launch(arrayOf("application/pdf")) },
                    ),
                    listOf(MenuAction(stringResource(R.string.attach_link), Icons.Rounded.Link) { linking = true }),
                ),
            )
        }
        if (failed) Text(stringResource(R.string.attach_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (cannotOpen) Text(stringResource(R.string.attach_cannot_open), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (linking) LinkDialog(onDismiss = { linking = false }) { link, name ->
        linking = false
        onChange(attachments + Attachment(newId(), "", AttachmentKind.LINK, link, name))
    }
    viewing?.let { ImageViewer(it) { viewing = null } }
}

/** A link and what to call it; the address may be typed without https://. */
@Composable
private fun LinkDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var address by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val link = Attachments.link(address)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
        title = { Text(stringResource(R.string.attach_link)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    address, { address = it }, stringResource(R.string.attach_link_address),
                    placeholder = stringResource(R.string.attach_link_hint),
                    error = if (address.isNotBlank() && link == null) stringResource(R.string.attach_link_invalid) else null,
                    keyboardType = KeyboardType.Uri, maxLength = Attachments.LINK_LIMIT,
                )
                FormField(name, { name = it }, stringResource(R.string.attach_link_name), placeholder = stringResource(R.string.attach_link_name_hint), maxLength = Attachment.NAME_LIMIT)
            }
        },
        confirmButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = { link?.let { onAdd(it, name.trim()) } }, enabled = link != null) {
                Text(stringResource(R.string.attach_link_add))
            }
        },
        dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.finance_cancel)) } },
    )
}

/** A receipt's photo filling the screen; pinch to zoom, drag to look around. */
@Composable
internal fun ImageViewer(attachment: Attachment, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var missing by remember { mutableStateOf(false) }
    LaunchedEffect(attachment.target) {
        bitmap = AttachmentFiles.loadImage(context, attachment)
        missing = bitmap == null
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            bitmap?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = attachment.title(),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                offset = if (scale == 1f) Offset.Zero else offset + pan
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            } ?: if (missing) Text(stringResource(R.string.attach_missing), color = Color.White) else LoadingIndicator()
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.attach_close), tint = Color.White)
            }
        }
    }
}
