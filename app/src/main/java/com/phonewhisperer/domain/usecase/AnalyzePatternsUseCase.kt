package com.phonewhisperer.domain.usecase

import android.content.Context
import android.util.Log
import com.phonewhisperer.ai_engine.clustering.DBSCANClusterer
import com.phonewhisperer.ai_engine.llm.RuleGenerator
import com.phonewhisperer.data.repository.EventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class AnalysisResult(
    val patternsFound: Int,
    val rulesGenerated: Int
)

class AnalyzePatternsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): Result<AnalysisResult> {
        return try {
            val unprocessedEvents = eventRepository.getUnprocessedBehaviorEvents()
            if (unprocessedEvents.size < 10) {
                return Result.success(AnalysisResult(0, 0))
            }

            var totalPatterns = 0
            var totalRules = 0

            val clusterer = DBSCANClusterer(eps = 0.20, minPts = 3)
            val ruleGenerator = RuleGenerator()
            val eventsByType = unprocessedEvents.groupBy { it.eventType }

            for ((eventType, events) in eventsByType) {
                val patterns = clusterer.cluster(events)
                if (patterns.isEmpty()) continue

                for (pattern in patterns) {
                    val patternId = eventRepository.insertBehaviorPattern(pattern)
                    val savedPattern = pattern.copy(id = patternId)
                    totalPatterns++

                    val rules = ruleGenerator.generateRules(listOf(savedPattern), context)
                    for (rule in rules) {
                        eventRepository.insertAutomationRule(rule)
                        totalRules++
                    }
                    
                    val eventIds = events.map { it.id }
                    eventRepository.markBehaviorEventsProcessed(eventIds, pattern.clusterId)
                }
            }

            // Fallback for events that did not cluster
            val allIds = unprocessedEvents.map { it.id }
            eventRepository.markBehaviorEventsProcessed(allIds, null)

            Result.success(AnalysisResult(totalPatterns, totalRules))
        } catch (e: Exception) {
            Log.e("AnalyzePatternsUseCase", "Analysis failed", e)
            Result.failure(e)
        }
    }
}
