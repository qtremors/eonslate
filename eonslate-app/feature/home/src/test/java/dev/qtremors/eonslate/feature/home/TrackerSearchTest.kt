package dev.qtremors.eonslate.feature.home

import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerSearchTest {
    private val coffee = Tracker(
        id = "t",
        name = "Café spending",
        description = "Morning habit",
        iconName = "Coffee",
        colorIndex = 0,
        position = 0,
        fields = listOf(FieldDefinition(id = "f", trackerId = "t", name = "Amount", config = FieldConfig.Money("INR"))),
        createdAt = 0,
    )

    @Test
    fun blankMatchesEverything() = assertTrue(TrackerSearch.matches(coffee, "  "))

    @Test
    fun caseAndAccentsAreIgnored() = assertTrue(TrackerSearch.matches(coffee, "CAFE"))

    @Test
    fun descriptionAndFieldNamesCount() {
        assertTrue(TrackerSearch.matches(coffee, "morning"))
        assertTrue(TrackerSearch.matches(coffee, "amount"))
    }

    @Test
    fun everyWordMustMatch() {
        assertTrue(TrackerSearch.matches(coffee, "cafe amount"))
        assertFalse(TrackerSearch.matches(coffee, "cafe sleep"))
    }
}
