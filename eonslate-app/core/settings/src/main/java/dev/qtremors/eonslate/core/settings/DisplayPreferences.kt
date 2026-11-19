package dev.qtremors.eonslate.core.settings

import java.util.Locale

enum class NumberLocale(val tag: String?) {
    DEVICE(null), INDIA("en-IN"), UNITED_STATES("en-US"), GERMANY("de-DE");

    fun locale(): Locale = tag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
}

enum class DateDisplay(val pattern: String?) {
    DEVICE(null), DAY_FIRST("dd/MM/uuuu"), MONTH_FIRST("MM/dd/uuuu"), YEAR_FIRST("uuuu-MM-dd")
}

data class DisplayPreferences(
    val name: String = "",
    val numbers: NumberLocale = NumberLocale.DEVICE,
    val dates: DateDisplay = DateDisplay.DEVICE,
) {
    companion object {
        const val NAME_LIMIT = 60
    }
}
