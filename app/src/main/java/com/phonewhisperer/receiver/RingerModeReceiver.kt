package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
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
 * Detects when the user manually toggles the phone's ringer mode.
 *
 * Listens for [AudioManager.RINGER_MODE_CHANGED_ACTION] and creates
 * BehaviorEvents with TYPE_SILENT_MODE. Each toggle is a direct training
 * signal for DND automation rules.
 *
 * Example pattern DBSCAN might detect:
 *   "User switches to Silent every weekday at 9:00 AM and back to Normal at 5:00 PM"
 *
 * Registered dynamically in PhoneWhispererApp.onCreate() — these implicit
 * broadcasts cannot be received via manifest on Android 8+.
 */
@AndroidEntryPoint
class RingerModeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var eventRepository: EventRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "RingerModeReceiver"

        // Track previous mode for comparison
        @Volatile
        var previousMode: Int = -1

        // Flag to ignore changes caused by our own executor (Phase 4)
        @Volatile
        var ignoreNextChange: Boolean = false

        fun ringerModeToString(mode: Int): String = when (mode) {
            AudioManager.RINGER_MODE_SILENT -> "SILENT"
            AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
            AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
            else -> "UNKNOWN"
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AudioManager.RINGER_MODE_CHANGED_ACTION) return

        // Skip if this change was triggered by our own executor
        if (ignoreNextChange) {
            ignoreNextChange = false
            Log.d(TAG, "Ignoring self-triggered ringer mode change")
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode

        // Skip if mode hasn't actually changed (can fire multiple times)
        if (currentMode == previousMode) return

        val prevModeStr = if (previousMode >= 0) ringerModeToString(previousMode) else "UNKNOWN"
        val currModeStr = ringerModeToString(currentMode)

        Log.d(TAG, "Ringer mode changed: $prevModeStr → $currModeStr")

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        scope.launch {
            try {
                val event = BehaviorEvent(
                    timestamp = now,
                    eventType = BehaviorEvent.TYPE_SILENT_MODE,
                    payload = """{"ringerMode":"$currModeStr","previousMode":"$prevModeStr"}""",
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hourOfDay
                )
                eventRepository.insertBehaviorEvent(event)
                Log.d(TAG, "Ringer mode event recorded: $currModeStr")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record ringer mode change", e)
            }
        }

        previousMode = currentMode
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (Sunday=1) to ISO-8601 (Monday=1, Sunday=7).
     */
    private fun Int.toIsoDayOfWeek(): Int = when (this) {
        Calendar.SUNDAY -> 7
        else -> this - 1
    }
}
