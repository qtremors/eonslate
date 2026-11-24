package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker

const val NOW = 1_762_740_000_000L
const val TRACKER_ID = "t1"

fun field(id: String, config: FieldConfig, name: String = id, required: Boolean = false) =
    FieldDefinition(id = id, trackerId = TRACKER_ID, name = name, config = config, required = required)

val moodOptions = listOf(ChoiceOption("good", "Good"), ChoiceOption("okay", "Okay"), ChoiceOption("bad", "Bad"))

/** A tracker with one field of every type, so tests can reach each branch. */
fun everyTypeTracker(): Tracker = Tracker(
    id = TRACKER_ID,
    name = "Everything",
    fields = listOf(
        field("weight", FieldConfig.Number(unit = "kg", decimals = 1, min = 0, max = 5_000)),
        field("spent", FieldConfig.Money("INR")),
        field("slept", FieldConfig.Duration(maxSeconds = 86_400)),
        field("stars", FieldConfig.Rating(max = 5)),
        field("done", FieldConfig.YesNo),
        field("mood", FieldConfig.SingleChoice(moodOptions)),
        field("tags", FieldConfig.MultiChoice(moodOptions)),
        field("note", FieldConfig.Text(maxLength = 20)),
        field("day", FieldConfig.Date),
    ),
    createdAt = NOW,
)

fun entry(values: Map<String, FieldValue> = emptyMap(), note: String = "", id: String = "e1") = Entry(
    id = id,
    trackerId = TRACKER_ID,
    occurredAt = NOW,
    zoneId = "Asia/Kolkata",
    note = note,
    values = values,
    createdAt = NOW,
)

/** A value for every field of [everyTypeTracker] that passes its rules. */
fun validValues(): Map<String, FieldValue> = mapOf(
    "weight" to FieldValue.Number(725),
    "spent" to FieldValue.Money(-12_050),
    "slept" to FieldValue.Duration(27_000),
    "stars" to FieldValue.Rating(4),
    "done" to FieldValue.YesNo(true),
    "mood" to FieldValue.SingleChoice("good"),
    "tags" to FieldValue.MultiChoice(setOf("good", "bad")),
    "note" to FieldValue.Text("Felt fine"),
    "day" to FieldValue.Date(20_402),
)
