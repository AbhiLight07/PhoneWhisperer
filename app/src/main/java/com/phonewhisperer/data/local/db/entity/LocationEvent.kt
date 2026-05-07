package com.phonewhisperer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Dedicated location tracking for geofence-based clustering.
 *
 * Separated from [BehaviorEvent] because location data is collected at a higher frequency
 * and has unique query patterns (spatial queries, accuracy filtering, label assignment).
 *
 * The [label] field starts as null and gets populated by DBSCAN when clusters of
 * frequently-visited locations are identified (e.g., "Home", "Office", "Gym").
 */
@Entity(
    tableName = "location_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["label"]),
        Index(value = ["geofence_id"])
    ]
)
data class LocationEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,   // GPS accuracy in meters

    @ColumnInfo(name = "label")
    val label: String? = null,   // Auto-assigned: "Home", "Office", etc.

    @ColumnInfo(name = "geofence_id")
    val geofenceId: String? = null,

    @ColumnInfo(name = "speed")
    val speed: Float = 0f,   // m/s — useful for detecting commute vs. stationary

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,

    @ColumnInfo(name = "hour_of_day")
    val hourOfDay: Int
)
