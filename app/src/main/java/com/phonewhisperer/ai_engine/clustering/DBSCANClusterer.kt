package com.phonewhisperer.ai_engine.clustering

import android.util.Log
import com.phonewhisperer.ai_engine.feature_engineering.BehavioralFeaturePipeline
import com.phonewhisperer.ai_engine.feature_engineering.BehavioralFeaturePipeline.FeatureVector
import com.phonewhisperer.ai_engine.feature_engineering.TemporalFeatureEncoder
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * DBSCAN Clustering Engine for behavioral event streams.
 *
 * Phase 6 upgrade: Now uses the Feature Engineering Pipeline for:
 *   - Cyclic sin/cos temporal encoding (23:00 and 01:00 are close)
 *   - Semantic app categorization (Spotify + YouTube Music → MUSIC)
 *   - Normalized distance metrics with proper weighting
 *   - Confidence scoring (density, frequency, temporal consistency)
 *   - Cluster explainability (WHY a pattern was detected)
 *
 * @param eps     Maximum distance between two points to be neighbors [0, 1]
 * @param minPts  Minimum points to form a dense core
 */
class DBSCANClusterer(
    private val eps: Double = 0.18,
    private val minPts: Int = 3
) {
    companion object {
        private const val TAG = "DBSCANClusterer"
        private const val UNCLASSIFIED = 0
        private const val NOISE = -1
    }

    /**
     * Clusters behavior events using enriched feature vectors.
     */
    fun cluster(events: List<BehaviorEvent>): List<BehaviorPatternEntity> {
        if (events.isEmpty()) return emptyList()

        val eventType = events.first().eventType
        Log.d(TAG, "DBSCAN: ${events.size} events of type '$eventType' (eps=$eps, minPts=$minPts)")

        // Step 1: Feature engineering — transform raw events to enriched vectors
        val vectors = BehavioralFeaturePipeline.extractFeatures(events)

        // Step 2: DBSCAN core algorithm
        val labels = IntArray(vectors.size) { UNCLASSIFIED }
        var clusterId = 0

        for (i in vectors.indices) {
            if (labels[i] != UNCLASSIFIED) continue
            val neighbors = regionQuery(vectors, i)
            if (neighbors.size < minPts) {
                labels[i] = NOISE
            } else {
                clusterId++
                expandCluster(vectors, labels, i, neighbors, clusterId)
            }
        }

        // Step 3: Aggregate clusters into BehaviorPatternEntity with confidence scoring
        val patterns = mutableListOf<BehaviorPatternEntity>()
        val groups = vectors.indices.groupBy { labels[it] }

        for ((cid, indices) in groups) {
            if (cid <= 0) continue // Skip noise

            val clusterVectors = indices.map { vectors[it] }
            val clusterEvents = indices.map { events[it] }
            val pattern = buildPattern(cid, eventType, clusterVectors, clusterEvents)
            patterns.add(pattern)
        }

        val noiseCount = labels.count { it == NOISE }
        Log.d(TAG, "DBSCAN result: ${patterns.size} clusters, $noiseCount noise points")
        return patterns
    }

    /**
     * Builds a BehaviorPatternEntity from a cluster with full confidence scoring
     * and explainability.
     */
    private fun buildPattern(
        clusterId: Int,
        eventType: String,
        vectors: List<FeatureVector>,
        events: List<BehaviorEvent>
    ): BehaviorPatternEntity {
        // ── Temporal statistics ─────────────────────────────────────
        var dayMask = 0
        var minHour = 24
        var maxHour = -1
        var minTime = Long.MAX_VALUE
        var maxTime = Long.MIN_VALUE
        val hours = mutableListOf<Int>()
        val days = mutableSetOf<Int>()

        val apps = mutableSetOf<String>()
        val locations = mutableSetOf<String>()
        val appCategories = mutableSetOf<String>()
        val timeSegments = mutableSetOf<String>()

        for (v in vectors) {
            val ev = v.event
            dayMask = dayMask or (1 shl ev.dayOfWeek)
            days.add(ev.dayOfWeek)
            minHour = min(minHour, ev.hourOfDay)
            maxHour = max(maxHour, ev.hourOfDay)
            minTime = min(minTime, ev.timestamp)
            maxTime = max(maxTime, ev.timestamp)
            hours.add(ev.hourOfDay)
            timeSegments.add(v.timeSegment)

            v.appCategory?.let { appCategories.add(it) }
            v.locationLabel?.let { locations.add(it) }

            // Extract app names from payload
            "\"packageName\":\"([^\"]+)\"".toRegex().find(ev.payload)?.groupValues?.get(1)?.let { apps.add(it) }
            "\"label\":\"([^\"]+)\"".toRegex().find(ev.payload)?.groupValues?.get(1)?.let { locations.add(it) }
        }

        // ── Confidence scoring ──────────────────────────────────────
        val confidence = calculateConfidence(vectors, events, hours, days)

        // ── Explainability ──────────────────────────────────────────
        val description = generateExplainableDescription(
            eventType, minHour, maxHour, dayMask, events.size,
            locations.firstOrNull(), appCategories, timeSegments, confidence
        )

        return BehaviorPatternEntity(
            clusterId = clusterId,
            patternType = eventType,
            description = description,
            confidence = confidence,
            dayOfWeekMask = dayMask,
            startHour = minHour,
            endHour = maxHour,
            associatedApps = apps.joinToString(","),
            associatedLocation = locations.firstOrNull(),
            eventCount = events.size,
            firstSeen = minTime,
            lastSeen = maxTime
        )
    }

    /**
     * Multi-factor confidence scoring.
     *
     * Factors:
     *   1. Support: Number of events in cluster (more = higher confidence)
     *   2. Frequency: How often per week this pattern occurs
     *   3. Temporal consistency: How tight the time window is
     *   4. Day consistency: Does it repeat on the same days?
     */
    private fun calculateConfidence(
        vectors: List<FeatureVector>,
        events: List<BehaviorEvent>,
        hours: List<Int>,
        days: Set<Int>
    ): Float {
        // 1. Support score [0, 0.3] — log scale to avoid dominating
        val supportScore = min(0.3f, (events.size / 15.0f) * 0.3f)

        // 2. Frequency score [0, 0.25] — events per week
        val spanDays = max(1L, (events.maxOf { it.timestamp } - events.minOf { it.timestamp }) / (86400000L))
        val eventsPerWeek = events.size.toFloat() / max(1f, spanDays / 7f)
        val frequencyScore = min(0.25f, (eventsPerWeek / 5f) * 0.25f)

        // 3. Temporal consistency [0, 0.25] — standard deviation of hours
        val avgHour = hours.average()
        val hourVariance = hours.map { (it - avgHour) * (it - avgHour) }.average()
        val hourStdDev = kotlin.math.sqrt(hourVariance)
        val temporalScore = (1.0 - min(1.0, hourStdDev / 6.0)).toFloat() * 0.25f

        // 4. Day consistency [0, 0.2] — fewer unique days = more consistent
        val dayConsistency = if (days.size <= 2) 0.2f
                             else if (days.size <= 5) 0.1f
                             else 0.05f

        val total = supportScore + frequencyScore + temporalScore + dayConsistency
        return min(1.0f, total)
    }

    /**
     * Generates human-readable, explainable descriptions.
     */
    private fun generateExplainableDescription(
        type: String, startHour: Int, endHour: Int, dayMask: Int,
        eventCount: Int, location: String?,
        appCategories: Set<String>, timeSegments: Set<String>,
        confidence: Float
    ): String {
        val timeStr = if (startHour == endHour) "around ${formatHour(startHour)}"
                      else "between ${formatHour(startHour)} and ${formatHour(endHour)}"
        val dayStr = formatDays(dayMask)
        val locStr = if (location != null) " at $location" else ""
        val confPct = (confidence * 100).roundToInt()

        return when (type) {
            BehaviorEvent.TYPE_SILENT_MODE ->
                "You silence your phone $dayStr $timeStr$locStr. Detected $eventCount times ($confPct% confidence)."
            BehaviorEvent.TYPE_SCREEN_OFF ->
                "Your screen-off pattern suggests a sleep window $timeStr. Observed $eventCount times ($confPct% confidence)."
            BehaviorEvent.TYPE_NOTIFICATION ->
                "Frequent notification dismissals $timeStr $dayStr. Observed $eventCount times."
            BehaviorEvent.TYPE_GEOFENCE_TRANSITION ->
                "Regular visits$locStr $dayStr $timeStr. Detected $eventCount transitions ($confPct% confidence)."
            BehaviorEvent.TYPE_APP_USAGE -> {
                val catStr = appCategories.joinToString(" & ") { com.phonewhisperer.ai_engine.feature_engineering.UsagePatternVectorizer.getCategoryDisplayName(it) }
                "Frequent $catStr usage $timeStr $dayStr. Observed $eventCount sessions ($confPct% confidence)."
            }
            "RULE_EXECUTED" ->
                "AI automation executed $eventCount times $dayStr $timeStr."
            else ->
                "Behavioral pattern detected $timeStr $dayStr ($eventCount events, $confPct% confidence)."
        }
    }

    private fun formatHour(hour: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "$displayHour $amPm"
    }

    private fun formatDays(mask: Int): String {
        val weekdays = 0b00111110 // Mon-Fri (bits 1-5)
        val weekends = 0b11000000 // Sat-Sun (bits 6-7)
        if ((mask and weekdays) == weekdays && (mask and weekends) == 0) return "on weekdays"
        if ((mask and weekends) == weekends && (mask and weekdays) == 0) return "on weekends"
        if (mask == 0b11111110 || mask == 0b01111111) return "every day"

        val names = mutableListOf<String>()
        if (mask and (1 shl 1) != 0) names.add("Mon")
        if (mask and (1 shl 2) != 0) names.add("Tue")
        if (mask and (1 shl 3) != 0) names.add("Wed")
        if (mask and (1 shl 4) != 0) names.add("Thu")
        if (mask and (1 shl 5) != 0) names.add("Fri")
        if (mask and (1 shl 6) != 0) names.add("Sat")
        if (mask and (1 shl 7) != 0) names.add("Sun")
        return "on " + names.joinToString(", ")
    }

    // ── DBSCAN Core ─────────────────────────────────────────────────

    private fun expandCluster(
        vectors: List<FeatureVector>, labels: IntArray,
        pointIdx: Int, neighbors: MutableList<Int>, clusterId: Int
    ) {
        labels[pointIdx] = clusterId
        var i = 0
        while (i < neighbors.size) {
            val nIdx = neighbors[i]
            if (labels[nIdx] == NOISE) labels[nIdx] = clusterId
            if (labels[nIdx] == UNCLASSIFIED) {
                labels[nIdx] = clusterId
                val nextNeighbors = regionQuery(vectors, nIdx)
                if (nextNeighbors.size >= minPts) {
                    for (n in nextNeighbors) {
                        if (!neighbors.contains(n)) neighbors.add(n)
                    }
                }
            }
            i++
        }
    }

    private fun regionQuery(vectors: List<FeatureVector>, targetIdx: Int): MutableList<Int> {
        val neighbors = mutableListOf<Int>()
        val target = vectors[targetIdx]
        for (i in vectors.indices) {
            if (BehavioralFeaturePipeline.distance(target, vectors[i]) <= eps) {
                neighbors.add(i)
            }
        }
        return neighbors
    }
}
