package dev.qtremors.eonslate.feature.backup

import android.content.Context
import android.net.Uri
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Reads and writes the files a person picked in the system's file picker. Eonslate asks for no
 * storage permission: the picker grants it this one file, once.
 */
interface Documents {
    suspend fun read(uri: String): ByteArray

    suspend fun write(uri: String, bytes: ByteArray)
}

class ContentDocuments @Inject constructor(@ApplicationContext private val context: Context) : Documents {
    override suspend fun read(uri: String): ByteArray = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(Uri.parse(uri)) ?: throw IOException("No stream for $uri")
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BYTES) throw IOException("File too large to be a backup")
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }

    override suspend fun write(uri: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        // "wt" truncates, so a shorter backup never leaves the tail of an older one behind.
        val output = context.contentResolver.openOutputStream(Uri.parse(uri), "wt") ?: throw IOException("No stream for $uri")
        output.use { it.write(bytes) }
    }

    private companion object {
        const val MAX_BYTES = 256L * 1024 * 1024
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentsModule {
    @Binds
    abstract fun bindDocuments(impl: ContentDocuments): Documents
}
