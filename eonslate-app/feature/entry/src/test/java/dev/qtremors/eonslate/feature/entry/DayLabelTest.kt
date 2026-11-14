package dev.qtremors.eonslate.feature.entry

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayLabelTest {
    private val today = LocalDate.of(2026, 3, 1)

    @Test
    fun theTwoClosestDaysGetWords() {
        assertEquals(DayLabel.TODAY, DayLabel.of(today, today))
        // Across a month end: 28 February is yesterday on 1 March.
        assertEquals(DayLabel.YESTERDAY, DayLabel.of(LocalDate.of(2026, 2, 28), today))
    }

    @Test
    fun everythingElseIsADate() {
        assertEquals(DayLabel.DATE, DayLabel.of(today.minusDays(2), today))
        assertEquals(DayLabel.DATE, DayLabel.of(today.plusDays(1), today))
        assertEquals(DayLabel.DATE, DayLabel.of(today.minusYears(1), today))
    }

    @Test
    fun theDayIsTheEntrysOwnZonesDay() {
        // 18:00 UTC is 23:30 on 1 March in Kolkata and 08:00 on 1 March in Honolulu.
        val instant = Instant.parse("2026-03-01T18:00:00Z")
        val kolkata = instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
        val honolulu = instant.atZone(ZoneId.of("Pacific/Honolulu")).toLocalDate()
        assertEquals(DayLabel.TODAY, DayLabel.of(kolkata, LocalDate.of(2026, 3, 1)))
        assertEquals(DayLabel.YESTERDAY, DayLabel.of(honolulu, LocalDate.of(2026, 3, 2)))
    }
}
