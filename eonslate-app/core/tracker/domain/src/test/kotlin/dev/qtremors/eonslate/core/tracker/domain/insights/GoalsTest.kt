package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Goal
import dev.qtremors.eonslate.core.tracker.domain.model.GoalPeriod
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GoalsTest {
    private val zone = "Asia/Kolkata"
    // 2026-11-09 is a Monday.
    private fun day(d: Int) = LocalDate.of(2026, 11, d)
    private fun e(id: String, d: Int, values: Map<String, FieldValue>) =
        Entry(id = id, trackerId = "t", occurredAt = day(d).atTime(9, 0).atZone(ZoneId.of(zone)).toInstant().toEpochMilli(), zoneId = zone, values = values, createdAt = 0)

    private val water = FieldDefinition("w", "t", "Water", FieldConfig.Number("glasses", goal = Goal(8)))
    private val coffee = FieldDefinition("c", "t", "Coffee", FieldConfig.Number("cups", goal = Goal(2, atMost = true)))
    private val run = FieldDefinition("r", "t", "Run", FieldConfig.Duration(goal = Goal(3 * 3600, GoalPeriod.WEEK)))
    private fun tracker(vararg fields: FieldDefinition) = Tracker(id = "t", name = "T", fields = fields.toList(), createdAt = 0)

    @Test
    fun aDailyGoalSumsTheDay() {
        val entries = listOf(e("1", 10, mapOf("w" to FieldValue.Number(5))), e("2", 10, mapOf("w" to FieldValue.Number(4))))
        val progress = Goals.progress(tracker(water), entries, today = day(10)).single()
        assertEquals(9, progress.total)
        assertTrue(progress.met)
        assertEquals(1f, progress.fraction)
    }

    @Test
    fun anAtMostGoalIsMetWithinTheLimitOnceSomethingIsLogged() {
        val within = Goals.progress(tracker(coffee), listOf(e("1", 10, mapOf("c" to FieldValue.Number(2)))), today = day(10)).single()
        assertTrue(within.met)
        val over = Goals.progress(tracker(coffee), listOf(e("1", 10, mapOf("c" to FieldValue.Number(3)))), today = day(10)).single()
        assertFalse(over.met)
        val nothing = Goals.progress(tracker(coffee), emptyList(), today = day(10)).single()
        assertFalse(nothing.met)
    }

    @Test
    fun aWeeklyGoalSumsFromTheWeekStart() {
        val entries = listOf(
            e("1", 8, mapOf("r" to FieldValue.Duration(3600))), // the Sunday before: last week
            e("2", 9, mapOf("r" to FieldValue.Duration(3600))),
            e("3", 11, mapOf("r" to FieldValue.Duration(2 * 3600))),
        )
        val progress = Goals.progress(tracker(run), entries, today = day(12)).single()
        assertEquals(3L * 3600, progress.total)
        assertTrue(progress.met)
    }

    @Test
    fun doneDaysNeedEveryDailyGoalButIgnoreWeeklyOnes() {
        val t = tracker(water, coffee, run)
        val entries = listOf(
            e("1", 10, mapOf("w" to FieldValue.Number(8), "c" to FieldValue.Number(1))),
            e("2", 11, mapOf("w" to FieldValue.Number(8), "c" to FieldValue.Number(4))),
            e("3", 12, mapOf("w" to FieldValue.Number(3), "c" to FieldValue.Number(1))),
        )
        assertEquals(setOf(day(10)), Goals.doneDays(t, entries))
        // Without daily goals, every logged day counts.
        assertEquals(setOf(day(10), day(11), day(12)), Goals.doneDays(tracker(run), entries))
    }
}
