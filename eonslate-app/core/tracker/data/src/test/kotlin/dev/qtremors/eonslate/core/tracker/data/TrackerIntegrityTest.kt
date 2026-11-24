package dev.qtremors.eonslate.core.tracker.data

import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.TrackerIntegrity
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The triggers refuse bad rows even when the repositories are bypassed. */
@RunWith(RobolectricTestRunner::class)
class TrackerIntegrityTest {
    private lateinit var db: TrackerDatabase
    private lateinit var sql: SupportSQLiteDatabase

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        RoomTrackerRepository(db, FakeClock()).saveTracker(everyTypeTracker())
        RoomEntryRepository(db).saveEntry(entry())
        sql = db.openHelper.writableDatabase
    }

    @After
    fun tearDown() = db.close()

    private fun assertRefused(statement: String) {
        try {
            sql.execSQL(statement)
            fail("Expected the database to refuse: $statement")
        } catch (_: SQLiteException) {
            // Refused, as it should be.
        }
    }

    private fun value(field: String, type: String, long: String = "NULL", text: String = "NULL") =
        "INSERT OR REPLACE INTO entry_values (entry_id, field_id, type, long_value, text_value) " +
            "VALUES ('e1', 't1-$field', '$type', $long, $text)"

    @Test
    fun everyTriggerIsInstalled() {
        val names = mutableSetOf<String>()
        sql.query("SELECT name FROM sqlite_master WHERE type = 'trigger'").use {
            while (it.moveToNext()) names += it.getString(0)
        }
        assertEquals(TrackerIntegrity.TRIGGER_NAMES.toSet(), names)
    }

    @Test
    fun installingAgainIsHarmless() {
        TrackerIntegrity.install(sql)
        TrackerIntegrity.install(sql)
        everyTriggerIsInstalled()
    }

    @Test
    fun badTrackersAreRefused() {
        assertRefused("UPDATE trackers SET name = '   ' WHERE id = 't1'")
        assertRefused("UPDATE trackers SET color_index = 17 WHERE id = 't1'")
        assertRefused("UPDATE trackers SET color_index = -1 WHERE id = 't1'")
        assertRefused("UPDATE trackers SET updated_at = created_at - 1 WHERE id = 't1'")
    }

    @Test
    fun badFieldsAreRefused() {
        assertRefused("UPDATE tracker_fields SET type = 'location' WHERE id = 't1-note'")
        assertRefused("UPDATE tracker_fields SET name = '' WHERE id = 't1-note'")
    }

    @Test
    fun fieldTypeCannotChangeUnderItsValues() {
        assertRefused("UPDATE tracker_fields SET type = 'rating' WHERE id = 't1-done'")
    }

    @Test
    fun entryCannotMoveTrackers() {
        sql.execSQL("INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, template_id, created_at, updated_at) VALUES ('t2', 'Other', '', 'Star', 0, 0, 1, NULL, 0, 0)")
        assertRefused("UPDATE entries SET tracker_id = 't2' WHERE id = 'e1'")
    }

    @Test
    fun valuesMustUseTheirColumn() {
        assertRefused(value("weight", "number", text = "'12'"))
        assertRefused(value("weight", "number"))
        assertRefused(value("note", "text", long = "1"))
        assertRefused(value("note", "text", long = "1", text = "'x'"))
    }

    @Test
    fun valuesMustMatchTheirFieldType() {
        assertRefused(value("weight", "money", long = "5"))
        assertRefused(value("weight", "location", long = "5"))
    }

    @Test
    fun valueRangesAreEnforced() {
        assertRefused(value("done", "yes_no", long = "2"))
        assertRefused(value("stars", "rating", long = "0"))
        assertRefused(value("slept", "duration", long = "-1"))
    }

    @Test
    fun valueMustBelongToItsEntrysTracker() {
        sql.execSQL("INSERT INTO trackers (id, name, description, icon_name, color_index, archived, position, template_id, created_at, updated_at) VALUES ('t2', 'Other', '', 'Star', 0, 0, 1, NULL, 0, 0)")
        sql.execSQL("INSERT INTO tracker_fields (id, tracker_id, name, type, config_json, required, position) VALUES ('t2-done', 't2', 'Done', 'yes_no', '{\"kind\":\"yes_no\"}', 0, 0)")
        assertRefused("INSERT INTO entry_values (entry_id, field_id, type, long_value, text_value) VALUES ('e1', 't2-done', 'yes_no', 1, NULL)")
    }

    @Test
    fun foreignKeysAreEnforced() {
        assertRefused("INSERT INTO entries (id, tracker_id, occurred_at, zone_id, note, created_at, updated_at) VALUES ('e9', 'ghost', 0, 'UTC', '', 0, 0)")
        assertRefused("INSERT INTO entry_values (entry_id, field_id, type, long_value, text_value) VALUES ('ghost', 't1-done', 'yes_no', 1, NULL)")
    }

    @Test
    fun goodRawRowsAreAccepted() {
        sql.execSQL(value("done", "yes_no", long = "1"))
        sql.execSQL(value("stars", "rating", long = "5"))
        sql.execSQL(value("tags", "multi_choice", text = "'[]'"))
    }
}
