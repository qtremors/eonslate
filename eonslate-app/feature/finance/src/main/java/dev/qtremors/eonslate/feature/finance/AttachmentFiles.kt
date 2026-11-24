package dev.qtremors.eonslate.feature.finance

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import dev.qtremors.eonslate.core.tracker.domain.finance.Attachment
import dev.qtremors.eonslate.core.tracker.domain.finance.AttachmentKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Attachments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Attachment files on the device: `files/attachments/<hash>.jpg` or `.pdf`. A photo is made at
 * most [MAX_SIDE] pixels on its longer side when attached, so a receipt stays readable and backups
 * stay small; a PDF is kept as it is, up to [Attachments.MAX_FILE_BYTES].
 */
internal object AttachmentFiles {
    private const val MAX_SIDE = 2048
    private const val QUALITY = 85

    fun dir(context: Context) = File(context.filesDir, "attachments")

    fun file(context: Context, attachment: Attachment): File? =
        attachment.kind.extension?.let { File(dir(context), "${attachment.target}.$it") }

    private fun authority(context: Context) = context.packageName + ".files"

    /** Where the camera writes a new photo before it is attached. */
    fun cameraUri(context: Context): Uri {
        val shot = File(File(context.cacheDir, "camera").apply { mkdirs() }, "shot.jpg")
        return FileProvider.getUriForFile(context, authority(context), shot)
    }

    /** A picked file's own name, such as "invoice-1234.pdf", without its extension. */
    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.') else null
        }
    }.getOrNull()?.take(Attachment.NAME_LIMIT)

    /** Stores a photo from [uri], upright and made smaller; its hash, or null when it is not an image. */
    suspend fun importImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(context, uri) ?: return@runCatching null
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                out.toByteArray()
            }
            store(context, bytes, AttachmentKind.IMAGE)
        }.getOrNull()
    }

    /** Stores a PDF from [uri]; its hash, or null when it is not a PDF or is too large. */
    suspend fun importPdf(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    buffer.write(chunk, 0, read)
                    if (buffer.size() > Attachments.MAX_FILE_BYTES) return@runCatching null
                }
                buffer.toByteArray()
            } ?: return@runCatching null
            if (bytes.size < 5 || String(bytes, 0, 5, Charsets.US_ASCII) != "%PDF-") return@runCatching null
            store(context, bytes, AttachmentKind.PDF)
        }.getOrNull()
    }

    private fun store(context: Context, bytes: ByteArray, kind: AttachmentKind): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val target = File(dir(context).apply { mkdirs() }, "$hash.${kind.extension}")
        if (!target.isFile) target.writeBytes(bytes)
        return hash
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        // From Android 9 the decoder turns a photo upright by its EXIF orientation and scales as it reads.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(w, h))
                decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height))
        return if (scale >= 1f) bitmap
        else Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    /** A stored photo, for the viewer; null when the file is missing. */
    suspend fun loadImage(context: Context, attachment: Attachment): Bitmap? = withContext(Dispatchers.IO) {
        file(context, attachment)?.takeIf { it.isFile }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
    }

    /**
     * Opens a link in the browser, or a PDF in the app that reads them. False when nothing on the
     * phone can, or the file is gone.
     */
    fun open(context: Context, attachment: Attachment): Boolean {
        val intent = when (attachment.kind) {
            AttachmentKind.LINK -> Intent(Intent.ACTION_VIEW, Uri.parse(attachment.target))
            AttachmentKind.PDF, AttachmentKind.IMAGE -> {
                val file = file(context, attachment)?.takeIf { it.isFile } ?: return false
                val type = if (attachment.kind == AttachmentKind.PDF) "application/pdf" else "image/jpeg"
                Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(context, authority(context), file), type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** Deletes stored files no attachment uses. */
    suspend fun prune(context: Context, used: Set<String>) = withContext(Dispatchers.IO) {
        dir(context).listFiles { file -> file.isFile }?.forEach { file ->
            if (file.name.substringBefore('.') !in used) file.delete()
        }
    }
}
