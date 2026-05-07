package com.phonewhisperer.ai_engine.llm

import android.util.Log
import com.phonewhisperer.ai_engine.feature_engineering.TemporalFeatureEncoder
import com.phonewhisperer.ai_engine.feature_engineering.UsagePatternVectorizer
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import kotlin.math.roundToInt

/**
 * Hybrid Rule Generator: Heuristic + LLM.
 *
 * Pipeline:
 *   BehaviorPattern → Heuristic (fast, reliable baseline)
 *                   → GeminiRuleEnhancer (optional, natural language polish)
 *                   → AutomationRule
 *
 * The heuristic always runs first to guarantee output.
 * Gemini enhances the description ONLY if:
 *   - API key is configured in GeminiRuleEnhancer
 *   - Network is available
 *   - LLM response is valid
 *
 * This dual-layer approach gives us:
 *   - Reliability of on-device heuristics
 *   - Expressiveness of LLM-generated language
 */
class RuleGenerator {

    companion object {
        private const val TAG = "RuleGenerator"
    }

    /**
     * Generates automation rules from detected behavior patterns.
     *
     * 3-tier LLM pipeline (highest priority first):
     *   1. On-device Gemma 2B (MediaPipe) — fully offline, no API key
     *   2. Gemini Cloud API — needs internet + API key
     *   3. Heuristic — always available, instant
     *
     * @param patterns Detected patterns from DBSCAN
     * @param context Android context (needed for on-device model path resolution)
     */
    suspend fun generateRules(
        patterns: List<BehaviorPatternEntity>,
        context: android.content.Context? = null
    ): List<AutomationRuleEntity> {
        // Determine which LLM backend to use
        val onDeviceAvailable = context != null && OnDeviceLlmEngine.isModelAvailable(context)
        val cloudAvailable = GeminiRuleEnhancer.isAvailable

        val mode = when {
            onDeviceAvailable -> "On-Device Gemma 2B (MediaPipe)"
            cloudAvailable -> "Cloud Gemini API"
            else -> "Heuristic-only"
        }
        Log.d(TAG, "Generating rules for ${patterns.size} patterns via $mode")

        // Step 1: Always generate heuristic baseline (fast, reliable)
        val heuristicRules = patterns.mapNotNull { inferRuleFromPattern(it) }

        // Step 2: Enhance with best available LLM
        if (onDeviceAvailable && context != null) {
            Log.d(TAG, "🧠 Enhancing ${heuristicRules.size} rules with on-device Gemma 2B...")
            return heuristicRules.mapIndexed { index, rule ->
                val pattern = patterns.getOrNull(index)
                if (pattern != null) {
                    OnDeviceLlmEngine.enhanceRule(context, pattern, rule)
                } else rule
            }
        }

        if (cloudAvailable) {
            Log.d(TAG, "☁️ Enhancing ${heuristicRules.size} rules with Gemini Cloud API...")
            return heuristicRules.mapIndexed { index, rule ->
                val pattern = patterns.getOrNull(index)
                if (pattern != null) {
                    GeminiRuleEnhancer.enhanceRule(pattern, rule)
                } else rule
            }
        }

        return heuristicRules
    }

    private fun inferRuleFromPattern(pattern: BehaviorPatternEntity): AutomationRuleEntity? {
        val name: String
        val description: String
        val triggerType: String
        val triggerValue: String
        val actionType: String
        val actionValue: String
        val confPct = (pattern.confidence * 100).roundToInt()

        when (pattern.patternType) {

            // ── Silent Mode Pattern ──────────────────────────────────
            BehaviorEvent.TYPE_SILENT_MODE -> {
                val timeSegment = TemporalFeatureEncoder.getTimeSegment(pattern.startHour)
                val location = pattern.associatedLocation

                name = when {
                    location == "WORK" || location == "Work/College" -> "🎓 Focus Mode"
                    timeSegment == "NIGHT" || timeSegment == "LATE_NIGHT" -> "🌙 Bedtime Silence"
                    timeSegment == "MORNING" && isWeekdayMask(pattern.dayOfWeekMask) -> "💼 Work Mode"
                    else -> "🔇 Auto-Mute"
                }

                val dayStr = formatDays(pattern.dayOfWeekMask)
                val contextStr = when {
                    location != null -> "while at $location"
                    isWeekdayMask(pattern.dayOfWeekMask) -> "during your work/college hours"
                    else -> ""
                }

                description = buildString {
                    append("Automatically silence your phone $dayStr at ${formatHour(pattern.startHour)}")
                    if (pattern.startHour != pattern.endHour) append(" – ${formatHour(pattern.endHour)}")
                    if (contextStr.isNotEmpty()) append(" $contextStr")
                    append(".\n\n")
                    append("📊 Confidence: $confPct% · Observed ${pattern.eventCount} times")
                    val spanDays = ((pattern.lastSeen - pattern.firstSeen) / 86400000L).coerceAtLeast(1)
                    append(" over ${spanDays}d")
                }

                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "RINGER_MODE"
                actionValue = "SILENT"
            }

            // ── Geofence Transition Pattern ──────────────────────────
            BehaviorEvent.TYPE_GEOFENCE_TRANSITION -> {
                val loc = pattern.associatedLocation ?: "Frequent Place"

                name = when (loc) {
                    "HOME" -> "🏠 Arriving Home"
                    "WORK", "Work/College" -> "🎓 Arriving at Work"
                    else -> "📍 Location: $loc"
                }

                description = buildString {
                    append("Switch to Vibrate when you arrive at $loc.\n\n")
                    append("📊 Confidence: $confPct% · ${pattern.eventCount} visits detected")
                }

                triggerType = "LOCATION"
                triggerValue = loc
                actionType = "RINGER_MODE"
                actionValue = "VIBRATE"
            }

            // ── Notification Fatigue Pattern ─────────────────────────
            BehaviorEvent.TYPE_NOTIFICATION -> {
                val apps = pattern.associatedApps.split(",").filter { it.isNotBlank() }
                val primaryApp = apps.firstOrNull() ?: return null
                val category = UsagePatternVectorizer.categorize(primaryApp)
                val categoryName = UsagePatternVectorizer.getCategoryDisplayName(category)

                name = when (category) {
                    "SOCIAL" -> "📵 Social Media Quiet Time"
                    "COMMUNICATION" -> "💬 Message Focus"
                    else -> "🔕 Silence $categoryName"
                }

                description = buildString {
                    append("You frequently dismiss ${categoryName.lowercase()} notifications ")
                    append("between ${formatHour(pattern.startHour)} and ${formatHour(pattern.endHour)}. ")
                    append("Auto-silence them during this window?\n\n")
                    append("📊 Confidence: $confPct% · ${pattern.eventCount} dismissals observed")
                }

                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "NOTIFICATION_BLOCK"
                actionValue = primaryApp
            }

            // ── Screen-Off / Sleep Pattern ───────────────────────────
            BehaviorEvent.TYPE_SCREEN_OFF -> {
                name = "🌙 Bedtime Mode"

                description = buildString {
                    append("You typically stop using your phone around ${formatHour(pattern.startHour)}. ")
                    append("Enable Do Not Disturb automatically?\n\n")
                    append("📊 Confidence: $confPct% · Detected ${pattern.eventCount} consistent sleep times")
                }

                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "DND"
                actionValue = "ON"
            }

            // ── App Usage Pattern ────────────────────────────────────
            BehaviorEvent.TYPE_APP_USAGE -> {
                val apps = pattern.associatedApps.split(",").filter { it.isNotBlank() }
                val primaryApp = apps.firstOrNull() ?: return null
                val category = UsagePatternVectorizer.categorize(primaryApp)
                val categoryName = UsagePatternVectorizer.getCategoryDisplayName(category)

                name = when (category) {
                    "MUSIC" -> "🎵 Music Session"
                    "SOCIAL" -> "📱 Social Routine"
                    "PRODUCTIVITY" -> "💻 Work Session"
                    "ENTERTAINMENT" -> "🎬 Entertainment Time"
                    "EDUCATION" -> "📚 Study Session"
                    else -> "📱 $categoryName Routine"
                }

                description = buildString {
                    append("You regularly use ${categoryName.lowercase()} apps ")
                    append("${formatDays(pattern.dayOfWeekMask)} around ${formatHour(pattern.startHour)}. ")
                    append("Detected pattern across ${apps.size} app(s).\n\n")
                    append("📊 Confidence: $confPct% · ${pattern.eventCount} sessions observed")
                }

                // App usage patterns don't have a direct action yet — mark as informational
                triggerType = "TIME"
                triggerValue = "${pattern.startHour}:00"
                actionType = "DND" // Sensible default: focus mode during work
                actionValue = if (category == "PRODUCTIVITY") "PRIORITY" else "ON"
            }

            else -> {
                Log.d(TAG, "Unsupported pattern type: ${pattern.patternType}")
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

    private fun isWeekdayMask(mask: Int): Boolean {
        val weekdays = 0b00111110
        return (mask and weekdays) == weekdays
    }

    private fun formatHour(hour: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "$h:00 $amPm"
    }

    private fun formatDays(mask: Int): String {
        val weekdays = 0b00111110
        val weekends = 0b11000000
        if ((mask and weekdays) == weekdays && (mask and weekends) == 0) return "on weekdays"
        if ((mask and weekends) == weekends && (mask and weekdays) == 0) return "on weekends"
        if (mask == 0b11111110 || mask == 0b01111111) return "every day"
        return "on select days"
    }
}
