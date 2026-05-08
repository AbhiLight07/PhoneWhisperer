package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.di.RepositoryEntryPoint
import com.phonewhisperer.execution.ActionExecutor
import com.phonewhisperer.execution.LocationRuleScheduler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Receives geofence transition events from the Geofencing API.
 *
 * Phase 2: Records BehaviorEvents for DBSCAN clustering.
 * Phase 4: Also checks for approved location-based rules and executes them.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var actionExecutor: ActionExecutor

    @Inject
    lateinit var locationRuleScheduler: LocationRuleScheduler

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

        // Get repository via EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RepositoryEntryPoint::class.java
        )
        val repository = entryPoint.eventRepository()

        for (geofence in triggeringGeofences) {
            val requestId = geofence.requestId
            Log.d(TAG, "Geofence transition: $transitionStr at '$requestId'")

            scope.launch {
                try {
                    // Phase 2: Record the event for DBSCAN
                    val event = BehaviorEvent(
                        timestamp = now,
                        eventType = BehaviorEvent.TYPE_GEOFENCE_TRANSITION,
                        payload = """{"label":"$requestId","transition":"$transitionStr"}""",
                        latitude = triggeringLocation?.latitude,
                        longitude = triggeringLocation?.longitude,
                        dayOfWeek = dayOfWeek,
                        hourOfDay = hourOfDay
                    )
                    repository.insertBehaviorEvent(event)

                    // Phase 4: Check for location-based rules to execute
                    if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        locationRuleScheduler.checkAndExecuteLocationRules(requestId)
                    }

                    Log.d(TAG, "Geofence event recorded: $transitionStr @ $requestId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process geofence transition", e)
                }
            }
        }
    }

    private fun Int.toIsoDayOfWeek(): Int = when (this) {
        Calendar.SUNDAY -> 7
        else -> this - 1
    }
}
