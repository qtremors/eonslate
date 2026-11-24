package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI

/** What an attachment is: a photo or image, a PDF, or a link to a page such as the product bought. */
@Serializable
enum class AttachmentKind(val storageKey: String) {
    @SerialName("image") IMAGE("image"),
    @SerialName("pdf") PDF("pdf"),
    @SerialName("link") LINK("link");

    /** The file's extension in storage; links have no file. */
    val extension: String? get() = when (this) {
        IMAGE -> "jpg"
        PDF -> "pdf"
        LINK -> null
    }

    companion object {
        fun fromKey(key: String?): AttachmentKind? = entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * Something kept with a transaction: a receipt's photo or PDF, or a link such as the product's
 * page. A file's [target] is its content hash, which names it in storage, so one file attached
 * twice is stored once; a link's [target] is its address.
 */
@Serializable
data class Attachment(
    val id: String,
    @SerialName("transaction_id") val transactionId: String,
    val kind: AttachmentKind,
    val target: String,
    /** What it is called, such as a file's name or "Product page"; blank shows the address or the kind. */
    val name: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
) {
    val isFile: Boolean get() = kind != AttachmentKind.LINK

    fun isValid(): Boolean = id.isNotBlank() && transactionId.isNotBlank() && name.length <= NAME_LIMIT &&
        if (isFile) HASH.matches(target) else Attachments.link(target) == target

    companion object {
        const val NAME_LIMIT = 120
        val HASH = Regex("[0-9a-f]{64}")
    }
}

object Attachments {
    /** The most attachments one transaction keeps. */
    const val PER_TRANSACTION = 10

    /** A PDF may be this large; photos are made smaller than this when attached. */
    const val MAX_FILE_BYTES = 15L * 1024 * 1024
    const val LINK_LIMIT = 2_048

    /**
     * A typed or pasted address as a link, or null when it is not one: "amazon.in/dp/B0C" becomes
     * "https://amazon.in/dp/B0C". Only web addresses with a host are links.
     */
    fun link(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > LINK_LIMIT || trimmed.any { it.isWhitespace() }) return null
        val full = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val uri = runCatching { URI(full) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (!host.contains('.') && host != "localhost") return null
        return full
    }

    /** "amazon.in" for a link, for a row that has no name of its own. */
    fun host(link: String): String = runCatching { URI(link).host?.removePrefix("www.") }.getOrNull() ?: link

    /** Every file the attachments use, by hash, with its kind. */
    fun files(attachments: List<Attachment>): Map<String, AttachmentKind> =
        attachments.filter { it.isFile }.associate { it.target to it.kind }
}
