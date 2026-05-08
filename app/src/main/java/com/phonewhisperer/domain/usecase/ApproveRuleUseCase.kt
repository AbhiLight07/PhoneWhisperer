package com.phonewhisperer.domain.usecase

import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.repository.EventRepository
import com.phonewhisperer.execution.TimeRuleScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ApproveRuleUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val timeRuleScheduler: TimeRuleScheduler
) {
    suspend operator fun invoke(ruleId: Long) {
        // Update status in DB
        eventRepository.updateRuleStatus(ruleId, AutomationRuleEntity.STATUS_APPROVED)

        // Get the updated rule
        val approvedRules = eventRepository.getApprovedRules().first()
        val rule = approvedRules.find { it.id == ruleId } ?: return

        // Schedule based on trigger type
        when (rule.triggerType) {
            "TIME" -> {
                timeRuleScheduler.scheduleRule(rule)
            }
            "LOCATION" -> {
                // Handled reactively by GeofenceBroadcastReceiver and LocationRuleScheduler
            }
        }
    }
}
