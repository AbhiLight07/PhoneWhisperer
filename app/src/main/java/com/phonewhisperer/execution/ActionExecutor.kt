package com.phonewhisperer.execution

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.receiver.RingerModeReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performs system-level actions to enforce approved AutomationRules.
 *
 * This is the final "ACT" component of the OBSERVE → INFER → ACT pipeline.
 * It talks directly to Android system APIs to execute rule actions like:
 *   - Setting ringer mode (Silent / Vibrate / Normal)
 *   - Toggling Do Not Disturb
 *   - (Future) Launching apps, setting brightness, toggling WiFi
 *
 * IMPORTANT: Actions that modify ringer mode set `RingerModeReceiver.ignoreNextChange`
 * to prevent our own changes from being re-recorded as user behavior.
 */
@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationBlockManager: NotificationBlockManager
) {
    companion object {
        private const val TAG = "ActionExecutor"
    }

    /**
     * Executes the action defined by the given automation rule.
     *
     * @param rule The approved automation rule to execute
     * @return true if the action was executed successfully
     */
    fun execute(rule: AutomationRuleEntity): Boolean {
        Log.d(TAG, "Executing rule: ${rule.name} (action=${rule.actionType}, value=${rule.actionValue})")

        return try {
            when (rule.actionType) {
                "RINGER_MODE" -> executeRingerMode(rule.actionValue)
                "DND" -> executeDnd(rule.actionValue)
                "NOTIFICATION_BLOCK" -> {
                    val packageName = rule.actionValue
                    var startHour: Int? = null
                    var endHour: Int? = null
                    
                    try {
                        val trigger = rule.triggerValue
                        if (trigger.contains("-")) {
                            val parts = trigger.split("-")
                            startHour = parts[0].trim().toIntOrNull()
                            endHour = parts[1].trim().toIntOrNull()
                        } else {
                            startHour = trigger.toIntOrNull()
                            endHour = startHour?.plus(1) // Default to 1 hour block if only start is given
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse time window from triggerValue: ${rule.triggerValue}", e)
                    }

                    notificationBlockManager.addBlock(packageName, startHour, endHour)
                    Log.d(TAG, "✓ Added $packageName to NotificationBlockManager window $startHour-$endHour")
                    true
                }
                else -> {
                    Log.w(TAG, "Unknown action type: ${rule.actionType}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute rule: ${rule.name}", e)
            false
        }
    }

    /**
     * Sets the phone's ringer mode.
     *
     * Sets `RingerModeReceiver.ignoreNextChange = true` so our own programmatic
     * change isn't re-recorded as a user training signal.
     */
    private fun executeRingerMode(value: String): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Prevent our own change from being logged as user behavior
        RingerModeReceiver.ignoreNextChange = true

        val mode = when (value.uppercase()) {
            "SILENT" -> AudioManager.RINGER_MODE_SILENT
            "VIBRATE" -> AudioManager.RINGER_MODE_VIBRATE
            "NORMAL" -> AudioManager.RINGER_MODE_NORMAL
            else -> {
                Log.w(TAG, "Unknown ringer mode value: $value")
                return false
            }
        }

        audioManager.ringerMode = mode
        Log.d(TAG, "✓ Ringer mode set to: $value")
        return true
    }

    /**
     * Toggles Do Not Disturb mode.
     *
     * Requires ACCESS_NOTIFICATION_POLICY permission (granted in Phase 2 manifest).
     * The user must also grant DND access in system settings.
     */
    private fun executeDnd(value: String): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "DND policy access not granted — cannot toggle DND")
            return false
        }

        // Prevent our own ringer change from being logged
        RingerModeReceiver.ignoreNextChange = true

        val filter = when (value.uppercase()) {
            "ON", "SILENT" -> NotificationManager.INTERRUPTION_FILTER_NONE
            "PRIORITY" -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
            "ALARMS" -> NotificationManager.INTERRUPTION_FILTER_ALARMS
            "OFF", "ALL" -> NotificationManager.INTERRUPTION_FILTER_ALL
            else -> {
                Log.w(TAG, "Unknown DND value: $value")
                return false
            }
        }

        notificationManager.setInterruptionFilter(filter)
        Log.d(TAG, "✓ DND set to: $value (filter=$filter)")
        return true
    }
}
