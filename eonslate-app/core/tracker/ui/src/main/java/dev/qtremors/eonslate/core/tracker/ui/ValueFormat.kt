package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.runtime.Composable
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.value.DurationSeconds
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.domain.value.ScaledDecimal
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.text.FieldPosition
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * Words the formatter needs that belong in string resources. Screens build one from resources;
 * tests pass plain English.
 */
data class ValueWords(
    val yes: String,
    val no: String,
    /** Formats hours and minutes, such as "6 h 30 m". */
    val hoursMinutes: (hours: Long, minutes: Int) -> String,
    /** Formats whole minutes under an hour, such as "32 m". */
    val minutes: (minutes: Int) -> String,
    /** Formats minutes and seconds for short times, such as "4 m 05 s". */
    val minutesSeconds: (minutes: Int, seconds: Int) -> String,
    /** Formats a rating out of its scale, such as "4/5". */
    val rating: (value: Int, max: Int) -> String,
    /** Shown for an option id that is no longer one of the field's choices. */
    val removedOption: String,
)

@Composable
fun displayDate(epochDay: Long): String =
    ValueFormat.date(epochDay, Locale.getDefault(), LocalValueDisplay.current.datePattern)

/**
 * Turns exact stored values into what a person reads, in their locale. Every value keeps its
 * exact digits: a number with two decimals always shows two, and money uses its currency's own.
 */
object ValueFormat {
    fun format(
        field: FieldDefinition, value: FieldValue, words: ValueWords,
        locale: Locale = Locale.getDefault(), datePattern: String? = null, dateLocale: Locale = locale,
    ): String =
        when (value) {
            is FieldValue.Number -> {
                val config = field.config as? FieldConfig.Number
                val decimals = config?.decimals ?: 0
                val text = decimal(ScaledDecimal.toBigDecimal(value.scaled, decimals), decimals, locale)
                config?.unit?.takeIf { it.isNotBlank() }?.let { "$text $it" } ?: text
            }
            is FieldValue.Money -> money(value.minor, (field.config as? FieldConfig.Money)?.currencyCode ?: "USD", locale)
            is FieldValue.Duration -> duration(value.seconds, words)
            is FieldValue.Rating -> words.rating(value.value, (field.config as? FieldConfig.Rating)?.max ?: value.value)
            is FieldValue.YesNo -> if (value.value) words.yes else words.no
            is FieldValue.SingleChoice -> optionLabel(field, value.optionId) ?: words.removedOption
            is FieldValue.MultiChoice -> {
                val options = (field.config as? FieldConfig.MultiChoice)?.options.orEmpty()
                // In the field's own order, not the order they were picked.
                options.filter { it.id in value.optionIds }.joinToString(", ") { it.label }
                    .ifEmpty { if (value.optionIds.isEmpty()) "–" else words.removedOption }
            }
            is FieldValue.Text -> value.text
            is FieldValue.Date -> date(value.epochDay, dateLocale, datePattern)
        }

    /**
     * A computed amount in [field]'s units, such as an average or a total: the unit for numbers,
     * the currency for money, hours and minutes for durations (given in seconds). [decimals] is
     * how many fraction digits to show; averages pass one more than the field has.
     */
    fun amount(field: FieldDefinition, value: BigDecimal, words: ValueWords, locale: Locale = Locale.getDefault(), decimals: Int = value.scale().coerceAtLeast(0)): String =
        when (val config = field.config) {
            is FieldConfig.Number -> {
                val text = decimal(value.setScale(decimals, java.math.RoundingMode.HALF_UP), decimals, locale)
                config.unit?.takeIf { it.isNotBlank() }?.let { "$text $it" } ?: text
            }
            is FieldConfig.Money -> {
                val format = NumberFormat.getCurrencyInstance(locale)
                runCatching { format.currency = Currency.getInstance(config.currencyCode) }
                format.minimumFractionDigits = decimals
                format.maximumFractionDigits = decimals
                localizedNumber(value.setScale(decimals, java.math.RoundingMode.HALF_UP), format, locale)
            }
            is FieldConfig.Duration -> duration(value.setScale(0, java.math.RoundingMode.HALF_UP).toLong(), words)
            else -> decimal(value, decimals, locale)
        }

    fun decimal(value: BigDecimal, decimals: Int, locale: Locale): String =
        localizedNumber(value, NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }, locale)

    fun money(minor: Long, currencyCode: String, locale: Locale): String =
        money(BigInteger.valueOf(minor), currencyCode, locale)

    fun money(minor: BigInteger, currencyCode: String, locale: Locale): String {
        val digits = MoneyAmount.fractionDigits(currencyCode)
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        return localizedNumber(BigDecimal(minor, digits), format, locale)
    }

    private fun localizedNumber(value: BigDecimal, format: NumberFormat, locale: Locale): String {
        if (locale.country != "IN") return format.format(value)
        // java.text implementations differ in support for the Indian secondary grouping size.
        format.isGroupingUsed = false
        val integer = FieldPosition(NumberFormat.INTEGER_FIELD)
        val text = format.format(value, StringBuffer(), integer)
        val digits = text.substring(integer.beginIndex, integer.endIndex)
        if (digits.length <= 3) return text.toString()
        val separator = DecimalFormatSymbols.getInstance(locale).groupingSeparator.toString()
        val leading = digits.dropLast(3).reversed().chunked(2).joinToString(separator).reversed()
        return text.replace(integer.beginIndex, integer.endIndex, leading + separator + digits.takeLast(3)).toString()
    }

    fun duration(seconds: Long, words: ValueWords): String {
        val parts = DurationSeconds.split(seconds.coerceAtLeast(0))
        return when {
            parts.hours == 0L && parts.seconds != 0 -> words.minutesSeconds(parts.minutes, parts.seconds)
            parts.hours == 0L -> words.minutes(parts.minutes)
            else -> words.hoursMinutes(parts.hours, parts.minutes)
        }
    }

    fun date(epochDay: Long, locale: Locale, pattern: String? = null): String {
        val formatter = if (pattern == null) DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        else DateTimeFormatter.ofPattern(pattern, locale)
        return EpochDays.toDate(epochDay).format(formatter)
    }

    private fun optionLabel(field: FieldDefinition, optionId: String): String? = when (val c = field.config) {
        is FieldConfig.SingleChoice -> c.options.firstOrNull { it.id == optionId }?.label
        is FieldConfig.MultiChoice -> c.options.firstOrNull { it.id == optionId }?.label
        else -> null
    }
}
