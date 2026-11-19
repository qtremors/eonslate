package dev.qtremors.eonslate.feature.finance

import com.caverock.androidsvg.SVG
import android.graphics.Canvas
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Service
import dev.qtremors.eonslate.core.tracker.domain.finance.Services
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.InlineSearchField
import dev.qtremors.eonslate.core.ui.components.MonogramBadge
import dev.qtremors.eonslate.core.ui.theme.EonslateDisplay
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Custom icons: any PNG, JPEG, WebP or SVG, decoded and drawn again as a 128 px square PNG, which
 * drops whatever else the file carried. Stored by the hash of that PNG; backups take them along.
 */
internal object CustomIcons {
    const val PREFIX = "file:"
    private const val SIZE = 128
    private const val MAX_INPUT = 20L * 1024 * 1024

    fun dir(context: Context) = File(context.filesDir, "icons")

    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_INPUT) return@runCatching null
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            } ?: return@runCatching null
            decode(bytes)?.let { save(context, it) }
        }.getOrNull()
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val head = String(bytes, 0, minOf(bytes.size, 512), Charsets.UTF_8).trimStart('\uFEFF', ' ', '\r', '\n', '\t')
        if (head.startsWith("<svg") || head.startsWith("<?xml")) {
            val source = bytes.toString(Charsets.UTF_8)
            val lower = source.lowercase()
            if ("<!doctype" in lower || "<!entity" in lower || "<script" in lower) return null
            val svg = SVG.getFromString(source)
            svg.setDocumentWidth(SIZE.toFloat())
            svg.setDocumentHeight(SIZE.toFloat())
            return Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also { svg.renderToCanvas(Canvas(it)) }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val side = minOf(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        return Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
    }

    private fun save(context: Context, bitmap: Bitmap): String {
        val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) }
        dir(context).mkdirs()
        File(dir(context), "$hash.png").writeBytes(png)
        return PREFIX + hash
    }

    fun load(context: Context, icon: String): ImageBitmap? {
        val hash = icon.removePrefix(PREFIX).takeIf { Regex("[0-9a-f]{64}").matches(it) } ?: return null
        val file = File(dir(context), "$hash.png").takeIf { it.isFile } ?: return null
        return runCatching { decode(file.readBytes())?.asImageBitmap() }.getOrNull()
    }
}

/** A subscription's tile: its own image, its service's color, or a letter tile. */
@Composable
internal fun SubscriptionIcon(icon: String?, name: String, size: Dp = 40.dp, color: Int? = null) {
    val context = LocalContext.current
    if (icon != null && icon.startsWith(CustomIcons.PREFIX)) {
        val image by produceState<ImageBitmap?>(null, icon) { value = withContext(Dispatchers.IO) { CustomIcons.load(context, icon) } }
        image?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(SlateTileShape))
            return
        }
    }
    val service = Services.byIcon(icon)
    val symbol = icon?.removePrefix("symbol:")?.let(::financeIcon)
    when {
        symbol != null -> FinanceBadge(icon?.removePrefix("symbol:"), color, name, size = size)
        service != null -> ServiceTile(service, size, color)
        else -> MonogramBadge(name.ifBlank { "?" }, name, size = size, colors = color?.let { financeBadgeColors(it, name) })
    }
}

@Composable
private fun ServiceTile(service: Service, size: Dp, color: Int? = null) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colors = remember(service.id, dark, color) {
        BadgePalette.forColor(Color(color?.or(0xFF000000.toInt()) ?: service.color.toInt()), dark)
    }
    Box(Modifier.size(size).background(colors.container, SlateTileShape), contentAlignment = Alignment.Center) {
        Text(service.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontFamily = EonslateDisplay), color = colors.content)
    }
}

/** The icon row of the subscription editor, and the sheet to change it. */
@Composable
internal fun IconChoice(icon: String?, name: String, onPick: (icon: String?, service: Service?) -> Unit, busy: Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubscriptionIcon(icon, name, 48.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(R.string.finance_icon), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Services.byIcon(icon)?.name ?: stringResource(if (icon == null) R.string.finance_icon_letter else R.string.finance_icon_image), style = MaterialTheme.typography.bodyLarge)
        }
        FilledTonalButton(shapes = ButtonDefaults.shapes(), enabled = !busy, onClick = { open = true }) { Text(stringResource(R.string.finance_icon_change)) }
    }
    if (open) IconSheet(onDismiss = { open = false }) { picked, service ->
        onPick(picked, service)
        open = false
    }
}

@Composable
private fun IconSheet(onDismiss: () -> Unit, onPick: (String?, Service?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val image = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val icon = CustomIcons.import(context, uri)
            if (icon != null) onPick(icon, null) else failed = true
        }
    }
    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.finance_icon)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { image.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/svg+xml")) }) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text(stringResource(R.string.finance_icon_choose), modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { onPick(null, null) }) { Text(stringResource(R.string.finance_icon_letter)) }
            }
            if (failed) Text(stringResource(R.string.finance_icon_failed), color = MaterialTheme.colorScheme.error)
            InlineSearchField(query, { query = it }, stringResource(R.string.finance_icon_search))
            LazyVerticalGrid(GridCells.Adaptive(88.dp), modifier = Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Services.search(query), key = { it.id }) { service ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(88.dp).clip(SlateTileShape).clickable { onPick(Services.PREFIX + service.id, service) }.padding(4.dp),
                    ) {
                        ServiceTile(service, 48.dp)
                        Text(service.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
