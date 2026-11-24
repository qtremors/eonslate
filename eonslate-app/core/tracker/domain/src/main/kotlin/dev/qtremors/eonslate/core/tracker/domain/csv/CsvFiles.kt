package dev.qtremors.eonslate.core.tracker.domain.csv

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

object CsvFiles {
    const val MIME = "text/csv"
    private const val MAX_BYTES = 16 * 1024 * 1024
    /** Android labels a .csv as "text/comma-separated-values", and other apps use the rest. */
    val importTypes get() = arrayOf(
        MIME, "text/comma-separated-values", "text/x-csv", "application/csv", "text/plain", "application/vnd.ms-excel", "application/octet-stream",
    )

    fun read(input: InputStream): String {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            if (bytes.size() + size > MAX_BYTES) throw CsvException(CsvProblem.TOO_LARGE)
            bytes.write(buffer, 0, size)
        }
        return try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
        } catch (_: CharacterCodingException) {
            throw CsvException(CsvProblem.FORMAT)
        }
    }
}
