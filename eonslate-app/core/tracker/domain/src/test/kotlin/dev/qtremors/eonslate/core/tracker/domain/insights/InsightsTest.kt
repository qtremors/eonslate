package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class StreaksTest {
    private val today = LocalDate.of(2026, 11, 10)
    private fun days(vararg back: Int) = back.map { today.minusDays(it.toLong()) }

    @Test
    fun nothingLoggedIsNoStreak() {
        assertEquals(Streak(0, 0, null, null), Streaks.of(emptyList(), today))
    }

    @Test
    fun aRunEndingTodayCounts() {
        assertEquals(Streak(3, 3, today, today), Streaks.of(days(0, 1, 2), today))
        assertEquals(true, Streaks.of(days(0, 1, 2), today).longestIsCurrent)
    }

    @Test
    fun todayNotLoggedYetKeepsYesterdaysRunAlive() {
        assertEquals(Streak(2, 2, today.minusDays(1), today.minusDays(1)), Streaks.of(days(1, 2), today))
    }

    @Test
    fun aGapBreaksTheCurrentRunButNotTheLongest() {
        val streak = Streaks.of(days(0, 3, 4, 5, 6, 10), today)
        assertEquals(1, streak.current)
        assertEquals(4, streak.longest)
        assertEquals(today.minusDays(3), streak.longestEnd)
        assertEquals(false, streak.longestIsCurrent)
        assertEquals(0, Streaks.of(days(2, 3), today).current)
    }

    @Test
    fun repeatedDaysCountOnce() {
        assertEquals(Streak(2, 2, today, today), Streaks.of(days(0, 0, 1, 1), today))
    }

    @Test
    fun runsCrossMonthAndYearEnds() {
        val newYear = LocalDate.of(2027, 1, 1)
        val streak = Streaks.of(listOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), newYear), newYear)
        assertEquals(3, streak.current)
    }
}

class FieldInsightsTest {
    private val zone = "Asia/Kolkata"
    private val today = LocalDate.of(2026, 11, 10)

    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).atZone(ZoneId.of(zone)).toInstant().toEpochMilli()
    private fun e(id: String, day: LocalDate, hour: Int = 9, values: Map<String, FieldValue>) =
        Entry(id = id, trackerId = "t", occurredAt = at(day, hour), zoneId = zone, values = values, createdAt = 0)

    private val weight = FieldDefinition("w", "t", "Weight", FieldConfig.Number("kg", 1))
    private val spent = FieldDefinition("m", "t", "Spent", FieldConfig.Money("INR"))
    private val slept = FieldDefinition("s", "t", "Slept", FieldConfig.Duration())

    @Test
    fun numbersAreSummedAndAveragedExactly() {
        val entries = listOf(
            e("a", today.minusDays(2), values = mapOf("w" to FieldValue.Number(725))),
            e("b", today, values = mapOf("w" to FieldValue.Number(720))),
            e("c", today.minusDays(1), values = mapOf("w" to FieldValue.Number(731))),
        )
        val stats = FieldInsights.stats(weight, entries) as FieldStats.Numeric
        assertEquals(3, stats.count)
        assertEquals(BigDecimal("217.6"), stats.sum)
        // 217.6 / 3 = 72.5333..., kept to the field's decimals plus two.
        assertEquals(BigDecimal("72.533"), stats.average)
        assertEquals(BigDecimal("72.0"), stats.min)
        assertEquals(BigDecimal("73.1"), stats.max)
        // Latest is the one that happened last, even though it was not the last in the list.
        assertEquals(BigDecimal("72.0"), stats.latest)
    }

    @Test
    fun moneyKeepsItsCurrencyDigitsAndSigns() {
        val entries = listOf(
            e("a", today, values = mapOf("m" to FieldValue.Money(-12_050))),
            e("b", today, values = mapOf("m" to FieldValue.Money(5_000))),
        )
        val stats = FieldInsights.stats(spent, entries) as FieldStats.Numeric
        assertEquals(BigDecimal("-70.50"), stats.sum)
        assertEquals(BigDecimal("-35.2500"), stats.average)
    }

    @Test
    fun anUnfilledFieldHasEmptyStats() {
        val stats = FieldInsights.stats(weight, listOf(e("a", today, values = emptyMap()))) as FieldStats.Numeric
        assertEquals(0, stats.count)
        assertEquals(BigDecimal("0.0"), stats.sum)
    }

    @Test
    fun ratingsHaveAnAverageAndAFullDistribution() {
        val mood = FieldDefinition("r", "t", "Mood", FieldConfig.Rating(5))
        val entries = listOf(4, 5, 4, 2).mapIndexed { i, r -> e("$i", today, values = mapOf("r" to FieldValue.Rating(r))) }
        val stats = FieldInsights.stats(mood, entries) as FieldStats.Rating
        assertEquals(BigDecimal("3.75"), stats.average)
        assertEquals(mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 2, 5 to 1), stats.distribution)
    }

    @Test
    fun yesNoRateRoundsHalfUp() {
        val done = FieldDefinition("y", "t", "Done", FieldConfig.YesNo)
        val entries = listOf(true, true, false).mapIndexed { i, v -> e("$i", today, values = mapOf("y" to FieldValue.YesNo(v))) }
        val stats = FieldInsights.stats(done, entries) as FieldStats.YesNo
        assertEquals(2, stats.yes)
        assertEquals(1, stats.no)
        assertEquals(67, stats.yesPercent)
        assertEquals(0, FieldStats.YesNo(0, 0).yesPercent)
    }

    @Test
    fun choicesCountInOptionOrder() {
        val options = listOf(ChoiceOption("a", "A"), ChoiceOption("b", "B"), ChoiceOption("c", "C"))
        val many = FieldDefinition("c", "t", "Tags", FieldConfig.MultiChoice(options))
        val entries = listOf(setOf("a", "c"), setOf("c"), emptySet()).mapIndexed { i, s -> e("$i", today, values = mapOf("c" to FieldValue.MultiChoice(s))) }
        val stats = FieldInsights.stats(many, entries) as FieldStats.Choices
        assertEquals(3, stats.count)
        assertEquals(listOf("a" to 1, "b" to 0, "c" to 2), stats.picks)
    }
}
