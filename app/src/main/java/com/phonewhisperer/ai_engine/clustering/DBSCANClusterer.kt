package com.phonewhisperer.ai_engine.clustering

import android.util.Log
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * DBSCAN Implementation tailored for behavioral event streams.
 *
 * It processes `BehaviorEvent`s and groups them into dense `BehaviorPatternEntity` clusters.
 *
 * Distance metric features:
 * 1. Temporal (hourOfDay): Circular distance (e.g., 23:00 and 01:00 are 2 hours apart).
 * 2. DayOfWeek: Exact match or adjacent.
 * 3. Spatial: Haversine distance (if coordinates are present).
 */
class DBSCANClusterer(
    private val eps: Double = 0.20,
    private val minPts: Int = 3
) {
    companion object {
        private const val TAG = "DBSCANClusterer"
        
        // DBSCAN point labels
        private const val UNCLASSIFIED = 0
        private const val NOISE = -1
    }

    /**
     * Clusters a list of behavior events into behavioral patterns.
     * Events should be passed in grouped by their eventType.
     */
    fun cluster(events: List<BehaviorEvent>): List<BehaviorPatternEntity> {
        if (events.isEmpty()) return emptyList()
        
        val eventType = events.first().eventType
        Log.d(TAG, "Running DBSCAN on ${events.size} events of type $eventType")

        // Map events to cluster IDs. 0 = UNCLASSIFIED, -1 = NOISE, >0 = Cluster ID
        val clusterLabels = IntArray(events.size) { UNCLASSIFIED }
        var currentClusterId = 0

        for (i in events.indices) {
            if (clusterLabels[i] != UNCLASSIFIED) continue

            val neighbors = regionQuery(events, i)

            if (neighbors.size < minPts) {
                clusterLabels[i] = NOISE
            } else {
                currentClusterId++
                expandCluster(events, clusterLabels, i, neighbors, currentClusterId)
            }
        }

        // Aggregate clusters into BehaviorPatternEntity objects
        val patterns = mutableListOf<BehaviorPatternEntity>()
        val groupedByCluster = events.indices.groupBy { clusterLabels[it] }

        for ((clusterId, indices) in groupedByCluster) {
            if (clusterId <= 0) continue // Skip noise and unclassified

            val clusterEvents = indices.map { events[it] }
            
            // Calculate cluster statistics
            var dayMask = 0
            var minHour = 24
            var maxHour = -1
            var minTime = Long.MAX_VALUE
            var maxTime = Long.MIN_VALUE
            
            val apps = mutableSetOf<String>()
            val locations = mutableSetOf<String>()

            clusterEvents.forEach { ev ->
                dayMask = dayMask or (1 shl ev.dayOfWeek)
                minHour = min(minHour, ev.hourOfDay)
                maxHour = max(maxHour, ev.hourOfDay)
                minTime = min(minTime, ev.timestamp)
                maxTime = max(maxTime, ev.timestamp)
                
                // Extract app and location info from payload if present
                if (ev.payload.contains("packageName")) {
                    val match = "\"packageName\":\"([^\"]+)\"".toRegex().find(ev.payload)
                    match?.groupValues?.get(1)?.let { apps.add(it) }
                }
                if (ev.payload.contains("label")) {
                    val match = "\"label\":\"([^\"]+)\"".toRegex().find(ev.payload)
                    match?.groupValues?.get(1)?.let { locations.add(it) }
                }
            }

            // Confidence based on density (events per week duration or raw count)
            val confidence = min(1.0f, clusterEvents.size / 10.0f)

            patterns.add(
                BehaviorPatternEntity(
                    clusterId = clusterId,
                    patternType = eventType,
                    description = generateHeuristicDescription(eventType, minHour, maxHour, dayMask, locations.firstOrNull()),
                    confidence = confidence,
                    dayOfWeekMask = dayMask,
                    startHour = minHour,
                    endHour = maxHour,
                    associatedApps = apps.joinToString(","),
                    associatedLocation = locations.firstOrNull(),
                    eventCount = clusterEvents.size,
                    firstSeen = minTime,
                    lastSeen = maxTime
                )
            )
        }

        Log.d(TAG, "DBSCAN found ${patterns.size} clusters for $eventType")
        return patterns
    }

    private fun expandCluster(
        events: List<BehaviorEvent>,
        labels: IntArray,
        pointIdx: Int,
        neighbors: MutableList<Int>,
        clusterId: Int
    ) {
        labels[pointIdx] = clusterId
        var i = 0
        while (i < neighbors.size) {
            val neighborIdx = neighbors[i]

            if (labels[neighborIdx] == NOISE) {
                labels[neighborIdx] = clusterId
            }

            if (labels[neighborIdx] == UNCLASSIFIED) {
                labels[neighborIdx] = clusterId
                val nextNeighbors = regionQuery(events, neighborIdx)
                if (nextNeighbors.size >= minPts) {
                    // Add new neighbors to the queue if not already present
                    for (n in nextNeighbors) {
                        if (!neighbors.contains(n)) {
                            neighbors.add(n)
                        }
                    }
                }
            }
            i++
        }
    }

    private fun regionQuery(events: List<BehaviorEvent>, targetIdx: Int): MutableList<Int> {
        val neighbors = mutableListOf<Int>()
        val target = events[targetIdx]

        for (i in events.indices) {
            if (distance(target, events[i]) <= eps) {
                neighbors.add(i)
            }
        }
        return neighbors
    }

    /**
     * Calculates distance between two events in feature space.
     * Returns a normalized distance [0.0, 1.0].
     */
    private fun distance(e1: BehaviorEvent, e2: BehaviorEvent): Double {
        // 1. Time distance (circular, 24 hours -> 0-1 range)
        val hourDiff = abs(e1.hourOfDay - e2.hourOfDay)
        val circularHourDiff = min(hourDiff, 24 - hourDiff)
        val timeDist = circularHourDiff / 12.0 // Max distance is 12 hours

        // 2. Day distance (if day matches = 0, if not = 0.5)
        val dayDist = if (e1.dayOfWeek == e2.dayOfWeek) 0.0 else 0.5

        // 3. Location distance (Haversine if coords exist)
        var locDist = 0.0
        if (e1.latitude != null && e1.longitude != null && e2.latitude != null && e2.longitude != null) {
            val R = 6371000.0 // meters
            val dLat = Math.toRadians(e2.latitude - e1.latitude)
            val dLng = Math.toRadians(e2.longitude - e1.longitude)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(e1.latitude)) * Math.cos(Math.toRadians(e2.latitude)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val distMeters = R * c
            
            // Normalize: >1000m is completely different (dist 1.0)
            locDist = min(distMeters / 1000.0, 1.0)
        }

        // Weighted sum
        return (timeDist * 0.5) + (dayDist * 0.2) + (locDist * 0.3)
    }
    
    private fun generateHeuristicDescription(type: String, startHour: Int, endHour: Int, dayMask: Int, location: String?): String {
        val timeStr = if (startHour == endHour) "at $startHour:00" else "between $startHour:00 and $endHour:00"
        val locStr = if (location != null) " at $location" else ""
        return "Frequent $type $timeStr$locStr"
    }
}
