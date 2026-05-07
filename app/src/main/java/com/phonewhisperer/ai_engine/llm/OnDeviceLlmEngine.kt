package com.phonewhisperer.ai_engine.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fully on-device LLM engine using MediaPipe + Gemma 2B.
 *
 * This is the TRUE on-device AI component:
 *   - No internet required
 *   - No API key required
 *   - No data ever leaves the device
 *   - Git-safe (no secrets in source code)
 *
 * Architecture:
 *   BehaviorPattern → prompt → [Gemma 2B via MediaPipe] → Natural Language Rule
 *
 * Model setup (one-time):
 *   1. Download Gemma 2B GPU model from Kaggle:
 *      https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-gpu-int4
 *   2. Push to device:
 *      adb push gemma-2b-it-gpu-int4.bin /data/local/tmp/llm/
 *   3. Or place in app's files dir and it auto-detects
 *
 * The model is ~1.3GB — too large for git or APK bundling.
 * It lives only on the device's storage.
 */
object OnDeviceLlmEngine {

    private const val TAG = "OnDeviceLlmEngine"

    // Model filename to look for
    private const val MODEL_FILENAME = "gemma-2b-it-gpu-int4.bin"

    // Known paths where the model might be placed
    private val SEARCH_PATHS = listOf(
        "/data/local/tmp/llm/$MODEL_FILENAME",
        "/sdcard/Download/$MODEL_FILENAME",
        "/sdcard/PhoneWhisperer/$MODEL_FILENAME"
    )

    private var llmInference: LlmInference? = null
    private var modelPath: String? = null

    /**
     * Checks if the Gemma model file exists on the device.
     */
    fun isModelAvailable(context: Context): Boolean {
        if (modelPath != null) return true

        // Check app's internal files directory first
        val internalPath = File(context.filesDir, "llm/$MODEL_FILENAME")
        if (internalPath.exists()) {
            modelPath = internalPath.absolutePath
            Log.d(TAG, "✓ Model found at: ${internalPath.absolutePath}")
            return true
        }

        // Check known external paths
        for (path in SEARCH_PATHS) {
            if (File(path).exists()) {
                modelPath = path
                Log.d(TAG, "✓ Model found at: $path")
                return true
            }
        }

        Log.d(TAG, "Model not found — on-device LLM unavailable")
        return false
    }

    /**
     * Initializes the MediaPipe LLM inference engine.
     * Must be called before [generateRuleDescription].
     * Thread-safe — creates the engine only once.
     */
    @Synchronized
    fun initialize(context: Context): Boolean {
        if (llmInference != null) return true
        if (!isModelAvailable(context)) return false

        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath!!)
                .setMaxTokens(256)
                // .setTopK(40)
                // .setTemperature(0.3f)
                // .setRandomSeed(42)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d(TAG, "✓ On-device Gemma 2B initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize on-device LLM", e)
            false
        }
    }

    /**
     * Generates an enhanced rule using the on-device Gemma model.
     *
     * @param pattern The detected behavior pattern
     * @param heuristicRule The baseline rule from the heuristic generator
     * @return Enhanced rule with LLM description, or original if inference fails
     */
    suspend fun enhanceRule(
        context: Context,
        pattern: BehaviorPatternEntity,
        heuristicRule: AutomationRuleEntity
    ): AutomationRuleEntity = withContext(Dispatchers.IO) {
        if (!initialize(context)) return@withContext heuristicRule

        try {
            val prompt = buildPrompt(pattern, heuristicRule)
            val response = llmInference?.generateResponse(prompt)

            if (response.isNullOrBlank()) {
                Log.w(TAG, "Empty LLM response — using heuristic")
                return@withContext heuristicRule
            }

            // Parse response (expects: Name|Description format)
            val cleanResponse = response.trim().lines().first().trim()
            val parts = cleanResponse.split("|", limit = 2)

            if (parts.size == 2) {
                val llmName = parts[0].trim().take(50)
                val llmDescription = parts[1].trim().take(300)
                Log.d(TAG, "✓ On-device LLM generated: $llmName")

                heuristicRule.copy(
                    name = llmName,
                    description = "$llmDescription\n\n🔒 Generated on-device by Gemma 2B · ${(pattern.confidence * 100).toInt()}% confidence"
                )
            } else {
                heuristicRule.copy(
                    description = "${cleanResponse.take(250)}\n\n🔒 Generated on-device by Gemma 2B · ${(pattern.confidence * 100).toInt()}% confidence"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "On-device inference failed — using heuristic", e)
            heuristicRule
        }
    }

    private fun buildPrompt(pattern: BehaviorPatternEntity, rule: AutomationRuleEntity): String {
        return """Generate a short phone automation rule name and description.

Pattern: ${pattern.patternType}, Time: ${pattern.startHour}:00-${pattern.endHour}:00, Days: ${formatDayMask(pattern.dayOfWeekMask)}, Location: ${pattern.associatedLocation ?: "unknown"}, Seen ${pattern.eventCount} times.
Action: ${rule.actionType} → ${rule.actionValue}

Format: RuleName|Description (1-2 sentences)
Example: 🎓 College Focus|Auto-mute during class hours. Detected from your daily pattern."""
    }

    private fun formatDayMask(mask: Int): String {
        val days = mutableListOf<String>()
        if (mask and (1 shl 1) != 0) days.add("Mon")
        if (mask and (1 shl 2) != 0) days.add("Tue")
        if (mask and (1 shl 3) != 0) days.add("Wed")
        if (mask and (1 shl 4) != 0) days.add("Thu")
        if (mask and (1 shl 5) != 0) days.add("Fri")
        if (mask and (1 shl 6) != 0) days.add("Sat")
        if (mask and (1 shl 7) != 0) days.add("Sun")
        return days.joinToString(", ").ifEmpty { "Everyday" }
    }

    /**
     * Releases the LLM resources. Call when the app is done with inference.
     */
    fun release() {
        llmInference?.close()
        llmInference = null
        Log.d(TAG, "On-device LLM resources released")
    }
}
