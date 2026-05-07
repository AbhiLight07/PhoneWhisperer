package com.phonewhisperer.domain.model

/**
 * Represents a detected behavioral pattern from DBSCAN clustering.
 *
 * Phase 3 implementation. A BehaviorPattern is a cluster of related events
 * that share temporal and/or spatial proximity, suggesting a repeating routine.
 *
 * Example: "User opens Spotify every weekday at 8:15 AM while commuting"
 */
data class BehaviorPattern(
    val id: Long = 0,
    val clusterId: Int,
    val patternType: String,           // "TIME_BASED", "LOCATION_BASED", "APP_SEQUENCE"
    val description: String,            // Human-readable description from LLM
    val confidence: Float,              // 0.0–1.0 cluster density metric
    val dayOfWeekMask: Int,             // Bitmask: Mon=1, Tue=2, Wed=4, ...
    val hourRange: IntRange,            // e.g., 8..9 for "around 8-9 AM"
    val associatedApps: List<String>,   // Package names involved
    val associatedLocation: String?,    // Cluster label if location-based
    val eventCount: Int,                // Number of events in this cluster
    val firstSeen: Long,                // Earliest event timestamp
    val lastSeen: Long                  // Latest event timestamp
)
