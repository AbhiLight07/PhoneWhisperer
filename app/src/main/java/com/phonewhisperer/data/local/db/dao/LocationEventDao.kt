package com.phonewhisperer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonewhisperer.data.local.db.entity.LocationEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationEventDao {

    // ── Inserts ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LocationEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<LocationEvent>): List<Long>

    // ── Queries (reactive) ──────────────────────────────────────────

    @Query("SELECT * FROM location_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<LocationEvent>>

    @Query("SELECT * FROM location_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<LocationEvent>>

    @Query("SELECT * FROM location_events WHERE label = :label ORDER BY timestamp DESC")
    fun getEventsByLabel(label: String): Flow<List<LocationEvent>>

    @Query("SELECT * FROM location_events WHERE label IS NULL ORDER BY timestamp ASC")
    suspend fun getUnlabeledLocations(): List<LocationEvent>

    // ── Spatial Queries ─────────────────────────────────────────────

    /**
     * Get locations within a rough bounding box. Not a true radius query
     * (that requires Haversine), but fast enough for DBSCAN pre-filtering.
     * For a ~500m radius at mid-latitudes: delta ≈ 0.0045 degrees.
     */
    @Query("""
        SELECT * FROM location_events 
        WHERE latitude BETWEEN (:centerLat - :deltaLat) AND (:centerLat + :deltaLat)
          AND longitude BETWEEN (:centerLng - :deltaLng) AND (:centerLng + :deltaLng)
        ORDER BY timestamp DESC
    """)
    suspend fun getEventsNearLocation(
        centerLat: Double,
        centerLng: Double,
        deltaLat: Double,
        deltaLng: Double
    ): List<LocationEvent>

    // ── Label Updates ───────────────────────────────────────────────

    @Query("UPDATE location_events SET label = :label WHERE id IN (:ids)")
    suspend fun updateLabels(ids: List<Long>, label: String)

    // ── Aggregation ─────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM location_events")
    fun getTotalLocationCount(): Flow<Int>

    @Query("SELECT DISTINCT label FROM location_events WHERE label IS NOT NULL")
    fun getDistinctLabels(): Flow<List<String>>

    // ── Snapshot (one-shot, non-reactive) ──────────────────────────

    @Query("SELECT * FROM location_events ORDER BY timestamp ASC")
    suspend fun getAllLocationsSnapshot(): List<LocationEvent>

    // ── Maintenance ─────────────────────────────────────────────────

    @Query("DELETE FROM location_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM location_events")
    suspend fun deleteAll()
}
