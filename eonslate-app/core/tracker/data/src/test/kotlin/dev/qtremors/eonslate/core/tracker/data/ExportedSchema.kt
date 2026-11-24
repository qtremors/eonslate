package dev.qtremors.eonslate.core.tracker.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Builds a database exactly as an older app version left it, from that version's exported
 * schema in `schemas/`, the same way Room's migration helper does. It runs every table, index
 * and setup statement, so the result carries that version's identity hash for Room to check.
 */
object ExportedSchema {
    private val directory = File("schemas/${TrackerDatabase::class.java.name}")

    /** The schema versions exported so far, oldest first. */
    fun versions(): List<Int> =
        directory.listFiles().orEmpty().mapNotNull { it.nameWithoutExtension.toIntOrNull() }.sorted()

    fun create(context: Context, name: String, version: Int): SupportSQLiteDatabase {
        val schema = Json.parseToJsonElement(File(directory, "$version.json").readText())
            .jsonObject.getValue("database").jsonObject
        require(schema.getValue("version").jsonPrimitive.int == version) { "Schema $version names another version" }

        val statements = buildList {
            schema.getValue("entities").jsonArray.map { it.jsonObject }.forEach { entity ->
                val table = entity.text("tableName")
                add(entity.text("createSql").replace("\${TABLE_NAME}", table))
                (entity["indices"]?.jsonArray.orEmpty()).forEach {
                    add(it.jsonObject.text("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { add(it.jsonPrimitive.content) }
        }

        context.deleteDatabase(name)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                    error("A fresh schema database never upgrades")
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}
