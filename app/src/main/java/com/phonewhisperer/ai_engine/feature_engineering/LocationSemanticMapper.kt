package com.phonewhisperer.ai_engine.feature_engineering

import android.util.Log
import com.phonewhisperer.data.local.db.entity.LocationEvent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Converts raw GPS coordinates into semantic location labels.
 *
 * Instead of working with raw lat/lng, the clustering pipeline benefits
 * from semantic abstractions:
 *   - HOME: Most visited evening/night location
 *   - WORK: Most visited weekday 9-5 location
 *   - FREQUENT_PLACE: Any cluster with ≥3 visits
 *   - TRANSIT: Points that don't cluster (moving between places)
 *
 * This mapper clusters locations using a simple grid approach and assigns
 * labels based on temporal usage patterns.
 */
object LocationSemanticMapper {

    private const val TAG = "LocationSemanticMapper"
    private const val CLUSTER_RADIUS_METERS = 200.0

    data class SemanticLocation(
        val label: String,          // HOME, WORK, FREQUENT_PLACE, TRANSIT
        val displayName: String,    // "Home", "Work", "Gym"
        val centerLat: Double,
        val centerLng: Double,
        val visitCount: Int,
        val avgHour: Double,        // Average hour of visits
        val weekdayRatio: Float     // 0.0 = all weekends, 1.0 = all weekdays
    )

    /**
     * Analyzes all location events and returns semantic location labels.
     */
    fun mapLocations(events: List<LocationEvent>): List<SemanticLocation> {
        if (events.isEmpty()) return emptyList()

        // Step 1: Cluster locations by proximity
        val clusters = mutableListOf<LocationCluster>()
        for (event in events) {
            var matched = false
            for (cluster in clusters) {
                if (haversineDistance(event.latitude, event.longitude, cluster.centerLat, cluster.centerLng) < CLUSTER_RADIUS_METERS) {
                    cluster.addEvent(event)
                    matched = true
                    break
                }
            }
            if (!matched) {
                clusters.add(LocationCluster(event))
            }
        }

        // Step 2: Assign semantic labels based on temporal patterns
        val semanticLocations = clusters
            .filter { it.visitCount >= 2 }
            .sortedByDescending { it.visitCount }
            .mapIndexed { index, cluster ->
                val label = inferLabel(cluster, index)
                SemanticLocation(
                    label = label,
                    displayName = inferDisplayName(label, index),
                    centerLat = cluster.centerLat,
                    centerLng = cluster.centerLng,
                    visitCount = cluster.visitCount,
                    avgHour = cluster.avgHour,
                    weekdayRatio = cluster.weekdayRatio
                )
            }

        Log.d(TAG, "Mapped ${events.size} location events → ${semanticLocations.size} semantic locations")
        return semanticLocations
    }

    /**
     * Infers a semantic label based on visit patterns.
     */
    private fun inferLabel(cluster: LocationCluster, rank: Int): String {
        return when {
            // HOME: Most visited place with strong evening/night presence
            rank == 0 && (cluster.avgHour >= 18 || cluster.avgHour <= 8) -> "HOME"
            // WORK: High weekday ratio, daytime hours
            cluster.weekdayRatio > 0.7 && cluster.avgHour in 8.0..18.0 -> "WORK"
            // HOME fallback: Most visited if not classified as WORK
            rank == 0 -> "HOME"
            // WORK fallback: Second most visited with weekday pattern
            rank == 1 && cluster.weekdayRatio > 0.5 -> "WORK"
            // Otherwise: frequent place
            cluster.visitCount >= 3 -> "FREQUENT_PLACE"
            else -> "TRANSIT"
        }
    }

    private fun inferDisplayName(label: String, rank: Int): String = when (label) {
        "HOME" -> "Home"
        "WORK" -> "Work/College"
        "FREQUENT_PLACE" -> "Place #${rank + 1}"
        else -> "Transit"
    }

    /**
     * Returns the semantic label for a specific lat/lng based on previously mapped locations.
     */
    fun getLabelForCoordinates(
        lat: Double, lng: Double,
        semanticLocations: List<SemanticLocation>
    ): String {
        for (loc in semanticLocations) {
            if (haversineDistance(lat, lng, loc.centerLat, loc.centerLng) < CLUSTER_RADIUS_METERS) {
                return loc.label
            }
        }
        return "TRANSIT"
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private class LocationCluster(initialEvent: LocationEvent) {
        var centerLat = initialEvent.latitude
            private set
        var centerLng = initialEvent.longitude
            private set
        var visitCount = 1
            private set

        private var totalHour = initialEvent.timestamp.let {
            java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.HOUR_OF_DAY).toDouble()
        }
        private var weekdayCount = if (isWeekday(initialEvent)) 1 else 0

        val avgHour: Double get() = totalHour / visitCount
        val weekdayRatio: Float get() = if (visitCount > 0) weekdayCount.toFloat() / visitCount else 0f

        fun addEvent(event: LocationEvent) {
            centerLat = (centerLat * visitCount + event.latitude) / (visitCount + 1)
            centerLng = (centerLng * visitCount + event.longitude) / (visitCount + 1)
            totalHour += java.util.Calendar.getInstance().apply { timeInMillis = event.timestamp }.get(java.util.Calendar.HOUR_OF_DAY)
            if (isWeekday(event)) weekdayCount++
            visitCount++
        }

        private fun isWeekday(event: LocationEvent): Boolean {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = event.timestamp }
            val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
            return dow != java.util.Calendar.SATURDAY && dow != java.util.Calendar.SUNDAY
        }
    }
}
