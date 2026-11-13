package dev.qtremors.eonslate.core.tracker.domain.value

import dev.qtremors.eonslate.core.tracker.domain.everyTypeTracker
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldConfigJsonTest {
    @Test
    fun everyConfigRoundTrips() {
        everyTypeTracker().fields.map { it.config }.forEach { config ->
            assertEquals(config, FieldConfigJson.decodeOrNull(FieldConfigJson.encode(config)))
        }
    }

    @Test
    fun kindNamesTheStorageKey() {
        // The database's type column and the JSON kind must always agree.
        everyTypeTracker().fields.map { it.config }.forEach { config ->
            assertTrue(FieldConfigJson.encode(config).contains("\"kind\":\"${config.type.storageKey}\""))
        }
        assertEquals(FieldType.entries.size, everyTypeTracker().fields.map { it.type }.toSet().size)
    }

    @Test
    fun defaultsAreWrittenSoOlderReadersSeeThem() {
        assertEquals("""{"kind":"rating","max":5}""", FieldConfigJson.encode(FieldConfig.Rating()))
    }

    @Test
    fun unknownPropertiesAreIgnored() {
        val newer = """{"kind":"number","decimals":2,"unit":"kg","goal":70}"""
        assertEquals(FieldConfig.Number(unit = "kg", decimals = 2), FieldConfigJson.decodeOrNull(newer))
    }

    @Test
    fun unknownKindAndMalformedTextAreNull() {
        assertNull(FieldConfigJson.decodeOrNull("""{"kind":"location"}"""))
        assertNull(FieldConfigJson.decodeOrNull("""{"decimals":2}"""))
        assertNull(FieldConfigJson.decodeOrNull("not json"))
        assertNull(FieldConfigJson.decodeOrNull(""))
    }

    @Test
    fun storageKeysAreStable() {
        assertEquals(
            listOf("number", "money", "duration", "rating", "yes_no", "single_choice", "multi_choice", "text", "date"),
            FieldType.entries.map { it.storageKey },
        )
        assertEquals(FieldType.MONEY, FieldType.fromStorageKey("money"))
        assertNull(FieldType.fromStorageKey("Money"))
    }
}
