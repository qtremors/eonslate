package dev.qtremors.eonslate.core.tracker.domain.time

import java.time.ZoneId
import java.util.UUID

/** The current time and zone, behind an interface so tests can pin them. */
interface AppClock {
    /** Epoch milliseconds now. */
    fun nowMillis(): Long

    /** The IANA id of the device's current time zone, such as "Asia/Kolkata". */
    fun zoneId(): String
}

/** The real device clock. */
object SystemAppClock : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zoneId(): String = ZoneId.systemDefault().id
}

/** Makes ids for new trackers, fields, options and entries. */
fun interface IdGenerator {
    fun newId(): String
}

/** Random UUIDs, so ids never collide across devices or restored backups. */
object UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
