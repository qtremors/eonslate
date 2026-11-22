package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/** Who a payment is with: a person, or a business such as a shop or a service. */
@Serializable
enum class PartyKind {
    @SerialName("person") PERSON,
    @SerialName("business") BUSINESS;

    companion object {
        private val businessWords = Regex(
            "\\b(pvt|ltd|limited|llp|inc|corp|store|stores|mart|shop|pay|bank|cafe|restaurant|hotel|foods?|" +
                "technologies|services|solutions|pharma|pharmacy|medical|hospital|clinic|fuels?|petrol|motors|" +
                "enterprises?|traders|amazon|flipkart|swiggy|zomato|uber|ola|rapido|netflix|spotify|google|apple|" +
                "jio|airtel|vodafone|bsnl|irctc|paytm|phonepe|myntra|zepto|blinkit|bigbasket|dmart)\\b|\\.com\\b",
            RegexOption.IGNORE_CASE,
        )

        /** A good first guess from a name alone: company words make it a business. */
        fun guess(name: String): PartyKind = if (businessWords.containsMatchIn(name)) BUSINESS else PERSON
    }
}

/** A person or business that payments are made to or received from, like a contact in a payments app. */
@Serializable
data class Party(
    val id: String,
    val name: String,
    val kind: PartyKind = PartyKind.PERSON,
    @SerialName("created_at") val createdAt: Long = 0,
) {
    /** The name as compared: two parties never share it, whatever the case or spacing. */
    val key: String get() = keyOf(name)

    fun isValid(): Boolean = id.isNotBlank() && name.isNotBlank() && name.trim().length <= NAME_LIMIT

    companion object {
        const val NAME_LIMIT = 60

        fun keyOf(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    }
}

/**
 * Finds a party by name, or makes one: for imports and payment messages that name who was paid.
 * The ones it made are in [created], to be saved before the transactions that use them.
 */
class PartyBook(existing: List<Party>, private val ids: IdGenerator, private val now: Long = 0) {
    private val byKey = existing.associateBy { it.key }.toMutableMap()
    val created = mutableListOf<Party>()

    fun idFor(name: String?, kind: PartyKind? = null): String? {
        val clean = name?.trim()?.replace(Regex("\\s+"), " ")?.take(Party.NAME_LIMIT)?.takeIf { it.isNotEmpty() } ?: return null
        byKey[Party.keyOf(clean)]?.let { return it.id }
        val party = Party(ids.newId(), clean, kind ?: PartyKind.guess(clean), now)
        byKey[party.key] = party
        created += party
        return party.id
    }
}
