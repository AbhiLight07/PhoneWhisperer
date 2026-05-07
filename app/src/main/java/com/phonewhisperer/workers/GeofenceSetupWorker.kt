package com.phonewhisperer.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonewhisperer.data.collector.GeofenceManager
import com.phonewhisperer.data.local.db.dao.LocationEventDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-time worker that sets up geofences at the user's most-visited locations.
 *
 * Triggered from [DataCollectionWorker] when:
 *   1. Total location events > 20 (enough data for meaningful clusters)
 *   2. Geofences haven't been set up yet (checked via SharedPreferences)
 *
 * Strategy:
 *   - Queries the top visited locations from LocationEventDao
 *   - Groups nearby points (within 200m) as the same "place"
 *   - Registers geofences at places with ≥5 visits
 *   - Labels them as "Place_1", "Place_2", etc. (user can rename later)
 *
 * This is a simplified clustering approach; Phase 3 DBSCAN will refine it.
 */
@HiltWorker
class GeofenceSetupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationEventDao: LocationEventDao,
    private val geofenceManager: GeofenceManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "GeofenceSetupWorker"
        const val WORK_NAME = "phonewhisperer_geofence_setup"
        private const val MIN_VISITS_FOR_GEOFENCE = 5
        private const val CLUSTER_RADIUS_METERS = 200.0
        private const val PREFS_NAME = "phonewhisperer_prefs"
        private const val KEY_GEOFENCES_SETUP = "geofences_setup_complete"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting geofence setup...")

        try {
            // Get all location events for clustering
            val locations = locationEventDao.getAllLocationsSnapshot()

            if (locations.size < 20) {
                Log.d(TAG, "Not enough locations yet (${locations.size}/20) — skipping")
                return Result.success()
            }

            // Simple grid-based clustering: group nearby points
            val clusters = mutableListOf<LocationCluster>()

            for (loc in locations) {
                var matched = false
                for (cluster in clusters) {
                    if (distanceMeters(loc.latitude, loc.longitude, cluster.centerLat, cluster.centerLng) < CLUSTER_RADIUS_METERS) {
                        cluster.addPoint(loc.latitude, loc.longitude)
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    clusters.add(LocationCluster(loc.latitude, loc.longitude))
                }
            }

            // Register geofences at clusters with enough visits
            var geofenceCount = 0
            for ((index, cluster) in clusters.withIndex()) {
                if (cluster.visitCount >= MIN_VISITS_FOR_GEOFENCE) {
                    val label = "Place_${index + 1}"
                    geofenceManager.registerGeofence(
                        requestId = label,
                        latitude = cluster.centerLat,
                        longitude = cluster.centerLng,
                        radiusMeters = GeofenceManager.DEFAULT_RADIUS_METERS
                    )
                    geofenceCount++
                    Log.d(TAG, "Registered geofence: $label @ (${cluster.centerLat}, ${cluster.centerLng}) — ${cluster.visitCount} visits")
                }
            }

            // Mark setup as complete
            applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_GEOFENCES_SETUP, true)
                .apply()

            Log.d(TAG, "Geofence setup complete: $geofenceCount geofences from ${clusters.size} clusters")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Geofence setup failed", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Haversine distance between two lat/lng points in meters.
     */
    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    /**
     * Simple location cluster — accumulates points and maintains a running center.
     */
    private class LocationCluster(initialLat: Double, initialLng: Double) {
        var centerLat: Double = initialLat
            private set
        var centerLng: Double = initialLng
            private set
        var visitCount: Int = 1
            private set

        fun addPoint(lat: Double, lng: Double) {
            // Running average for center
            centerLat = (centerLat * visitCount + lat) / (visitCount + 1)
            centerLng = (centerLng * visitCount + lng) / (visitCount + 1)
            visitCount++
        }
    }
}
