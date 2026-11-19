package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Currency
import java.util.Locale

data class PaymentMessage(
    val kind: TransactionKind,
    val currency: String,
    val minor: Long,
    val merchant: String? = null,
    val category: String? = null,
    val accountEnding: String? = null,
    val reference: String? = null,
    val epochDay: Long? = null,
    val sender: String? = null,
    val key: String? = null,
    val issuer: String? = null,
    val isCard: Boolean = false,
    val confidence: Float = 0.85f,
)

/**
 * The transaction for a found payment: on the account whose name ends with the card or account
 * digits, else the first account in its currency, or null when none uses it.
 */
fun PaymentMessage.asTransaction(id: String, accounts: List<Account>, today: Long): Transaction? {
    val matching = accounts.filter { it.currency == currency }
    val account = accountEnding?.let { end -> matching.firstOrNull { it.name.trim().endsWith(end) } } ?: matching.firstOrNull() ?: return null
    val note = listOfNotNull(merchant, reference?.let { "Ref $it" }).joinToString(" · ").take(Transaction.NOTE_LIMIT)
    return Transaction(id, account.id, kind, minor, epochDay ?: today, note, null, category.orEmpty().take(Transaction.CATEGORY_LIMIT), key)
}

/** Turns one completed bank alert into a reviewable draft. */
object PaymentMessages {
    const val MAX_LENGTH = 2_000

    private val blocked = pattern("""\b(otp|(?:verification|security|login|secret|auth)\s+code|one[- ]?time password|password reset|login attempt|failed|declined|rejected|cancelled|pending|request|not debited|not credited|(?:do not|never) share)\b""")
    private val balance = pattern("""\b(?:(?:avl|avail(?:able)?|current|remaining|closing)\.?\s+(?:bal(?:ance)?|(?:credit\s+)?limit)|(?:clear|eff)\s+bal(?:ance)?|bal(?:ance)?(?=\s*(?:[:=]|is\b|INR\b|Rs\b|₹)))\b""")
    private val debit = pattern("""\b(debited|spent|withdrawn|paid|charged|deducted|sent to|transfer(?:red)? to|used (?:at|on)|purchase (?:at|of)|debit of)\b""")
    private val credit = pattern("""\b(credited|received|deposited|refunded|credit of|refund of|reversal of|reversed to|added to)\b""")
    private val weakDebit = pattern("""\b(?:DR|payment of|txn of)\b""")
    private val weakCredit = pattern("""\b(?:CR|cash\s?back|salary|inflow)\b""")
    private val prefix = pattern("""(?<![A-Za-z])((?:Rs\.?|US\$|\$|₹|€|£|[A-Za-z]{3}))\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)""")
    private val suffix = pattern("""(?<![0-9,.])([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)\s*(INR|Rs\.?|USD|EUR|GBP|JPY|AED|KWD)\b(?!\s*[0-9])""")
    private val number = Regex("""(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})+,[0-9]{3})(?:\.[0-9]+)?""")
    private val unmarkedAmount = pattern("""\b(?:debited|credited|spent|paid)\s+(?:(?:by|for|with)\s+)?([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?![\d,]|\.\d)""")
    private val upi = pattern("""\bUPI\s*[/\*: ]\s*(?:(DR|CR)[/\*: ]+)?([a-z0-9]{4,30})(?:[/\*]+([^/\*;,\n]+))?""")
    private val ending = pattern("""\b(a/c|ac(?=\s*no\b)|acct|account|card)\s*(?:(?:ending(?:\s+in)?|no\.?|num(?:ber)?|\(ending\))\s*)?[:#-]?\s*(?:[*x•×-]+\s*)*(\d{3,4})\b""")
    private val reference = pattern("""\b(?:upi\s*(?:ref(?:erence)?|txn|transaction)?|rrn|ref(?:erence)?)\s*(?:(?:no\.?|id|num(?:ber)?)\s*)?[:#-]?\s*([a-z0-9]{4,30})\b""")
    private val merchant = pattern("""\b(?:received\s+from|towards|at|to|info:)\s+([\p{L}\d][\p{L}\d .&@'_/-]{1,58}?)(?=\s+(?:on|dated|via|through|thru|ref|using|with|upi|recurring)\b|[;,\n]|[.]\s|[.]?$)""")
    private val merchantNoise = pattern("""^(?:your\b|my\b|a/c\b|account\b|acct\b|(?:credit |debit )?card\b|upi[/\*: ]|inr\b|rs\b|\d+[ /.-])""")
    private val date = pattern("""\b(?:on|dated)\s+(\d{4}-\d{2}-\d{2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}[- ]?[a-z]{3}[- ]?\d{2,4})\b""")
    private val formats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
    ) + listOf("d-M-uuuu", "d-M-uu", "d-MMM-uuuu", "d-MMM-uu", "dMMMuuuu", "dMMMuu").map {
        DateTimeFormatter.ofPattern(it, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
    }
    private val keywords = paymentCategoryKeywords.entries
        .filter { it.key != "Income" }
        .flatMap { (category, words) -> words.map { it to category } }.toMap()
    private val brands = pattern(keywords.keys.sortedByDescending(String::length)
        .joinToString("|", """\b(?:""", """)\b""", transform = Regex::escape))
    private val whitespace = Regex("""\s+""")
    // Android accepts arbitrary three-letter strings in Currency.getInstance().
    private val currencyCodes by lazy { Currency.getAvailableCurrencies().mapTo(HashSet()) { it.currencyCode } }

    fun parse(text: String, sender: String? = null, receivedDay: Long? = null): PaymentMessage? {
        if (text.length > MAX_LENGTH || blocked.containsMatchIn(text)) return null
        val alert = balance.find(text)?.let { text.substring(0, it.range.first) } ?: text
        val kind = kind(alert) ?: return null
        val issuer = paymentSenderName(sender)
        val amounts = (prefix.findAll(alert).mapNotNull { money(it.groupValues[1], it.groupValues[2]) } +
            suffix.findAll(alert).mapNotNull { money(it.groupValues[2], it.groupValues[1]) }).distinct().toList()
        val money = if (amounts.isEmpty() && (issuer != null || alert.contains("UPI", ignoreCase = true))) {
            unmarkedAmount.findAll(alert).mapNotNull { money("INR", it.groupValues[1]) }.distinct().singleOrNull()
        } else amounts.singleOrNull()
        if (money == null) return null

        val account = ending.find(alert)
        val mask = account?.groupValues?.get(2)
        val upiDetails = upi.findAll(alert).firstOrNull { it.groupValues[2].any(Char::isDigit) }
        val ref = (reference.findAll(alert).map { it.groupValues[1] }.firstOrNull { it.any(Char::isDigit) }
            ?: upiDetails?.groupValues?.get(2))?.uppercase(Locale.ROOT)
        val label = upiDetails?.groupValues?.get(3)?.trim()?.trimEnd('.')?.takeIf { it.length in 2..60 }
            ?: merchant.findAll(alert).map { it.groupValues[1].trim().trimEnd('.') }
                .firstOrNull { it.any(Char::isLetter) && !merchantNoise.containsMatchIn(it) }
        val brand = sequenceOf(label, alert, issuer).filterNotNull().mapNotNull { brands.find(it)?.value }.firstOrNull()
        val payee = label ?: brand?.split(' ')?.joinToString(" ") { it.lowercase(Locale.ROOT).replaceFirstChar(Char::titlecase) }
            ?: issuer
        val day = date.find(alert)?.groupValues?.get(1)?.replace('/', '-')?.replace(' ', '-')?.let { raw ->
            val value = raw.lowercase(Locale.ROOT).replaceFirstMonthLetter()
            formats.firstNotNullOfOrNull { runCatching { LocalDate.parse(value, it).toEpochDay() }.getOrNull() }
        } ?: receivedDay
        val source = if (ref != null) listOf(money.first, money.second, kind.name, mask.orEmpty(), ref).joinToString("|")
        else whitespace.replace(text.trim().lowercase(Locale.ROOT), " ")
        return PaymentMessage(
            kind = kind,
            currency = money.first,
            minor = money.second,
            merchant = payee,
            category = if (kind == TransactionKind.INCOME) "Income" else keywords[brand?.lowercase(Locale.ROOT)] ?: "Other",
            accountEnding = mask,
            reference = ref,
            epochDay = day?.takeIf { it in Transaction.DATES },
            sender = sender?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
            key = fingerprint(source),
            issuer = issuer,
            isCard = account?.groupValues?.get(1).equals("card", ignoreCase = true),
            confidence = if (payee != null && mask != null) 1f else 0.85f,
        )
    }

    private fun kind(alert: String): TransactionKind? {
        val outgoing = debit.containsMatchIn(alert)
        val incoming = credit.containsMatchIn(alert)
        if (outgoing || incoming) return direction(outgoing, incoming)
        return direction(weakDebit.containsMatchIn(alert), weakCredit.containsMatchIn(alert))
    }

    private fun direction(outgoing: Boolean, incoming: Boolean): TransactionKind? = when {
        outgoing == incoming -> null
        outgoing -> TransactionKind.EXPENSE
        else -> TransactionKind.INCOME
    }

    private fun String.replaceFirstMonthLetter(): String {
        val index = indexOfFirst(Char::isLetter)
        return if (index < 0) this else replaceRange(index, index + 1, this[index].titlecase(Locale.ENGLISH))
    }

    private fun money(unit: String, digits: String): Pair<String, Long>? {
        val code = when (val canonical = unit.uppercase(Locale.ROOT)) {
            "RS", "RS.", "₹" -> "INR"
            "US$", "$" -> "USD"
            "€" -> "EUR"
            "£" -> "GBP"
            else -> canonical
        }
        if (code !in currencyCodes || !number.matches(digits)) return null
        val minor = MoneyAmount.parse(digits, code)?.takeIf { it > 0 } ?: return null
        return code to minor
    }

    private fun pattern(value: String) = Regex(value, RegexOption.IGNORE_CASE)

    private fun fingerprint(source: String): String {
        val hex = "0123456789abcdef"
        return buildString(64) {
            for (byte in MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))) {
                val value = byte.toInt() and 255
                append(hex[value ushr 4]).append(hex[value and 15])
            }
        }
    }
}
