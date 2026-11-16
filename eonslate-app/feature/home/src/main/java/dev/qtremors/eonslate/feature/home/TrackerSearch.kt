package dev.qtremors.eonslate.feature.home

import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import java.text.Normalizer
import java.util.Locale

/** Matches trackers by name, description, field names and tags, ignoring case and accents. */
internal object TrackerSearch {
    private val marks = Regex("\\p{Mn}+")

    fun normalize(text: String): String =
        marks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT).trim()

    /** True when [tracker] carries [tag], ignoring case; a null tag matches all. */
    fun hasTag(tracker: Tracker, tag: String?): Boolean = tag == null || tracker.tags.any { it.equals(tag, ignoreCase = true) }

    /** True when every word of [query] appears somewhere in the tracker. Blank matches all. */
    fun matches(tracker: Tracker, query: String): Boolean {
        val words = normalize(query).split(' ').filter(String::isNotBlank)
        if (words.isEmpty()) return true
        val haystack = normalize(
            buildString {
                append(tracker.name).append(' ').append(tracker.description)
                tracker.fields.forEach { append(' ').append(it.name) }
                tracker.tags.forEach { append(' ').append(it) }
            }
        )
        return words.all { it in haystack }
    }
}
