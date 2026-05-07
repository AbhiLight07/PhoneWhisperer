package com.phonewhisperer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonewhisperer.data.local.db.entity.NotificationEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {

    // ── Inserts ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NotificationEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<NotificationEvent>): List<Long>

    // ── Queries (reactive with Flow) ────────────────────────────────

    @Query("SELECT * FROM notification_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<NotificationEvent>>

    @Query("SELECT * FROM notification_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<NotificationEvent>>

    @Query("SELECT * FROM notification_events WHERE package_name = :packageName ORDER BY timestamp DESC")
    fun getEventsByPackage(packageName: String): Flow<List<NotificationEvent>>

    @Query("SELECT * FROM notification_events WHERE action = :action ORDER BY timestamp DESC")
    fun getEventsByAction(action: String): Flow<List<NotificationEvent>>

    @Query("SELECT * FROM notification_events WHERE day_of_week = :dayOfWeek AND hour_of_day = :hourOfDay ORDER BY timestamp DESC")
    fun getEventsByDayAndHour(dayOfWeek: Int, hourOfDay: Int): Flow<List<NotificationEvent>>

    // ── Aggregation Queries ─────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM notification_events")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT package_name) FROM notification_events")
    fun getDistinctAppCount(): Flow<Int>

    @Query("SELECT action, COUNT(*) as count FROM notification_events GROUP BY action")
    suspend fun getCountByAction(): List<NotificationActionCount>

    @Query("SELECT package_name, COUNT(*) as count FROM notification_events GROUP BY package_name ORDER BY count DESC LIMIT :limit")
    suspend fun getTopAppsByNotificationCount(limit: Int = 10): List<NotificationAppCount>

    @Query("SELECT COUNT(*) FROM notification_events WHERE action = :action")
    fun getCountForAction(action: String): Flow<Int>

    // ── Maintenance ─────────────────────────────────────────────────

    @Query("DELETE FROM notification_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM notification_events")
    suspend fun deleteAll()
}

/**
 * Helper data class for action-based aggregation.
 */
data class NotificationActionCount(
    val action: String,
    val count: Int
)

/**
 * Helper data class for per-app notification count.
 */
data class NotificationAppCount(
    val package_name: String,
    val count: Int
)
