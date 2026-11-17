package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
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
)

/** Turns one completed bank alert into a reviewable draft. */
object PaymentMessages {
    const val MAX_LENGTH = 2_000

    private val blocked = pattern("""\b(otp|verification code|one[- ]time password|failed|declined|rejected|cancelled|pending|request|not debited|not credited|do not share)\b""")
    private val balance = pattern("""\b(?:(?:avl|avail(?:able)?|current|remaining|closing)\.?\s+bal(?:ance)?|balance)\b""")
    private val debit = pattern("""\b(debited|spent|withdrawn|paid|charged|deducted|sent to|transferred to|used at|purchase at|debit of)\b""")
    private val credit = pattern("""\b(credited|received|deposited|refunded|credit of|refund of|reversal of)\b""")
    private val weakDebit = pattern("""(?:\bUPI[/ ]DR\b|\bDR\b|\bpayment of\b)""")
    private val weakCredit = pattern("""(?:\bUPI[/ ]CR\b|\bCR\b|\bcashback\b)""")
    private val prefix = pattern("""(?<![A-Za-z])((?:Rs\.?|US\$|\$|₹|€|£|[A-Za-z]{3}))\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)""")
    private val suffix = pattern("""(?<![0-9,.])([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)\s*(INR|Rs\.?|USD|EUR|GBP|JPY|AED|KWD)\b(?!\s*[0-9])""")
    private val number = Regex("""(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})+,[0-9]{3})(?:\.[0-9]+)?""")
    private val ending = pattern("""\b(?:a/c|acct|account|card)\s*(?:ending|no\.?|#|\(ending\))?\s*[*xX]*(\d{3,4})\b""")
    private val reference = pattern("""\b(?:upi\s*(?:ref(?:erence)?|txn)|rrn|ref(?:erence)?)\s*(?:no\.?|id)?\s*[:#-]?\s*([a-z0-9]{6,30})\b""")
    private val merchant = pattern("""\b(?:used at|purchase at|spent at|paid to|received from|sent to|transferred to|towards|at|info:)\s+([\p{L}][\p{L}\d .&@'_/-]{1,58}?)(?=\s+(?:on|dated|via|ref|using|with|upi)\b|[;,\n]|[.]\s|[.]?$)""")
    private val date = pattern("""\b(?:on|dated)\s+(\d{4}-\d{2}-\d{2}|\d{2}[-/]\d{2}[-/]\d{2,4})(?!\d)""")
    private val formats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd-MM-uu").withResolverStyle(ResolverStyle.STRICT),
    )
    private val categories = paymentCategoryKeywords.mapValues { (_, words) ->
        pattern(words.sortedByDescending { it.length }.joinToString("|", """\b(?:""", """)\b""", transform = Regex::escape))
    }
    private val whitespace = Regex("""\s+""")

    fun parse(text: String, sender: String? = null, receivedDay: Long? = null): PaymentMessage? {
        if (text.length > MAX_LENGTH || blocked.containsMatchIn(text)) return null
        val alert = balance.find(text)?.let { text.substring(0, it.range.first) } ?: text
        val kind = kind(alert) ?: return null
        val money = (prefix.findAll(alert).mapNotNull { money(it.groupValues[1], it.groupValues[2]) } +
            suffix.findAll(alert).mapNotNull { money(it.groupValues[2], it.groupValues[1]) }).distinct().singleOrNull()
            ?: return null

        val mask = ending.find(alert)?.groupValues?.get(1)
        val ref = reference.find(alert)?.groupValues?.get(1)?.uppercase(Locale.ROOT)?.takeIf { it.any(Char::isDigit) }
        val label = merchant.find(alert)?.groupValues?.get(1)?.trim()?.trimEnd('.')
        val knownMerchant = categories.asSequence().mapNotNull { (category, pattern) ->
            pattern.find(label ?: alert)?.let { category to it.value }
        }.firstOrNull()
        val day = date.find(alert)?.groupValues?.get(1)?.replace('/', '-')?.let { value ->
            formats.firstNotNullOfOrNull { runCatching { LocalDate.parse(value, it).toEpochDay() }.getOrNull() }
        } ?: receivedDay
        val source = if (ref != null) listOf(money.first, money.second, kind.name, mask.orEmpty(), ref).joinToString("|")
        else whitespace.replace(text.trim().lowercase(Locale.ROOT), " ")
        return PaymentMessage(
            kind = kind,
            currency = money.first,
            minor = money.second,
            merchant = label ?: knownMerchant?.second?.replaceFirstChar { it.titlecase(Locale.ROOT) },
            category = if (kind == TransactionKind.INCOME) "Income" else knownMerchant?.first,
            accountEnding = mask,
            reference = ref,
            epochDay = day?.takeIf { it in Transaction.DATES },
            sender = sender?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
            key = fingerprint(source),
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

    private fun money(unit: String, digits: String): Pair<String, Long>? {
        val code = when (val canonical = unit.uppercase(Locale.ROOT)) {
            "RS", "RS.", "₹" -> "INR"
            "US$", "$" -> "USD"
            "€" -> "EUR"
            "£" -> "GBP"
            else -> canonical
        }
        if (!MoneyAmount.isKnownCurrency(code) || !number.matches(digits)) return null
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
