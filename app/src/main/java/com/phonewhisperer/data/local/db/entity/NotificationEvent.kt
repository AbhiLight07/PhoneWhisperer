package com.phonewhisperer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Notification tracking entity — every notification posted/dismissed/clicked becomes a row.
 *
 * Data source: NotificationListenerService.
 *
 * This table enables the DBSCAN pipeline to detect notification-related patterns:
 *   - "User always dismisses WhatsApp group notifications during work hours"
 *   - "User opens email notifications immediately in the morning"
 *
 * [action] tracks the notification lifecycle:
 *   - POSTED:    notification appeared in the shade
 *   - DISMISSED: user swiped it away
 *   - CLICKED:   user tapped to open the notification
 */
@Entity(
    tableName = "notification_events",
    indices = [
        Index(value = ["package_name"]),
        Index(value = ["timestamp"]),
        Index(value = ["day_of_week", "hour_of_day"]),
        Index(value = ["action"])
    ]
)
data class NotificationEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    @ColumnInfo(name = "action")
    val action: String,   // POSTED, DISMISSED, CLICKED

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,   // 1 (Monday) – 7 (Sunday), ISO-8601

    @ColumnInfo(name = "hour_of_day")
    val hourOfDay: Int    // 0–23
) {
    companion object {
        const val ACTION_POSTED = "POSTED"
        const val ACTION_DISMISSED = "DISMISSED"
        const val ACTION_CLICKED = "CLICKED"
    }
}
