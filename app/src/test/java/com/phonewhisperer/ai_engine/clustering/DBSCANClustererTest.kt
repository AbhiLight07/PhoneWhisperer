package com.phonewhisperer.ai_engine.clustering

import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DBSCANClustererTest {

    @Test
    fun `cluster should return empty list when given no events`() {
        val clusterer = DBSCANClusterer(eps = 0.2, minPts = 3)
        val result = clusterer.cluster(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `cluster should identify single dense cluster of similar events`() {
        val clusterer = DBSCANClusterer(eps = 0.2, minPts = 3)
        val events = listOf(
            createEvent(hour = 9, minute = 0),
            createEvent(hour = 9, minute = 5),
            createEvent(hour = 8, minute = 55)
        )
        
        val result = clusterer.cluster(events)
        assertEquals(1, result.size)
        assertEquals(3, result[0].eventCount)
    }

    @Test
    fun `cluster should identify noise points correctly`() {
        val clusterer = DBSCANClusterer(eps = 0.2, minPts = 3)
        val events = listOf(
            createEvent(hour = 9, minute = 0),
            createEvent(hour = 9, minute = 5),
            createEvent(hour = 14, minute = 0) // Noise point
        )
        
        // minPts = 3, so even the two close ones might be noise if they don't form a core point
        val result = clusterer.cluster(events)
        assertEquals(0, result.size) // All noise since no group of 3 exists
    }

    private fun createEvent(hour: Int, minute: Int): BehaviorEvent {
        return BehaviorEvent(
            timestamp = System.currentTimeMillis(),
            eventType = "TYPE_APP_USAGE",
            payload = """{"packageName":"com.example.app","appName":"Example"}""",
            dayOfWeek = 1,
            hourOfDay = hour
        )
    }
}
