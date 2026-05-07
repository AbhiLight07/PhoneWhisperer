package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
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
 * Tracks screen on/off events to detect phone usage session boundaries.
 *
 * Listens for [Intent.ACTION_SCREEN_ON] and [Intent.ACTION_SCREEN_OFF] broadcasts.
 * Each event pair defines a "phone usage session" — critical for detecting routines:
 *   - Bedtime (last screen off of the day)
 *   - Wake time (first screen on)
 *   - Commute patterns (extended screen-off during travel hours)
 *
 * Enriches events with battery level and charging state for additional context.
 *
 * Registered dynamically in PhoneWhispererApp.onCreate() — these implicit
 * broadcasts cannot be received via manifest on modern Android.
 */
@AndroidEntryPoint
class ScreenStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var eventRepository: EventRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventType = when (intent.action) {
            Intent.ACTION_SCREEN_ON -> BehaviorEvent.TYPE_SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> BehaviorEvent.TYPE_SCREEN_OFF
            else -> return
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        // Get battery info for context enrichment
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        Log.d(TAG, "Screen ${if (eventType == BehaviorEvent.TYPE_SCREEN_ON) "ON" else "OFF"} — battery: $batteryLevel%, charging: $isCharging")

        scope.launch {
            try {
                val event = BehaviorEvent(
                    timestamp = now,
                    eventType = eventType,
                    payload = """{"batteryLevel":$batteryLevel,"isCharging":$isCharging}""",
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hourOfDay
                )
                eventRepository.insertBehaviorEvent(event)
                Log.d(TAG, "Screen state event recorded: $eventType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record screen state change", e)
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
