package com.phonewhisperer.ai_engine.feature_engineering

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Encodes temporal features using cyclic (sin/cos) transformations.
 *
 * Raw hour values create artificial distance problems:
 *   23:00 and 01:00 appear 22 hours apart numerically, but are only 2 hours apart.
 *
 * By projecting onto the unit circle:
 *   sin(2π * hour/24), cos(2π * hour/24)
 *
 * we get continuous, wraparound-aware embeddings where temporal proximity
 * is correctly preserved in Euclidean space.
 *
 * Similarly for day-of-week (7-cycle) and month (12-cycle).
 */
object TemporalFeatureEncoder {

    /**
     * Encodes hour of day (0–23) into a 2D cyclic vector.
     * @return Pair(sin_component, cos_component) both in [-1, 1]
     */
    fun encodeHour(hour: Int): Pair<Double, Double> {
        val angle = 2.0 * PI * hour / 24.0
        return Pair(sin(angle), cos(angle))
    }

    /**
     * Encodes day of week (1–7, ISO) into a 2D cyclic vector.
     */
    fun encodeDayOfWeek(dayOfWeek: Int): Pair<Double, Double> {
        val angle = 2.0 * PI * dayOfWeek / 7.0
        return Pair(sin(angle), cos(angle))
    }

    /**
     * Calculates circular distance between two hours (0–23).
     * Returns a value in [0, 1] where 0 = same time, 1 = 12 hours apart.
     */
    fun circularHourDistance(h1: Int, h2: Int): Double {
        val (sin1, cos1) = encodeHour(h1)
        val (sin2, cos2) = encodeHour(h2)
        // Euclidean distance on unit circle, normalized to [0, 1]
        val dx = sin1 - sin2
        val dy = cos1 - cos2
        return kotlin.math.sqrt(dx * dx + dy * dy) / 2.0 // max euclidean on unit circle is 2
    }

    /**
     * Calculates circular distance between two days (1–7).
     * Returns a value in [0, 1].
     */
    fun circularDayDistance(d1: Int, d2: Int): Double {
        val (sin1, cos1) = encodeDayOfWeek(d1)
        val (sin2, cos2) = encodeDayOfWeek(d2)
        val dx = sin1 - sin2
        val dy = cos1 - cos2
        return kotlin.math.sqrt(dx * dx + dy * dy) / 2.0
    }

    /**
     * Returns a human-readable time-of-day segment.
     */
    fun getTimeSegment(hour: Int): String = when (hour) {
        in 5..8 -> "EARLY_MORNING"
        in 9..11 -> "MORNING"
        in 12..13 -> "MIDDAY"
        in 14..17 -> "AFTERNOON"
        in 18..20 -> "EVENING"
        in 21..23 -> "NIGHT"
        else -> "LATE_NIGHT"
    }

    /**
     * Returns whether a day is a weekday or weekend.
     */
    fun isWeekday(dayOfWeek: Int): Boolean = dayOfWeek in 1..5
}
