package com.phonewhisperer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonewhisperer.data.local.db.entity.AppUsageEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageEventDao {

    // ── Inserts ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AppUsageEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AppUsageEvent>): List<Long>

    // ── Queries (reactive) ──────────────────────────────────────────

    @Query("SELECT * FROM app_usage_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<AppUsageEvent>>

    @Query("SELECT * FROM app_usage_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<AppUsageEvent>>

    @Query("SELECT * FROM app_usage_events WHERE package_name = :packageName ORDER BY timestamp DESC")
    fun getEventsByPackage(packageName: String): Flow<List<AppUsageEvent>>

    @Query("SELECT * FROM app_usage_events WHERE day_of_week = :dayOfWeek ORDER BY hour_of_day ASC")
    fun getEventsByDayOfWeek(dayOfWeek: Int): Flow<List<AppUsageEvent>>

    // ── Aggregation (for dashboard / pattern detection) ─────────────

    @Query("""
        SELECT package_name, app_name, SUM(duration_ms) as total_duration, COUNT(*) as session_count
        FROM app_usage_events 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY package_name 
        ORDER BY total_duration DESC
    """)
    suspend fun getTopAppsByDuration(startTime: Long, endTime: Long): List<AppUsageSummary>

    @Query("""
        SELECT package_name, app_name, AVG(hour_of_day) as avg_hour, COUNT(*) as frequency
        FROM app_usage_events
        GROUP BY package_name
        HAVING frequency >= :minFrequency
        ORDER BY frequency DESC
    """)
    suspend fun getFrequentAppsWithAvgHour(minFrequency: Int): List<AppFrequencyInfo>

    @Query("SELECT COUNT(*) FROM app_usage_events")
    fun getTotalUsageCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT package_name) FROM app_usage_events")
    fun getDistinctAppCount(): Flow<Int>

    @Query("SELECT SUM(duration_ms) FROM app_usage_events")
    fun getTotalScreenTimeMs(): Flow<Long?>

    // ── Deduplication Check ─────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM app_usage_events 
        WHERE package_name = :packageName 
          AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun countEventsForPackageInRange(
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Int

    // ── Maintenance ─────────────────────────────────────────────────

    @Query("DELETE FROM app_usage_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM app_usage_events")
    suspend fun deleteAll()
}

/**
 * Aggregation result for top apps by usage duration.
 */
data class AppUsageSummary(
    val package_name: String,
    val app_name: String,
    val total_duration: Long,
    val session_count: Int
)

/**
 * Aggregation result for frequently used apps with average usage hour.
 */
data class AppFrequencyInfo(
    val package_name: String,
    val app_name: String,
    val avg_hour: Double,
    val frequency: Int
)
