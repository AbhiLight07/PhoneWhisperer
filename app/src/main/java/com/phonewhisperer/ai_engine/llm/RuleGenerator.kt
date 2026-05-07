package com.phonewhisperer.ai_engine.llm

import android.util.Log
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import kotlinx.coroutines.delay

/**
 * On-device LLM rule generator.
 *
 * For Phase 3 (Hackathon execution), this is a "Heuristic LLM Mock".
 * It structurally replaces the MediaPipe Gemma 2B SDK to avoid massive
 * model downloads (1.5GB) while perfectly simulating the JSON-structured
 * outputs an LLM would provide.
 *
 * It maps detected DBSCAN patterns (`BehaviorPatternEntity`) into
 * human-readable proposed `AutomationRuleEntity`s.
 */
class RuleGenerator {

    companion object {
        private const val TAG = "RuleGenerator"
    }

    /**
     * Generates automation rules from detected behavior patterns.
     *
     * @param patterns List of BehaviorPatterns from DBSCAN
     * @return List of proposed AutomationRules for user approval
     */
    suspend fun generateRules(patterns: List<BehaviorPatternEntity>): List<AutomationRuleEntity> {
        Log.d(TAG, "Generating rules for ${patterns.size} patterns via Heuristic LLM Mock")
        
        // Simulate LLM inference delay (processing time)
        delay(1500)
        
        val rules = mutableListOf<AutomationRuleEntity>()

        for (pattern in patterns) {
            val rule = inferRuleFromPattern(pattern)
            if (rule != null) {
                rules.add(rule)
            }
        }

        return rules
    }

    private fun inferRuleFromPattern(pattern: BehaviorPatternEntity): AutomationRuleEntity? {
        val name: String
        val description: String
        val triggerType: String
        val triggerValue: String
        val actionType: String
        val actionValue: String

        // Mock LLM prompt/response logic via heuristics
        when (pattern.patternType) {
            BehaviorEvent.TYPE_SILENT_MODE -> {
                name = "Auto-Mute Phone"
                description = "Automatically mute the phone ${formatDays(pattern.dayOfWeekMask)} between ${pattern.startHour}:00 and ${pattern.endHour}:00."
                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "RINGER_MODE"
                actionValue = "SILENT"
            }
            BehaviorEvent.TYPE_GEOFENCE_TRANSITION -> {
                val loc = pattern.associatedLocation ?: "Unknown Place"
                name = "Location Routine: $loc"
                description = "When you arrive at $loc, switch phone to Vibrate."
                triggerType = "LOCATION"
                triggerValue = loc
                actionType = "RINGER_MODE"
                actionValue = "VIBRATE"
            }
            BehaviorEvent.TYPE_NOTIFICATION -> {
                val app = pattern.associatedApps.split(",").firstOrNull() ?: "App"
                name = "Silence $app Notifications"
                description = "You frequently dismiss $app notifications between ${pattern.startHour}:00 and ${pattern.endHour}:00. Want to silence them automatically?"
                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "NOTIFICATION_BLOCK"
                actionValue = app
            }
            BehaviorEvent.TYPE_SCREEN_OFF -> {
                name = "Bedtime Mode"
                description = "You typically stop using your phone around ${pattern.startHour}:00. Enable Do Not Disturb automatically?"
                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "DND"
                actionValue = "ON"
            }
            else -> {
                Log.d(TAG, "Pattern type ${pattern.patternType} unsupported by rule generator yet.")
                return null
            }
        }

        return AutomationRuleEntity(
            patternId = pattern.id,
            name = name,
            description = description,
            triggerType = triggerType,
            triggerValue = triggerValue,
            actionType = actionType,
            actionValue = actionValue,
            status = AutomationRuleEntity.STATUS_PENDING
        )
    }

    private fun formatDays(mask: Int): String {
        // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
        val weekdays = 31 // 1|2|4|8|16
        val weekends = 96 // 32|64
        
        if ((mask and weekdays) == weekdays && (mask and weekends) == 0) return "on weekdays"
        if ((mask and weekends) == weekends && (mask and weekdays) == 0) return "on weekends"
        if (mask == 127) return "every day"
        
        return "on specific days"
    }
}
