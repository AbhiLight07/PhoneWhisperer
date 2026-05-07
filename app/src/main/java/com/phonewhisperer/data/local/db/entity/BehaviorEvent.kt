package com.phonewhisperer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core event table — every observable phone behavior becomes a row.
 *
 * This is the primary data source for DBSCAN clustering. The [dayOfWeek] and [hourOfDay]
 * fields are denormalized from [timestamp] for fast pattern queries without date math at
 * query time.
 *
 * [payload] stores event-type-specific data as a JSON string to keep the schema flexible
 * as we add new event types. Example payloads:
 *   - SILENT_MODE:     {"ringerMode": "SILENT", "previousMode": "NORMAL"}
 *   - APP_OPEN:        {"packageName": "com.spotify.music", "appName": "Spotify"}
 *   - CALENDAR:        {"title": "Team Standup", "location": "Room 4B", "startTime": 1717200000000}
 *   - ALARM:           {"alarmTime": "07:30", "isRepeating": true}
 *   - LOCATION_CHANGE: {"speed": 0.0, "provider": "fused"}
 */
@Entity(
    tableName = "behavior_events",
    indices = [
        Index(value = ["event_type"]),
        Index(value = ["timestamp"]),
        Index(value = ["day_of_week", "hour_of_day"]),
        Index(value = ["is_processed"])
    ]
)
data class BehaviorEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "payload")
    val payload: String = "{}",

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,   // 1 (Monday) – 7 (Sunday), ISO-8601

    @ColumnInfo(name = "hour_of_day")
    val hourOfDay: Int,   // 0–23

    @ColumnInfo(name = "is_processed")
    val isProcessed: Boolean = false,

    @ColumnInfo(name = "cluster_id")
    val clusterId: Int? = null   // Assigned by DBSCAN in Phase 3
) {
    companion object {
        // Event type constants
        const val TYPE_SILENT_MODE = "SILENT_MODE"
        const val TYPE_APP_OPEN = "APP_OPEN"
        const val TYPE_APP_USAGE = "APP_USAGE"
        const val TYPE_LOCATION_CHANGE = "LOCATION_CHANGE"
        const val TYPE_LOCATION = "LOCATION"
        const val TYPE_CALENDAR = "CALENDAR"
        const val TYPE_ALARM = "ALARM"
        const val TYPE_SCREEN_ON = "SCREEN_ON"
        const val TYPE_SCREEN_OFF = "SCREEN_OFF"
        const val TYPE_NOTIFICATION = "NOTIFICATION"
        const val TYPE_GEOFENCE_TRANSITION = "GEOFENCE_TRANSITION"
    }
}
