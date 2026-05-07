package com.phonewhisperer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a detected behavioral pattern from DBSCAN clustering.
 *
 * Phase 3 AI Engine output. A BehaviorPattern is a dense cluster of
 * related events that share temporal and/or spatial proximity.
 */
@Entity(tableName = "behavior_patterns")
data class BehaviorPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "cluster_id")
    val clusterId: Int,

    @ColumnInfo(name = "pattern_type")
    val patternType: String, // e.g., "SILENT_MODE", "NOTIFICATION"

    @ColumnInfo(name = "description")
    val description: String, // e.g., "Silences phone at 9 AM"

    @ColumnInfo(name = "confidence")
    val confidence: Float, // 0.0 to 1.0 based on cluster density

    @ColumnInfo(name = "day_of_week_mask")
    val dayOfWeekMask: Int, // Bitmask: Mon=1, Tue=2, Wed=4...

    @ColumnInfo(name = "start_hour")
    val startHour: Int,

    @ColumnInfo(name = "end_hour")
    val endHour: Int,

    @ColumnInfo(name = "associated_apps")
    val associatedApps: String, // Comma separated package names

    @ColumnInfo(name = "associated_location")
    val associatedLocation: String?, // e.g., "Place_1"

    @ColumnInfo(name = "event_count")
    val eventCount: Int,

    @ColumnInfo(name = "first_seen")
    val firstSeen: Long,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long
)
