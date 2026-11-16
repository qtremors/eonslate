package dev.qtremors.eonslate.core.tracker.domain.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Why a file could not be opened as a backup. Each has its own message for the person. */
enum class BackupProblem {
    /** Not an Eonslate backup at all. */
    NOT_A_BACKUP,

    /** Made by a newer Eonslate that this version cannot read. */
    NEWER_VERSION,

    /** Encrypted, and no password was given. */
    PASSWORD_NEEDED,

    /** Encrypted, and the password does not open it. */
    WRONG_PASSWORD,

    /** Cut short or changed after it was written. */
    DAMAGED,

    /** Readable, but its rows do not fit together, so restoring it would break the data. */
    INCONSISTENT,
}

class BackupException(val problem: BackupProblem, cause: Throwable? = null) :
    Exception(problem.name, cause)

/** What can be learned about a file before any password is asked for. */
data class ArchiveHeader(val formatVersion: Int, val encrypted: Boolean)

/**
 * The backup file format. One file, `.eonslate`:
 *
 * ```
 * "EONSLATE"            8 bytes, so any other file is refused at once
 * format version        1 byte
 * flags                 1 byte, bit 0 set when encrypted
 * salt                  16 bytes  ┐
 * iterations            4 bytes   ├ only when encrypted
 * nonce                 12 bytes  ┘
 * body                  the snapshot as gzipped JSON, or that sealed with AES-256-GCM
 * ```
 *
 * The key comes from the password through PBKDF2 with HMAC-SHA256 and the stored salt and
 * iteration count, so the count can rise in later versions and old files still open. GCM
 * authenticates the body and, as associated data, the whole header: a wrong password and a file
 * changed in any byte are both caught before anything is restored.
 *
 * Without a password the body is only gzipped. That is the person's choice, and the page says
 * plainly that anyone with such a file can read it.
 */
object BackupArchive {
    const val FORMAT_VERSION = 1
    const val EXTENSION = "eonslate"
    const val MIME_TYPE = "application/octet-stream"

    /**
     * About 3 s on a moto g(40); Android's PBKDF2 is not native, so 600k took 9 s there. Stored
     * in each file, so it can rise later without breaking older backups.
     */
    const val DEFAULT_ITERATIONS = 210_000

    private val MAGIC = "EONSLATE".toByteArray(Charsets.US_ASCII)
    private const val FLAG_ENCRYPTED = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val PLAIN_HEADER = 10
    private const val ENCRYPTED_HEADER = PLAIN_HEADER + SALT_BYTES + 4 + NONCE_BYTES

    /** Far above any real history; a file that inflates past this is refused, not read. */
    private const val MAX_JSON_BYTES = 512L * 1024 * 1024

    /** Iterations below this are refused on reading, so a crafted file cannot weaken the key. */
    private const val MIN_ITERATIONS = 10_000
    private const val MAX_ITERATIONS = 10_000_000

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Writes [snapshot] as a backup file, encrypted when [password] is not empty. The caller
     * should clear [password] afterwards; it is never kept.
     */
    fun encode(
        snapshot: BackupSnapshot,
        password: CharArray?,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        val body = gzip(json.encodeToString(BackupSnapshot.serializer(), snapshot).toByteArray(Charsets.UTF_8))
        if (password == null || password.isEmpty()) {
            return header(encrypted = false) + body
        }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val head = header(encrypted = true) + ByteBuffer.allocate(SALT_BYTES + 4 + NONCE_BYTES)
            .put(salt).putInt(iterations).put(nonce).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(head)
        return head + cipher.doFinal(body)
    }

    /** Reads only the header: whether [bytes] is a backup, and whether it needs a password. */
    fun inspect(bytes: ByteArray): ArchiveHeader {
        if (bytes.size < PLAIN_HEADER || !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException(BackupProblem.NOT_A_BACKUP)
        }
        val version = bytes[MAGIC.size].toInt() and 0xFF
        if (version > FORMAT_VERSION) throw BackupException(BackupProblem.NEWER_VERSION)
        if (version < 1) throw BackupException(BackupProblem.NOT_A_BACKUP)
        val encrypted = (bytes[MAGIC.size + 1].toInt() and FLAG_ENCRYPTED) != 0
        return ArchiveHeader(version, encrypted)
    }

    /**
     * Opens a backup file. Throws [BackupException] with the reason when it is not a backup,
     * needs a password, the password is wrong, or the file is damaged.
     */
    fun decode(bytes: ByteArray, password: CharArray?): BackupSnapshot {
        val header = inspect(bytes)
        val body = if (header.encrypted) {
            if (password == null || password.isEmpty()) throw BackupException(BackupProblem.PASSWORD_NEEDED)
            if (bytes.size < ENCRYPTED_HEADER + TAG_BITS / 8) throw BackupException(BackupProblem.DAMAGED)
            val fields = ByteBuffer.wrap(bytes, PLAIN_HEADER, SALT_BYTES + 4 + NONCE_BYTES)
            val salt = ByteArray(SALT_BYTES).also { fields.get(it) }
            val iterations = fields.int
            val nonce = ByteArray(NONCE_BYTES).also { fields.get(it) }
            if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupException(BackupProblem.DAMAGED)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, nonce))
                cipher.updateAAD(bytes, 0, ENCRYPTED_HEADER)
                cipher.doFinal(bytes, ENCRYPTED_HEADER, bytes.size - ENCRYPTED_HEADER)
            } catch (e: AEADBadTagException) {
                // GCM cannot tell a wrong key from a changed file; the password is far likelier.
                throw BackupException(BackupProblem.WRONG_PASSWORD, e)
            } catch (e: GeneralSecurityException) {
                throw BackupException(BackupProblem.DAMAGED, e)
            }
        } else {
            bytes.copyOfRange(PLAIN_HEADER, bytes.size)
        }
        return try {
            json.decodeFromString(BackupSnapshot.serializer(), gunzip(body).toString(Charsets.UTF_8))
        } catch (e: IOException) {
            throw BackupException(BackupProblem.DAMAGED, e)
        } catch (e: SerializationException) {
            throw BackupException(BackupProblem.DAMAGED, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupProblem.DAMAGED, e)
        }
    }

    private fun header(encrypted: Boolean): ByteArray =
        MAGIC + byteArrayOf(FORMAT_VERSION.toByte(), (if (encrypted) FLAG_ENCRYPTED else 0).toByte())

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(secret, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    private fun gunzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_JSON_BYTES) throw IOException("Backup too large")
                out.write(buffer, 0, read)
            }
        }
        return out.toByteArray()
    }
}
