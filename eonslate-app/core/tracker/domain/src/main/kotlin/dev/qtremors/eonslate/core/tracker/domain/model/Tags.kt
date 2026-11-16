package dev.qtremors.eonslate.core.tracker.domain.model

import java.util.Locale

object Tags {
    const val MAX_COUNT = 10
    const val MAX_LENGTH = 24

    /** Trimmed, single-spaced, non-blank, cut to [MAX_LENGTH], first spelling of each kept, at most [MAX_COUNT]. */
    fun normalize(tags: List<String>): List<String> {
        val seen = HashSet<String>()
        return tags.asSequence()
            .map { it.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim() }
            .filter { it.isNotEmpty() && seen.add(it.lowercase(Locale.ROOT)) }
            .take(MAX_COUNT)
            .toList()
    }

    /** Every tag in use, sorted, each once regardless of case. */
    fun all(trackers: List<Tracker>): List<String> =
        trackers.flatMap { it.tags }.distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }
}
