package com.phonewhisperer.data.collector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.phonewhisperer.data.local.db.dao.LocationEventDao
import com.phonewhisperer.data.local.db.entity.LocationEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Collects location data using Google's Fused Location Provider.
 *
 * Design decisions:
 * - Uses PRIORITY_BALANCED_POWER_ACCURACY (cell tower + WiFi, not GPS)
 *   to keep battery impact minimal during passive observation.
 * - Single-shot request per WorkManager cycle rather than continuous listener,
 *   since we only need ~4 samples/hour for routine detection.
 * - Falls back gracefully if location permission is revoked mid-observation.
 */
@Singleton
class LocationCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationEventDao: LocationEventDao
) {
    companion object {
        private const val TAG = "LocationCollector"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Collects a single location fix and persists it to Room.
     *
     * @return true if a location was successfully collected, false if permissions
     *         are missing or location is unavailable.
     */
    suspend fun collectCurrentLocation(): Boolean {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted, skipping collection")
            return false
        }

        return try {
            val location = getCurrentLocation()
            if (location != null) {
                val calendar = Calendar.getInstance().apply { timeInMillis = location.time }

                val event = LocationEvent(
                    timestamp = location.time,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                )

                locationEventDao.insert(event)
                Log.d(TAG, "Location collected: ${location.latitude}, ${location.longitude} (±${location.accuracy}m)")
                true
            } else {
                Log.w(TAG, "Location unavailable")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during location collection", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting location", e)
            false
        }
    }

    /**
     * Gets a single location fix using a one-shot request.
     * Uses suspendCancellableCoroutine to bridge the callback-based API.
     */
    @Suppress("MissingPermission") // Permission checked in collectCurrentLocation()
    private suspend fun getCurrentLocation(): android.location.Location? {
        return suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                10_000L  // interval (not critical for one-shot)
            )
                .setMaxUpdates(1) // Single fix
                .setMaxUpdateDelayMillis(15_000L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )

            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
