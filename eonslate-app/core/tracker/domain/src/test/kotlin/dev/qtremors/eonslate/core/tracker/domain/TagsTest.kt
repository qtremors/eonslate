package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.model.Tags
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import org.junit.Assert.assertEquals
import org.junit.Test

class TagsTest {
    @Test
    fun tagsAreTrimmedDeduplicatedAndCapped() {
        assertEquals(listOf("Health", "Daily habit"), Tags.normalize(listOf("  Health ", "", "health", "Daily   habit")))
        assertEquals(Tags.MAX_COUNT, Tags.normalize((1..20).map { "t$it" }).size)
        assertEquals(Tags.MAX_LENGTH, Tags.normalize(listOf("x".repeat(40))).single().length)
    }

    @Test
    fun allTagsAreSortedOnceEach() {
        val a = Tracker(id = "a", name = "A", createdAt = 0, tags = listOf("money", "Daily"))
        val b = Tracker(id = "b", name = "B", createdAt = 0, tags = listOf("daily", "Health"))
        assertEquals(listOf("Daily", "Health", "money"), Tags.all(listOf(a, b)))
    }
}
