package dev.qtremors.eonslate.core.tracker.domain.value

import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The one JSON format for [FieldConfig], shared by the database and, later, backups. The
 * `kind` property names the type; unknown properties are ignored so an older app can still
 * read a config written by a newer one.
 */
object FieldConfigJson {
    val json: Json = Json {
        classDiscriminator = "kind"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(config: FieldConfig): String = json.encodeToString(FieldConfig.serializer(), config)

    /** The config in [text], or null when it is malformed or names a kind this version lacks. */
    fun decodeOrNull(text: String): FieldConfig? = try {
        json.decodeFromString(FieldConfig.serializer(), text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
