package dev.qtremors.eonslate.core.tracker.domain.value

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScaledDecimalTest {
    @Test
    fun parsesExactlyAtFieldPrecision() {
        assertEquals(125L, ScaledDecimal.parse("12.5", 1))
        assertEquals(1250L, ScaledDecimal.parse("12.5", 2))
        assertEquals(-300L, ScaledDecimal.parse("-3", 2))
        assertEquals(125_075L, ScaledDecimal.parse("1,250.75", 2))
        assertEquals(50L, ScaledDecimal.parse(".5", 2))
        assertEquals(1200L, ScaledDecimal.parse("12.00", 2))
        assertEquals(12L, ScaledDecimal.parse("12.000", 0))
    }

    @Test
    fun refusesTooMuchPrecisionInsteadOfRounding() {
        assertNull(ScaledDecimal.parse("12.55", 1))
        assertNull(ScaledDecimal.parse("0.1", 0))
    }

    @Test
    fun refusesNonNumbersAndOverflow() {
        listOf("", " ", "abc", "1e5", "1.2.3", "--1", "12a", "NaN", "Infinity").forEach {
            assertNull(it, ScaledDecimal.parse(it, 2))
        }
        assertNull(ScaledDecimal.parse("92233720368547758.08", 2))
        assertEquals(Long.MAX_VALUE, ScaledDecimal.parse("92233720368547758.07", 2))
    }

    @Test
    fun formatsPlainWithFixedDigits() {
        assertEquals("12.50", ScaledDecimal.format(1250, 2))
        assertEquals("-0.05", ScaledDecimal.format(-5, 2))
        assertEquals("7", ScaledDecimal.format(7, 0))
    }

    @Test
    fun formatThenParseRoundTrips() {
        listOf(0L, 1L, -1L, 999_999L, Long.MIN_VALUE + 1, Long.MAX_VALUE).forEach { scaled ->
            assertEquals(scaled, ScaledDecimal.parse(ScaledDecimal.format(scaled, 3), 3))
        }
    }

    @Test
    fun rescaleGainsExactlyAndLosesHalfUp() {
        assertEquals(12_500L, ScaledDecimal.rescale(125, 1, 3))
        assertEquals(13L, ScaledDecimal.rescale(125, 1, 0))
        assertEquals(-13L, ScaledDecimal.rescale(-125, 1, 0))
        assertEquals(12L, ScaledDecimal.rescale(124, 1, 0))
        assertNull(ScaledDecimal.rescale(Long.MAX_VALUE, 0, 1))
    }
}

class MoneyAmountTest {
    @Test
    fun usesEachCurrencysMinorDigits() {
        assertEquals(2, MoneyAmount.fractionDigits("INR"))
        assertEquals(0, MoneyAmount.fractionDigits("JPY"))
        assertEquals(3, MoneyAmount.fractionDigits("KWD"))
        assertEquals(2, MoneyAmount.fractionDigits("NOPE"))
    }

    @Test
    fun parsesAndFormatsInMinorUnits() {
        assertEquals(1234L, MoneyAmount.parse("12.34", "USD"))
        assertEquals(1234L, MoneyAmount.parse("1234", "JPY"))
        assertNull(MoneyAmount.parse("12.5", "JPY"))
        assertEquals("1.234", MoneyAmount.format(1234, "KWD"))
    }

    @Test
    fun knowsRealCurrenciesOnly() {
        assertTrue(MoneyAmount.isKnownCurrency("EUR"))
        assertFalse(MoneyAmount.isKnownCurrency("eur"))
        assertFalse(MoneyAmount.isKnownCurrency("ZZZ"))
        assertFalse(MoneyAmount.isKnownCurrency(""))
    }
}

class DurationSecondsTest {
    @Test
    fun parsesMinutesAndClockForms() {
        assertEquals(2_700L, DurationSeconds.parse("45"))
        assertEquals(90L, DurationSeconds.parse("1:30"))
        assertEquals(3_900L, DurationSeconds.parse("1:05:00"))
        assertEquals(360_000L, DurationSeconds.parse("100:00:00"))
    }

    @Test
    fun refusesMalformedInput() {
        listOf("", "1:60", "1:00:60", "-5", "1::2", "a", "1:2:3:4", "1.5").forEach {
            assertNull(it, DurationSeconds.parse(it))
        }
    }

    @Test
    fun combineSplitAndFormat() {
        assertEquals(3_723L, DurationSeconds.of(1, 2, 3))
        assertNull(DurationSeconds.of(hours = -1))
        assertNull(DurationSeconds.of(hours = Long.MAX_VALUE))
        assertEquals(DurationSeconds.Parts(1, 2, 3), DurationSeconds.split(3_723))
        assertEquals("1:02:03", DurationSeconds.format(3_723))
        assertEquals("0:00:00", DurationSeconds.format(0))
    }
}

class EpochDaysTest {
    @Test
    fun matchesLocalDate() {
        val date = LocalDate.of(2026, 11, 10)
        assertEquals(date.toEpochDay(), EpochDays.of(date))
        assertEquals(date, EpochDays.toDate(EpochDays.of(date)))
        assertEquals("2026-11-10", EpochDays.format(date.toEpochDay()))
        assertEquals(0L, EpochDays.parse("1970-01-01"))
    }

    @Test
    fun refusesImpossibleDates() {
        assertNull(EpochDays.parse("2026-02-30"))
        assertNull(EpochDays.parse("10/11/2026"))
        assertFalse(EpochDays.isValid(Long.MAX_VALUE))
        assertTrue(EpochDays.isValid(-1))
    }
}
