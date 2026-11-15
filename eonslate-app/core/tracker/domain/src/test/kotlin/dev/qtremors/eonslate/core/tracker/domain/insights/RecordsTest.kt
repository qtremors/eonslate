package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class RecordsTest {
    private val day1 = LocalDate.of(2026, 11, 1)
    private val day2 = LocalDate.of(2026, 11, 2)

    private fun e(day: LocalDate, hour: Int, values: Map<String, FieldValue>) = Entry(
        id = "$day-$hour", trackerId = "t",
        occurredAt = day.atTime(hour, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        zoneId = "UTC", values = values, createdAt = 0,
    )

    @Test
    fun aNumberRecordIsTheHighestSingleValue() {
        val lift = FieldDefinition("k", "t", "Lift", FieldConfig.Number("kg", 1))
        val entries = listOf(
            e(day1, 8, mapOf("k" to FieldValue.Number(800))),
            e(day1, 9, mapOf("k" to FieldValue.Number(825))),
            e(day2, 8, mapOf("k" to FieldValue.Number(810))),
        )
        assertEquals(Record(BigDecimal("82.5"), day1), Records.best(lift, entries))
    }

    @Test
    fun timeAndMoneyRecordsAreTheBestDay() {
        val sleep = FieldDefinition("s", "t", "Sleep", FieldConfig.Duration())
        val spent = FieldDefinition("m", "t", "Spent", FieldConfig.Money("INR"))
        val entries = listOf(
            // Two naps on day 1 add up to more than day 2's single long sleep.
            e(day1, 3, mapOf("s" to FieldValue.Duration(18_000), "m" to FieldValue.Money(1_000))),
            e(day1, 15, mapOf("s" to FieldValue.Duration(10_000), "m" to FieldValue.Money(1_000))),
            e(day2, 3, mapOf("s" to FieldValue.Duration(25_000), "m" to FieldValue.Money(5_000))),
        )
        assertEquals(Record(BigDecimal(28_000), day1), Records.best(sleep, entries))
        assertEquals(Record(BigDecimal("50.00"), day2), Records.best(spent, entries))
    }

    @Test
    fun aTieKeepsTheDayTheRecordWasSet() {
        val pages = FieldDefinition("p", "t", "Pages", FieldConfig.Number())
        val entries = listOf(e(day2, 8, mapOf("p" to FieldValue.Number(40))), e(day1, 8, mapOf("p" to FieldValue.Number(40))))
        assertEquals(day1, Records.best(pages, entries)!!.day)
    }

    @Test
    fun otherFieldsAndEmptyHistoriesHaveNoRecord() {
        assertNull(Records.best(FieldDefinition("p", "t", "Pages", FieldConfig.Number()), emptyList()))
        assertNull(Records.best(FieldDefinition("y", "t", "Done", FieldConfig.YesNo), listOf(e(day1, 8, mapOf("y" to FieldValue.YesNo(true))))))
    }

    @Test
    fun todayProgressCountsTrackersNotEntries() {
        val progress = Today.progress(
            trackerIds = listOf("a", "b", "c"),
            dayOf = mapOf("a" to listOf(day2, day2), "b" to listOf(day1), "gone" to listOf(day2)),
            today = day2,
        )
        assertEquals(TodayProgress(logged = 1, total = 3), progress)
    }
}
