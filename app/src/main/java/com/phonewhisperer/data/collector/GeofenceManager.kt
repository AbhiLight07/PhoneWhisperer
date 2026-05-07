package com.phonewhisperer.data.collector

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.phonewhisperer.receiver.GeofenceBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages geofence registration at frequently-visited locations.
 *
 * Uses the Geofencing API from Google Play Services to register virtual
 * perimeters around detected "places" (home, office, gym, etc.).
 *
 * When the user enters, exits, or dwells at a geofenced location, the
 * [GeofenceBroadcastReceiver] fires and records a BehaviorEvent.
 *
 * Geofences are set up by [GeofenceSetupWorker] after sufficient location
 * data has been collected (≥20 locations with ≥5 visits to a cluster).
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "GeofenceManager"
        const val DEFAULT_RADIUS_METERS = 200f
        const val DWELL_DELAY_MS = 5 * 60 * 1000 // 5 minutes loiter delay
        private const val GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE
    }

    /**
     * Registers a geofence at the given coordinates.
     *
     * @param requestId Unique ID for this geofence (e.g., "home", "office", "place_3")
     * @param latitude Latitude of the center point
     * @param longitude Longitude of the center point
     * @param radiusMeters Radius in meters (default 200m)
     */
    fun registerGeofence(
        requestId: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = DEFAULT_RADIUS_METERS
    ) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot register geofence — location permission not granted")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(GEOFENCE_EXPIRATION)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                Geofence.GEOFENCE_TRANSITION_EXIT or
                Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(DWELL_DELAY_MS)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence registered: $requestId @ ($latitude, $longitude) r=${radiusMeters}m")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofence: $requestId", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering geofence — missing permission", e)
        }
    }

    /**
     * Removes a specific geofence by request ID.
     */
    fun removeGeofence(requestId: String) {
        geofencingClient.removeGeofences(listOf(requestId))
            .addOnSuccessListener { Log.d(TAG, "Geofence removed: $requestId") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to remove geofence: $requestId", e) }
    }

    /**
     * Removes all registered geofences.
     */
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener { Log.d(TAG, "All geofences removed") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to remove all geofences", e) }
    }

    /**
     * Creates the PendingIntent that will be fired on geofence transitions.
     * Points to [GeofenceBroadcastReceiver].
     */
    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = android.content.Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
