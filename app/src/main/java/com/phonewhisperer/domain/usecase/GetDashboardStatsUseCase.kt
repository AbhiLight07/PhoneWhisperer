package com.phonewhisperer.domain.usecase

import com.phonewhisperer.data.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class DashboardStats(
    val eventCount: Int,
    val patternCount: Int,
    val ruleCount: Int,
    val activeRules: Int,
    val locationsLearned: Int
)

class GetDashboardStatsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<DashboardStats> {
        return combine(
            eventRepository.getBehaviorEventCount(),
            eventRepository.getAllBehaviorPatterns().map { it.size },
            eventRepository.getApprovedRules().map { it.size },
            eventRepository.getAllLocationEvents().map { locations -> locations.distinctBy { it.geofenceId }.size }
        ) { events: Int, patterns: Int, rules: Int, locations: Int ->
            DashboardStats(
                eventCount = events,
                patternCount = patterns,
                ruleCount = rules,
                activeRules = rules, // Active rules are the approved rules
                locationsLearned = locations
            )
        }
    }
}
