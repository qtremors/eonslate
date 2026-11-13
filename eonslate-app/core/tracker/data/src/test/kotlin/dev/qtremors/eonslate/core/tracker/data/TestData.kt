package dev.qtremors.eonslate.core.tracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase.Companion.configure
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock

const val NOW = 1_762_740_000_000L

/** A clock tests can move by hand. */
class FakeClock(var now: Long = NOW, var zone: String = "Asia/Kolkata") : AppClock {
    override fun nowMillis(): Long = now
    override fun zoneId(): String = zone
}

/** An in-memory database with the same migrations and triggers as the app's. */
fun inMemoryDatabase(): TrackerDatabase = Room
    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TrackerDatabase::class.java)
    .configure()
    .allowMainThreadQueries()
    .build()

val moodOptions = listOf(ChoiceOption("good", "Good"), ChoiceOption("okay", "Okay"), ChoiceOption("bad", "Bad"))

fun field(trackerId: String, id: String, config: FieldConfig, name: String = id, required: Boolean = false) =
    FieldDefinition(id = id, trackerId = trackerId, name = name, config = config, required = required)

/** A tracker with one field of every type. */
fun everyTypeTracker(id: String = "t1", name: String = "Everything", position: Int = 0, createdAt: Long = NOW) = Tracker(
    id = id,
    name = name,
    iconName = "Mood",
    colorIndex = 3,
    position = position,
    fields = listOf(
        field(id, "$id-weight", FieldConfig.Number(unit = "kg", decimals = 1)),
        field(id, "$id-spent", FieldConfig.Money("INR")),
        field(id, "$id-slept", FieldConfig.Duration()),
        field(id, "$id-stars", FieldConfig.Rating(max = 5)),
        field(id, "$id-done", FieldConfig.YesNo),
        field(id, "$id-mood", FieldConfig.SingleChoice(moodOptions)),
        field(id, "$id-tags", FieldConfig.MultiChoice(moodOptions)),
        field(id, "$id-note", FieldConfig.Text()),
        field(id, "$id-day", FieldConfig.Date),
    ),
    createdAt = createdAt,
)

/** A value for every field of [everyTypeTracker]. */
fun everyValue(trackerId: String = "t1"): Map<String, FieldValue> = mapOf(
    "$trackerId-weight" to FieldValue.Number(725),
    "$trackerId-spent" to FieldValue.Money(-12_050),
    "$trackerId-slept" to FieldValue.Duration(27_000),
    "$trackerId-stars" to FieldValue.Rating(4),
    "$trackerId-done" to FieldValue.YesNo(false),
    "$trackerId-mood" to FieldValue.SingleChoice("okay"),
    "$trackerId-tags" to FieldValue.MultiChoice(setOf("good", "bad")),
    "$trackerId-note" to FieldValue.Text("Line with \"quotes\" and émojis 🌙"),
    "$trackerId-day" to FieldValue.Date(20_402),
)

fun entry(
    id: String = "e1",
    trackerId: String = "t1",
    occurredAt: Long = NOW,
    values: Map<String, FieldValue> = everyValue(trackerId),
    note: String = "",
) = Entry(
    id = id,
    trackerId = trackerId,
    occurredAt = occurredAt,
    zoneId = "Asia/Kolkata",
    note = note,
    values = values,
    createdAt = NOW,
)
