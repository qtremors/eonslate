package dev.qtremors.eonslate.core.settings

import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale

/** Which day a week starts on in calendars and weekly insights. */
enum class WeekStart { LOCALE, MONDAY, SUNDAY, SATURDAY }

/**
 * How tracking works across the app.
 *
 * @property currency the ISO 4217 code new money fields start with; null follows the device.
 */
data class Tracking(
    val weekStart: WeekStart = WeekStart.LOCALE,
    val currency: String? = null,
) {
    /** The first day of the week for [locale], unless a day was chosen. */
    fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek = when (weekStart) {
        WeekStart.LOCALE -> WeekFields.of(locale).firstDayOfWeek
        WeekStart.MONDAY -> DayOfWeek.MONDAY
        WeekStart.SUNDAY -> DayOfWeek.SUNDAY
        WeekStart.SATURDAY -> DayOfWeek.SATURDAY
    }

    /** The currency new money fields use: the chosen one, else the device's, else US dollars. */
    fun currencyCode(locale: Locale = Locale.getDefault()): String =
        currency?.takeIf(::isCurrency) ?: deviceCurrency(locale)

    companion object {
        /** The currency of [locale]'s country, or US dollars for a locale without one. */
        fun deviceCurrency(locale: Locale = Locale.getDefault()): String =
            runCatching { Currency.getInstance(locale)?.currencyCode }.getOrNull() ?: "USD"

        /** True for a three-letter code this device knows, such as "EUR". */
        fun isCurrency(code: String): Boolean =
            code.matches(Regex("[A-Z]{3}")) && runCatching { Currency.getInstance(code) }.isSuccess
    }
}
