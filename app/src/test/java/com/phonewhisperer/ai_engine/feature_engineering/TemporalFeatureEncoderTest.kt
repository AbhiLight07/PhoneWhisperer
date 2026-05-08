package com.phonewhisperer.ai_engine.feature_engineering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TemporalFeatureEncoderTest {

    @Test
    fun `encodeTimeOfDay should return valid cyclic coordinates`() {
        // 0:00 (Midnight)
        var coords = TemporalFeatureEncoder.encodeTimeOfDay(0, 0)
        assertEquals(1.0, coords.first, 0.01) // Cos is 1
        assertEquals(0.0, coords.second, 0.01) // Sin is 0

        // 6:00 AM
        coords = TemporalFeatureEncoder.encodeTimeOfDay(6, 0)
        assertEquals(0.0, coords.first, 0.01)
        assertEquals(1.0, coords.second, 0.01)

        // 12:00 PM (Noon)
        coords = TemporalFeatureEncoder.encodeTimeOfDay(12, 0)
        assertEquals(-1.0, coords.first, 0.01)
        assertEquals(0.0, coords.second, 0.01)
    }

    @Test
    fun `cyclic distance between 23_00 and 01_00 should be small`() {
        val (cos1, sin1) = TemporalFeatureEncoder.encodeTimeOfDay(23, 0)
        val (cos2, sin2) = TemporalFeatureEncoder.encodeTimeOfDay(1, 0)

        // Euclidean distance between the points on the circle
        val dist = Math.sqrt(Math.pow(cos1 - cos2, 2.0) + Math.pow(sin1 - sin2, 2.0))
        
        // Should be same as distance between 01:00 and 03:00
        val (cos3, sin3) = TemporalFeatureEncoder.encodeTimeOfDay(3, 0)
        val dist2 = Math.sqrt(Math.pow(cos2 - cos3, 2.0) + Math.pow(sin2 - sin3, 2.0))

        assertEquals(dist, dist2, 0.01)
        assertTrue("Distance should be small", dist < 1.0)
    }
}
