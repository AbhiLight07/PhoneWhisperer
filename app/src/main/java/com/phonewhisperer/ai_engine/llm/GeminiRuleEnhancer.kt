package com.phonewhisperer.ai_engine.llm

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.phonewhisperer.BuildConfig
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity

/**
 * Gemini LLM-powered rule enhancer.
 *
 * Uses Google's Generative AI SDK to generate semantically rich, natural-language
 * automation rules from DBSCAN-detected behavior patterns.
 *
 * Architecture:
 *   DBSCAN → BehaviorPattern → [GeminiRuleEnhancer] → Natural Language Rule
 *
 * Falls back to heuristic RuleGenerator if:
 *   - No API key configured
 *   - Network unavailable
 *   - LLM response parsing fails
 *   - Any exception occurs
 *
 * To enable: Set your API key in your local.properties file as GEMINI_API_KEY.
 * Get one free at https://aistudio.google.com/apikey
 */
object GeminiRuleEnhancer {

    private const val TAG = "GeminiRuleEnhancer"

    /**
     * API key is injected at build time from local.properties via BuildConfig.
     * It is NEVER hardcoded in source — safe to commit this file to git.
     */
    private val apiKey: String get() = BuildConfig.GEMINI_API_KEY

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.3f   // Low creativity for predictable output
                maxOutputTokens = 256
            }
        )
    }

    /**
     * Enhances a heuristic-generated rule with LLM-generated natural language.
     *
     * @param pattern The detected behavior pattern
     * @param heuristicRule The baseline rule from the heuristic generator
     * @return Enhanced rule with LLM description, or original if LLM fails
     */
    suspend fun enhanceRule(
        pattern: BehaviorPatternEntity,
        heuristicRule: AutomationRuleEntity
    ): AutomationRuleEntity {
        if (!isAvailable) return heuristicRule

        return try {
            val prompt = buildPrompt(pattern, heuristicRule)
            val response = model.generateContent(prompt)
            val text = response.text?.trim()

            if (text.isNullOrBlank()) {
                Log.w(TAG, "Empty LLM response — using heuristic")
                return heuristicRule
            }

            // Parse LLM response (expects: Name|Description format)
            val parts = text.split("|", limit = 2)
            if (parts.size == 2) {
                val llmName = parts[0].trim().take(50)
                val llmDescription = parts[1].trim().take(300)

                Log.d(TAG, "✓ LLM enhanced: $llmName")
                heuristicRule.copy(
                    name = llmName,
                    description = "$llmDescription\n\n📊 Confidence: ${(pattern.confidence * 100).toInt()}% · ${pattern.eventCount} observations · AI-generated"
                )
            } else {
                // LLM returned freeform text — use as description only
                heuristicRule.copy(
                    description = "${text.take(250)}\n\n📊 Confidence: ${(pattern.confidence * 100).toInt()}% · ${pattern.eventCount} observations · AI-generated"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM enhancement failed — using heuristic fallback", e)
            heuristicRule
        }
    }

    private fun buildPrompt(pattern: BehaviorPatternEntity, rule: AutomationRuleEntity): String {
        return """
You are PhoneWhisperer, an on-device AI that learns phone usage routines.

Given this detected behavioral pattern:
- Type: ${pattern.patternType}
- Time window: ${pattern.startHour}:00 – ${pattern.endHour}:00
- Days: ${formatDayMask(pattern.dayOfWeekMask)}
- Location: ${pattern.associatedLocation ?: "unknown"}
- Apps involved: ${pattern.associatedApps.ifBlank { "none" }}
- Times observed: ${pattern.eventCount}
- Confidence: ${(pattern.confidence * 100).toInt()}%

Current suggested action: ${rule.actionType} → ${rule.actionValue}

Generate a concise, friendly automation rule. 
Format your response EXACTLY as: RuleName|Description
- RuleName: Short name with one emoji (max 30 chars)
- Description: 1-2 sentences explaining what the rule does and why, written for the phone owner

Example: 🎓 College Focus|Automatically mute your phone during college hours. You consistently silence it weekdays around 9 AM.
        """.trimIndent()
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
        return days.joinToString(", ")
    }
}
