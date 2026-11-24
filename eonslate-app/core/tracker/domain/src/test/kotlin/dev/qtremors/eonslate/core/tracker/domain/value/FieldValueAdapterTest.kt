package dev.qtremors.eonslate.core.tracker.domain.value

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.moodOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldValueAdapterTest {
    @Test
    fun changingDecimalsRescalesKeepingTheShownNumber() {
        val old = FieldConfig.Number(decimals = 1)
        val new = FieldConfig.Number(decimals = 3)
        assertTrue(FieldValueAdapter.needsAdapting(old, new))
        assertEquals(FieldValue.Number(72_500), FieldValueAdapter.adapt(old, new, FieldValue.Number(725)))
        assertEquals(FieldValue.Number(73), FieldValueAdapter.adapt(old, FieldConfig.Number(decimals = 0), FieldValue.Number(725)))
    }

    @Test
    fun rescaleThatOverflowsDropsTheValue() {
        assertNull(
            FieldValueAdapter.adapt(
                FieldConfig.Number(decimals = 0),
                FieldConfig.Number(decimals = 6),
                FieldValue.Number(Long.MAX_VALUE),
            )
        )
    }

    @Test
    fun limitsDoNotTouchLoggedValues() {
        assertFalse(FieldValueAdapter.needsAdapting(FieldConfig.Number(max = 100), FieldConfig.Number(max = 10)))
        assertFalse(FieldValueAdapter.needsAdapting(FieldConfig.Rating(10), FieldConfig.Rating(5)))
        assertFalse(FieldValueAdapter.needsAdapting(FieldConfig.Text(maxLength = 500), FieldConfig.Text(maxLength = 5)))
        assertFalse(FieldValueAdapter.needsAdapting(FieldConfig.Duration(), FieldConfig.Duration(60)))
    }

    @Test
    fun currencyWithOtherDigitsRescales() {
        val inr = FieldConfig.Money("INR")
        val jpy = FieldConfig.Money("JPY")
        assertTrue(FieldValueAdapter.needsAdapting(inr, jpy))
        assertFalse(FieldValueAdapter.needsAdapting(inr, FieldConfig.Money("USD")))
        assertEquals(FieldValue.Money(13), FieldValueAdapter.adapt(inr, jpy, FieldValue.Money(1_250)))
        assertEquals(FieldValue.Money(125_000), FieldValueAdapter.adapt(jpy, inr, FieldValue.Money(1_250)))
    }

    @Test
    fun typeChangeDropsTheValue() {
        assertTrue(FieldValueAdapter.needsAdapting(FieldConfig.YesNo, FieldConfig.Rating()))
        assertNull(FieldValueAdapter.adapt(FieldConfig.YesNo, FieldConfig.Rating(), FieldValue.YesNo(true)))
    }

    @Test
    fun removedOptionsAreDropped() {
        val old = FieldConfig.SingleChoice(moodOptions)
        val renamed = FieldConfig.SingleChoice(moodOptions.map { it.copy(label = it.label + "!") })
        val fewer = FieldConfig.SingleChoice(moodOptions.filterNot { it.id == "bad" })
        assertFalse(FieldValueAdapter.needsAdapting(old, renamed))
        assertFalse(FieldValueAdapter.needsAdapting(old, FieldConfig.SingleChoice(moodOptions + ChoiceOption("new", "New"))))
        assertTrue(FieldValueAdapter.needsAdapting(old, fewer))
        assertNull(FieldValueAdapter.adapt(old, fewer, FieldValue.SingleChoice("bad")))
        assertEquals(FieldValue.SingleChoice("good"), FieldValueAdapter.adapt(old, fewer, FieldValue.SingleChoice("good")))
    }

    @Test
    fun multiChoiceKeepsTheOptionsThatRemain() {
        val old = FieldConfig.MultiChoice(moodOptions)
        val fewer = FieldConfig.MultiChoice(moodOptions.filterNot { it.id == "bad" })
        assertEquals(
            FieldValue.MultiChoice(setOf("good")),
            FieldValueAdapter.adapt(old, fewer, FieldValue.MultiChoice(setOf("good", "bad"))),
        )
        assertEquals(
            FieldValue.MultiChoice(emptySet()),
            FieldValueAdapter.adapt(old, fewer, FieldValue.MultiChoice(setOf("bad"))),
        )
    }
}
