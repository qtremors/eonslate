package dev.qtremors.eonslate.core.tracker.domain.value

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Currency

/**
 * Converts between what a person types and the exact whole numbers Eonslate stores. Every
 * function here is pure and never rounds silently: input with more precision than the field
 * allows, or outside the range of a [Long], returns null.
 */
object ScaledDecimal {
    /**
     * Parses [text] such as "12.5", "-3" or "1,250.75" into a value scaled by 10^[decimals].
     * Grouping commas and spaces are ignored. Returns null for anything that is not a number or
     * has more fraction digits than [decimals].
     */
    fun parse(text: String, decimals: Int): Long? {
        require(decimals >= 0) { "decimals must not be negative" }
        val cleaned = text.trim().replace(",", "").replace(" ", "").replace(" ", "")
        if (cleaned.isEmpty() || !NUMBER.matches(cleaned)) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.stripTrailingZeros().scale() > decimals) return null
        return toScaledOrNull(value, decimals)
    }

    /** The exact value of [scaled] as a decimal with [decimals] fraction digits. */
    fun toBigDecimal(scaled: Long, decimals: Int): BigDecimal =
        BigDecimal.valueOf(scaled, decimals)

    /**
     * Formats [scaled] with exactly [decimals] fraction digits and no grouping, such as "12.50".
     * Locale-aware formatting belongs to the UI; this is the stable, parseable form.
     */
    fun format(scaled: Long, decimals: Int): String = toBigDecimal(scaled, decimals).toPlainString()

    /**
     * Re-expresses [scaled] from [fromDecimals] to [toDecimals], for when a field's precision is
     * changed. Gaining digits is exact; losing digits rounds half up. Null when out of range.
     */
    fun rescale(scaled: Long, fromDecimals: Int, toDecimals: Int): Long? =
        toScaledOrNull(BigDecimal.valueOf(scaled, fromDecimals).setScale(toDecimals, RoundingMode.HALF_UP), toDecimals)

    private fun toScaledOrNull(value: BigDecimal, decimals: Int): Long? = try {
        value.setScale(decimals, RoundingMode.UNNECESSARY).unscaledValue().longValueExact()
    } catch (_: ArithmeticException) {
        null
    }

    private val NUMBER = Regex("""^[+-]?(\d+(\.\d*)?|\.\d+)$""")
}

/** Money in minor units of an ISO 4217 currency, using each currency's own number of digits. */
object MoneyAmount {
    /** True when [code] is a currency this device knows, such as "INR", "USD" or "JPY". */
    fun isKnownCurrency(code: String): Boolean = currencyOrNull(code) != null

    /** Minor-unit digits for [code]: 2 for USD, 0 for JPY, 3 for KWD. Unknown codes use 2. */
    fun fractionDigits(code: String): Int =
        currencyOrNull(code)?.defaultFractionDigits?.takeIf { it >= 0 } ?: DEFAULT_DIGITS

    /** Parses "12.34" into 1234 for a two-digit currency. Null when invalid or too precise. */
    fun parse(text: String, currencyCode: String): Long? = ScaledDecimal.parse(text, fractionDigits(currencyCode))

    /** The plain form of [minor], such as "12.34". */
    fun format(minor: Long, currencyCode: String): String = ScaledDecimal.format(minor, fractionDigits(currencyCode))

    private fun currencyOrNull(code: String): Currency? =
        if (!CODE.matches(code)) null else try {
            Currency.getInstance(code)
        } catch (_: IllegalArgumentException) {
            null
        }

    private const val DEFAULT_DIGITS = 2
    private val CODE = Regex("^[A-Z]{3}$")
}

/** Durations in whole seconds, split into the parts an input shows. */
object DurationSeconds {
    data class Parts(val hours: Long, val minutes: Int, val seconds: Int)

    /** Combines parts into seconds. Null when a part is negative, out of range, or overflows. */
    fun of(hours: Long = 0, minutes: Long = 0, seconds: Long = 0): Long? {
        if (hours < 0 || minutes < 0 || seconds < 0) return null
        return try {
            Math.addExact(Math.addExact(Math.multiplyExact(hours, 3_600L), Math.multiplyExact(minutes, 60L)), seconds)
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Splits a non-negative [totalSeconds] into hours, minutes and seconds. */
    fun split(totalSeconds: Long): Parts {
        require(totalSeconds >= 0) { "duration must not be negative" }
        return Parts(
            hours = totalSeconds / 3_600L,
            minutes = ((totalSeconds % 3_600L) / 60L).toInt(),
            seconds = (totalSeconds % 60L).toInt(),
        )
    }

    /**
     * Parses "1:30" (minutes:seconds), "1:05:00" (hours:minutes:seconds) or a bare number of
     * minutes such as "45". Minutes and seconds after a colon must be below 60.
     */
    fun parse(text: String): Long? {
        val parts = text.trim().split(':')
        if (parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return null
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        return when (numbers.size) {
            1 -> of(minutes = numbers[0])
            2 -> if (numbers[1] < 60) of(minutes = numbers[0], seconds = numbers[1]) else null
            3 -> if (numbers[1] < 60 && numbers[2] < 60) of(numbers[0], numbers[1], numbers[2]) else null
            else -> null
        }
    }

    /** The stable form "H:MM:SS", such as "1:05:00". */
    fun format(totalSeconds: Long): String {
        val parts = split(totalSeconds)
        return "${parts.hours}:${parts.minutes.toString().padStart(2, '0')}:${parts.seconds.toString().padStart(2, '0')}"
    }
}

/** Calendar dates as days since 1970-01-01. */
object EpochDays {
    fun of(date: LocalDate): Long = date.toEpochDay()

    fun toDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** Parses an ISO date such as "2026-11-10". Null when it is not a real date. */
    fun parse(text: String): Long? = try {
        LocalDate.parse(text.trim()).toEpochDay()
    } catch (_: DateTimeException) {
        null
    }

    /** The ISO form, such as "2026-11-10". */
    fun format(epochDay: Long): String = toDate(epochDay).toString()

    /** True when [epochDay] is inside the range `java.time.LocalDate` can represent. */
    fun isValid(epochDay: Long): Boolean = epochDay in LocalDate.MIN.toEpochDay()..LocalDate.MAX.toEpochDay()
}
