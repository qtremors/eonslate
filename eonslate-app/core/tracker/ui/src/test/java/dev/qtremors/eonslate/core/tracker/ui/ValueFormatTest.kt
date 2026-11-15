package dev.qtremors.eonslate.core.tracker.ui

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ValueFormatTest {
    private val words = ValueWords(
        yes = "Yes",
        no = "No",
        hoursMinutes = { h, m -> "$h h ${m.toString().padStart(2, '0')} m" },
        minutes = { m -> "$m m" },
        minutesSeconds = { m, s -> "$m m ${s.toString().padStart(2, '0')} s" },
        rating = { v, max -> "$v/$max" },
        removedOption = "Removed",
    )
    private val us = Locale.US
    private val options = listOf(ChoiceOption("a", "Apple"), ChoiceOption("b", "Banana"), ChoiceOption("c", "Cherry"))

    private fun f(config: FieldConfig) = FieldDefinition("f", "t", "F", config)
    private fun fmt(config: FieldConfig, value: FieldValue, locale: Locale = us) = ValueFormat.format(f(config), value, words, locale)

    @Test
    fun numbersKeepTheirDecimalsAndUnit() {
        assertEquals("72.5 kg", fmt(FieldConfig.Number("kg", 1), FieldValue.Number(725)))
        assertEquals("1,250.00", fmt(FieldConfig.Number(decimals = 2), FieldValue.Number(125_000)))
        assertEquals("-3", fmt(FieldConfig.Number(), FieldValue.Number(-3)))
        assertEquals("1.250,5", fmt(FieldConfig.Number(decimals = 1), FieldValue.Number(12_505), Locale.GERMANY))
    }

    @Test
    fun moneyUsesTheCurrencysDigitsAndSymbol() {
        assertEquals("$12.34", fmt(FieldConfig.Money("USD"), FieldValue.Money(1_234)))
        assertEquals("-$0.05", fmt(FieldConfig.Money("USD"), FieldValue.Money(-5)))
        assertEquals("¥1,234", fmt(FieldConfig.Money("JPY"), FieldValue.Money(1_234), Locale.JAPAN).replace('￥', '¥'))
        assertEquals("KWD1.234", fmt(FieldConfig.Money("KWD"), FieldValue.Money(1_234)).replace(" ", ""))
    }

    @Test
    fun durationsReadAsHoursAndMinutes() {
        assertEquals("6 h 30 m", fmt(FieldConfig.Duration(), FieldValue.Duration(23_400)))
        assertEquals("45 m", fmt(FieldConfig.Duration(), FieldValue.Duration(2_700)))
        assertEquals("0 m", fmt(FieldConfig.Duration(), FieldValue.Duration(0)))
        assertEquals("1 h 00 m", fmt(FieldConfig.Duration(), FieldValue.Duration(3_600)))
        assertEquals("4 m 05 s", fmt(FieldConfig.Duration(), FieldValue.Duration(245)))
        assertEquals("100 h 00 m", fmt(FieldConfig.Duration(), FieldValue.Duration(360_000)))
    }

    @Test
    fun ratingsYesNoAndText() {
        assertEquals("4/5", fmt(FieldConfig.Rating(5), FieldValue.Rating(4)))
        assertEquals("Yes", fmt(FieldConfig.YesNo, FieldValue.YesNo(true)))
        assertEquals("No", fmt(FieldConfig.YesNo, FieldValue.YesNo(false)))
        assertEquals("hello", fmt(FieldConfig.Text(), FieldValue.Text("hello")))
    }

    @Test
    fun choicesUseLabelsInFieldOrder() {
        assertEquals("Banana", fmt(FieldConfig.SingleChoice(options), FieldValue.SingleChoice("b")))
        assertEquals("Removed", fmt(FieldConfig.SingleChoice(options), FieldValue.SingleChoice("z")))
        assertEquals("Apple, Cherry", fmt(FieldConfig.MultiChoice(options), FieldValue.MultiChoice(setOf("c", "a"))))
        assertEquals("–", fmt(FieldConfig.MultiChoice(options), FieldValue.MultiChoice(emptySet())))
        assertEquals("Removed", fmt(FieldConfig.MultiChoice(options), FieldValue.MultiChoice(setOf("z"))))
    }

    @Test
    fun computedAmountsUseTheFieldsUnits() {
        val weight = f(FieldConfig.Number("kg", 1))
        assertEquals("72.53 kg", ValueFormat.amount(weight, java.math.BigDecimal("72.533"), words, us, decimals = 2))
        assertEquals("-\$35.25", ValueFormat.amount(f(FieldConfig.Money("USD")), java.math.BigDecimal("-35.2500"), words, us, decimals = 2))
        assertEquals("1 h 30 m", ValueFormat.amount(f(FieldConfig.Duration()), java.math.BigDecimal("5400.4"), words, us))
        assertEquals("3.8", ValueFormat.amount(f(FieldConfig.Rating(5)), java.math.BigDecimal("3.75"), words, us, decimals = 1))
    }

    @Test
    fun datesFollowTheLocale() {
        assertEquals("Nov 10, 2026", fmt(FieldConfig.Date, FieldValue.Date(java.time.LocalDate.of(2026, 11, 10).toEpochDay())))
    }
}
