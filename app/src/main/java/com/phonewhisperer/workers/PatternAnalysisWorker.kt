package com.phonewhisperer.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonewhisperer.ai_engine.clustering.DBSCANClusterer
import com.phonewhisperer.ai_engine.llm.RuleGenerator
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.repository.EventRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that runs the AI pattern analysis pipeline.
 *
 * Phase 3 implementation. Scheduled to run nightly or triggered manually via Debug.
 * Pipeline:
 * 1. Fetch unprocessed BehaviorEvents from Room
 * 2. Group events by type
 * 3. Run DBSCAN clustering on each type
 * 4. Save detected BehaviorPatterns to DB
 * 5. Feed patterns to RuleGenerator (LLM)
 * 6. Store proposed AutomationRules for user approval
 * 7. Mark original events as processed
 */
@HiltWorker
class PatternAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventRepository: EventRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "PatternAnalysisWorker"
        const val WORK_NAME = "phonewhisperer_pattern_analysis"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Pattern analysis worker starting...")

        val unprocessedEvents = eventRepository.getUnprocessedBehaviorEvents()
        if (unprocessedEvents.size < 10) { // Lowered to 10 for testing
            Log.d(TAG, "Not enough data yet (${unprocessedEvents.size} events). Need 10+.")
            return Result.success()
        }

        try {
            val clusterer = DBSCANClusterer(eps = 0.20, minPts = 3)
            val ruleGenerator = RuleGenerator()

            // 1. Group events by type (we only cluster similar actions)
            val eventsByType = unprocessedEvents.groupBy { it.eventType }

            for ((eventType, events) in eventsByType) {
                Log.d(TAG, "Processing ${events.size} events of type $eventType")

                // 2. Run DBSCAN
                val patterns = clusterer.cluster(events)
                if (patterns.isEmpty()) continue

                // 3. Save Patterns
                for (pattern in patterns) {
                    val patternId = eventRepository.insertBehaviorPattern(pattern)
                    Log.d(TAG, "Saved pattern: ${pattern.description} (ID: $patternId)")

                    // Reconstruct pattern with DB-assigned ID for LLM
                    val savedPattern = pattern.copy(id = patternId)

                    // 4. Generate Rules (3-tier: On-device Gemma → Gemini Cloud → Heuristic)
                    val rules = ruleGenerator.generateRules(listOf(savedPattern), applicationContext)
                    
                    // 5. Save Rules
                    for (rule in rules) {
                        val ruleId = eventRepository.insertAutomationRule(rule)
                        Log.d(TAG, "Proposed rule: ${rule.name} (ID: $ruleId)")
                    }

                    // 6. Mark events as processed (assigned to this cluster ID)
                    val eventIds = events.filter { 
                        // In a real scenario we'd track exactly which event went to which cluster.
                        // For simplicity in this worker, we just mark all passed events as processed.
                        true 
                    }.map { it.id }
                    
                    eventRepository.markBehaviorEventsProcessed(eventIds, pattern.clusterId)
                }
            }

            // Fallback to ensure all events are marked processed even if no clusters found
            val allIds = unprocessedEvents.map { it.id }
            eventRepository.markBehaviorEventsProcessed(allIds, null)

            Log.d(TAG, "Pattern analysis complete. Processed ${unprocessedEvents.size} events.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Pattern analysis failed", e)
            return Result.failure()
        }
    }
}
