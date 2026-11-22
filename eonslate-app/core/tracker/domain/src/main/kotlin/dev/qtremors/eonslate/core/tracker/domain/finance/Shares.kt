package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger
import kotlin.math.abs

/**
 * What one person or business owes you because of a transaction, in its account's minor units:
 * positive when they owe you more (you lent, split a bill, or paid them back), negative when you
 * owe them more (you borrowed, or they paid you back). [settles] marks a payment back.
 */
@Serializable
data class Share(
    @SerialName("party_id") val partyId: String,
    val minor: Long,
    val settles: Boolean = false,
)

/** How a payment with people reads, worked out from its shares. */
enum class ShareKind { LENT, SPLIT, PAID_BACK, BORROWED, GOT_BACK }

/** Your own part of a transaction: what you spent or earned once others' shares are taken out. */
val Transaction.own: Long get() = (minor - abs(shares.sumOf { it.minor })).coerceAtLeast(0)

/** What this transaction is with people, or null when it is all yours. */
val Transaction.shareKind: ShareKind?
    get() {
        if (shares.isEmpty()) return null
        val settles = shares.all { it.settles }
        return when (kind) {
            TransactionKind.EXPENSE -> when {
                settles -> ShareKind.PAID_BACK
                shares.size > 1 || own > 0 -> ShareKind.SPLIT
                else -> ShareKind.LENT
            }
            TransactionKind.INCOME -> if (settles) ShareKind.GOT_BACK else ShareKind.BORROWED
            TransactionKind.TRANSFER -> null
        }
    }

/**
 * Shares that make sense for this transaction: a transfer has none; an expense's are positive
 * (they owe you) and an income's negative (you owe them), one per party, adding up to no more
 * than the amount.
 */
fun Transaction.sharesValid(): Boolean {
    if (shares.isEmpty()) return true
    if (kind == TransactionKind.TRANSFER) return false
    if (shares.map { it.partyId }.toSet().size != shares.size || shares.any { it.partyId.isBlank() }) return false
    val positive = kind == TransactionKind.EXPENSE
    if (shares.any { if (positive) it.minor <= 0 else it.minor >= 0 }) return false
    return abs(shares.sumOf { it.minor }) <= minor
}

/**
 * Where you stand with each person or business, by currency: positive when they owe you,
 * negative when you owe them. Settled-up people are left out.
 */
fun Ledger.owed(): Map<String, Map<String, BigInteger>> {
    val currencies = accounts.associate { it.id to it.currency }
    val totals = HashMap<String, HashMap<String, BigInteger>>()
    for (t in transactions) {
        val currency = currencies[t.accountId] ?: continue
        for (share in t.shares) {
            val byCurrency = totals.getOrPut(share.partyId) { HashMap() }
            byCurrency[currency] = byCurrency.getOrDefault(currency, BigInteger.ZERO) + BigInteger.valueOf(share.minor)
        }
    }
    return totals.mapValues { (_, byCurrency) -> byCurrency.filterValues { it.signum() != 0 } }.filterValues { it.isNotEmpty() }
}

/** Splits [minor] equally among [people], with you as one more when [includeMe]; you keep any odd paise. */
fun splitEqually(minor: Long, people: List<String>, includeMe: Boolean): List<Share> {
    if (people.isEmpty() || minor <= 0) return emptyList()
    val each = minor / (people.size + if (includeMe) 1 else 0)
    return if (each <= 0) emptyList() else people.distinct().map { Share(it, each) }
}
