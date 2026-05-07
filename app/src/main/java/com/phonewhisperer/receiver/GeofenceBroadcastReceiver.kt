package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.repository.EventRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Receives geofence transition events from the Geofencing API.
 *
 * When the user enters, exits, or dwells at a registered geofence,
 * this receiver fires and records a BehaviorEvent with TYPE_GEOFENCE_TRANSITION.
 *
 * The payload contains the geofence label (e.g., "Home", "Office") and
 * the transition type, which feeds into DBSCAN for location-based pattern detection.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var eventRepository: EventRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "GeofenceReceiver"

        fun transitionToString(type: Int): String = when (type) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN"
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val transitionStr = transitionToString(transitionType)
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val triggeringLocation = geofencingEvent.triggeringLocation

        for (geofence in triggeringGeofences) {
            val requestId = geofence.requestId
            Log.d(TAG, "Geofence transition: $transitionStr at '$requestId'")

            scope.launch {
                try {
                    val event = BehaviorEvent(
                        timestamp = now,
                        eventType = BehaviorEvent.TYPE_GEOFENCE_TRANSITION,
                        payload = """{"label":"$requestId","transition":"$transitionStr"}""",
                        latitude = triggeringLocation?.latitude,
                        longitude = triggeringLocation?.longitude,
                        dayOfWeek = dayOfWeek,
                        hourOfDay = hourOfDay
                    )
                    eventRepository.insertBehaviorEvent(event)
                    Log.d(TAG, "Geofence event recorded: $transitionStr @ $requestId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record geofence transition", e)
                }
            }
        }
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (Sunday=1) to ISO-8601 (Monday=1, Sunday=7).
     */
    private fun Int.toIsoDayOfWeek(): Int = when (this) {
        Calendar.SUNDAY -> 7
        else -> this - 1
    }
}
