package com.phonewhisperer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorEventDao {

    // ── Inserts ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BehaviorEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<BehaviorEvent>): List<Long>

    // ── Queries (reactive with Flow) ────────────────────────────────

    @Query("SELECT * FROM behavior_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<BehaviorEvent>>

    @Query("SELECT * FROM behavior_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorEvent>>

    @Query("SELECT * FROM behavior_events WHERE event_type = :eventType ORDER BY timestamp DESC")
    fun getEventsByType(eventType: String): Flow<List<BehaviorEvent>>

    @Query("SELECT * FROM behavior_events WHERE event_type = :eventType AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getEventsByTypeAndTimeRange(eventType: String, startTime: Long, endTime: Long): Flow<List<BehaviorEvent>>

    @Query("SELECT * FROM behavior_events WHERE day_of_week = :dayOfWeek AND hour_of_day = :hourOfDay ORDER BY timestamp DESC")
    fun getEventsByDayAndHour(dayOfWeek: Int, hourOfDay: Int): Flow<List<BehaviorEvent>>

    // ── DBSCAN Pipeline Queries (suspend, not Flow — one-shot) ──────

    @Query("SELECT * FROM behavior_events WHERE is_processed = 0 ORDER BY timestamp ASC")
    suspend fun getUnprocessedEvents(): List<BehaviorEvent>

    @Query("UPDATE behavior_events SET is_processed = 1, cluster_id = :clusterId WHERE id IN (:ids)")
    suspend fun markAsProcessed(ids: List<Long>, clusterId: Int?)

    @Query("UPDATE behavior_events SET is_processed = 1 WHERE id IN (:ids)")
    suspend fun markAsProcessed(ids: List<Long>)

    // ── Aggregation Queries ─────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM behavior_events")
    fun getTotalEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM behavior_events WHERE event_type = :eventType")
    fun getEventCountByType(eventType: String): Flow<Int>

    @Query("SELECT event_type, COUNT(*) as count FROM behavior_events GROUP BY event_type")
    suspend fun getEventCountsByType(): List<EventTypeCount>

    @Query("SELECT MAX(timestamp) FROM behavior_events")
    fun getLastEventTimestamp(): Flow<Long?>

    // ── Maintenance ─────────────────────────────────────────────────

    @Query("DELETE FROM behavior_events WHERE timestamp < :cutoffTimestamp AND is_processed = 1")
    suspend fun deleteProcessedOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM behavior_events")
    suspend fun deleteAll()
}

/**
 * Helper data class for GROUP BY aggregation results.
 */
data class EventTypeCount(
    val event_type: String,
    val count: Int
)
