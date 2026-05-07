package com.phonewhisperer.ai_engine.feature_engineering

import com.phonewhisperer.data.local.db.entity.BehaviorEvent

/**
 * Orchestrates the full feature engineering pipeline.
 *
 * Transforms raw BehaviorEvents into enriched feature vectors before DBSCAN.
 *
 * Pipeline:
 *   Raw BehaviorEvent → TemporalFeatureEncoder (cyclic time)
 *                      → UsagePatternVectorizer (app categories)
 *                      → LocationSemanticMapper (place labels)
 *                      → EnrichedFeatureVector
 *
 * The enriched vector is what DBSCAN actually clusters on,
 * giving dramatically better cluster quality than raw fields.
 */
object BehavioralFeaturePipeline {

    /**
     * Enriched feature vector for a single BehaviorEvent.
     * This is the input to DBSCAN distance calculations.
     */
    data class FeatureVector(
        val event: BehaviorEvent,
        val hourSin: Double,
        val hourCos: Double,
        val daySin: Double,
        val dayCos: Double,
        val timeSegment: String,      // MORNING, AFTERNOON, NIGHT, etc.
        val isWeekday: Boolean,
        val appCategory: String?,     // SOCIAL, MUSIC, etc. (null if not app-related)
        val locationLabel: String?,   // HOME, WORK, etc. (null if no coordinates)
        val latitude: Double?,
        val longitude: Double?
    )

    /**
     * Transforms a list of raw BehaviorEvents into enriched FeatureVectors.
     */
    fun extractFeatures(events: List<BehaviorEvent>): List<FeatureVector> {
        return events.map { event ->
            val (hourSin, hourCos) = TemporalFeatureEncoder.encodeHour(event.hourOfDay)
            val (daySin, dayCos) = TemporalFeatureEncoder.encodeDayOfWeek(event.dayOfWeek)

            // Extract app category from payload if present
            val appCategory = extractAppCategory(event)

            FeatureVector(
                event = event,
                hourSin = hourSin,
                hourCos = hourCos,
                daySin = daySin,
                dayCos = dayCos,
                timeSegment = TemporalFeatureEncoder.getTimeSegment(event.hourOfDay),
                isWeekday = TemporalFeatureEncoder.isWeekday(event.dayOfWeek),
                appCategory = appCategory,
                locationLabel = null, // Set by caller if location mapping is available
                latitude = event.latitude,
                longitude = event.longitude
            )
        }
    }

    /**
     * Calculates the distance between two feature vectors.
     * Uses properly normalized cyclic encoding instead of raw heuristics.
     *
     * @return Distance in [0, 1] range
     */
    fun distance(v1: FeatureVector, v2: FeatureVector): Double {
        // 1. Temporal distance using cyclic encoding (already normalized by encoder)
        val hourDist = euclidean2D(v1.hourSin, v1.hourCos, v2.hourSin, v2.hourCos) / 2.0
        val dayDist = euclidean2D(v1.daySin, v1.dayCos, v2.daySin, v2.dayCos) / 2.0

        // 2. Weekday/weekend distance
        val weekdayDist = if (v1.isWeekday == v2.isWeekday) 0.0 else 0.5

        // 3. Location distance (Haversine, normalized to [0, 1])
        var locDist = 0.0
        if (v1.latitude != null && v1.longitude != null && v2.latitude != null && v2.longitude != null) {
            val meters = haversineMeters(v1.latitude, v1.longitude, v2.latitude, v2.longitude)
            locDist = (meters / 1000.0).coerceAtMost(1.0) // >1km = max distance
        }

        // 4. App category distance
        var appDist = 0.0
        if (v1.appCategory != null && v2.appCategory != null) {
            appDist = if (v1.appCategory == v2.appCategory) 0.0 else 0.8
        }

        // Weighted combination (normalized components → fair weighting)
        return (hourDist * 0.35) +    // Time is most important
               (dayDist * 0.15) +     // Day pattern
               (weekdayDist * 0.10) + // Weekday vs weekend
               (locDist * 0.25) +     // Location context
               (appDist * 0.15)       // App category
    }

    private fun extractAppCategory(event: BehaviorEvent): String? {
        val pkgMatch = "\"packageName\":\"([^\"]+)\"".toRegex().find(event.payload)
        return pkgMatch?.groupValues?.get(1)?.let { UsagePatternVectorizer.categorize(it) }
    }

    private fun euclidean2D(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        return R * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
}
