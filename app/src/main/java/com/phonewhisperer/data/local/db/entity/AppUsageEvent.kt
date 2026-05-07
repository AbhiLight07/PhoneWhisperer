package com.phonewhisperer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * App usage sessions aggregated from UsageStatsManager.
 *
 * Each row represents one app usage session (foreground time between app open and
 * app switch/close). The [durationMs] field is critical for DBSCAN — we can cluster
 * apps by time-of-day × duration to detect patterns like "Instagram 30min before bed".
 *
 * [packageName] is indexed for fast lookups; [appName] is a cached human-readable label
 * resolved via PackageManager at insert time.
 */
@Entity(
    tableName = "app_usage_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["package_name"]),
        Index(value = ["day_of_week", "hour_of_day"])
    ]
)
data class AppUsageEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,   // Session start epoch millis

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,   // Foreground time in milliseconds

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,   // 1–7

    @ColumnInfo(name = "hour_of_day")
    val hourOfDay: Int    // 0–23
)
