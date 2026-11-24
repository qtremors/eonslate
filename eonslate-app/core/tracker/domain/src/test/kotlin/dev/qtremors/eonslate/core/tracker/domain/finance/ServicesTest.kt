package dev.qtremors.eonslate.core.tracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicesTest {
    @Test
    fun everyOldServiceIsHereOnceWithAColor() {
        assertTrue(Services.all.size >= 100)
        assertEquals(Services.all.size, Services.all.map { it.id }.toSet().size)
        assertTrue(Services.all.all { it.color ushr 24 == 0xFFL && it.category.isNotBlank() })
    }

    @Test
    fun searchAndIconsFindTheService() {
        val netflix = Services.search("netf").single()
        assertEquals("Netflix", netflix.name)
        assertEquals(netflix, Services.byIcon(Services.PREFIX + netflix.id))
        assertTrue(Services.search("streaming").size > 5)
        assertNull(Services.byIcon("file:abc"))
        assertNull(Services.byIcon(null))
    }
}
