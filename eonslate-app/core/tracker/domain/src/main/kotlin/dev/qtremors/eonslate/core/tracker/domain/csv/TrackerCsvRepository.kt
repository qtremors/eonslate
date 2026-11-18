package dev.qtremors.eonslate.core.tracker.domain.csv

import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker

data class TrackerHistory(val tracker: Tracker, val entries: List<Entry>)

interface TrackerCsvRepository {
    suspend fun readHistory(trackerId: String): TrackerHistory?
    suspend fun append(expected: Tracker, entries: List<Entry>)
}
