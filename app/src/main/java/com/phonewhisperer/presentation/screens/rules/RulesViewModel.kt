package com.phonewhisperer.presentation.screens.rules

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.repository.EventRepository
import com.phonewhisperer.execution.ActionExecutor
import com.phonewhisperer.execution.TimeRuleScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Rules Approval screen.
 *
 * Phase 3: Exposes pending/approved rule flows.
 * Phase 4: On approve → schedules time-based alarms.
 *          On revoke  → cancels alarms and reverts rule status.
 *          On testExecute → immediately runs the action for demo.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val timeRuleScheduler: TimeRuleScheduler,
    private val actionExecutor: ActionExecutor
) : ViewModel() {

    val pendingRules: StateFlow<List<AutomationRuleEntity>> = eventRepository.getPendingRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedRules: StateFlow<List<AutomationRuleEntity>> = eventRepository.getApprovedRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Approves a rule and schedules its alarm if time-based.
     */
    fun approveRule(ruleId: Long) {
        viewModelScope.launch {
            eventRepository.updateRuleStatus(ruleId, AutomationRuleEntity.STATUS_APPROVED)

            // Find and schedule the rule
            val rules = eventRepository.getApprovedRules().stateIn(viewModelScope).value
            val rule = rules.find { it.id == ruleId }
            if (rule != null && rule.triggerType == "TIME") {
                timeRuleScheduler.scheduleRule(rule)
            }
        }
    }

    /**
     * Rejects a pending rule.
     */
    fun rejectRule(ruleId: Long) {
        viewModelScope.launch {
            eventRepository.updateRuleStatus(ruleId, AutomationRuleEntity.STATUS_REJECTED)
        }
    }

    /**
     * Revokes an active rule — cancels alarm and sets status back to REJECTED.
     */
    fun revokeRule(ruleId: Long) {
        viewModelScope.launch {
            timeRuleScheduler.cancelRule(ruleId)
            eventRepository.updateRuleStatus(ruleId, AutomationRuleEntity.STATUS_REJECTED)
        }
    }

    /**
     * Immediately executes a rule for testing/demo purposes.
     * Does not affect scheduling — the rule will still fire at its next alarm.
     *
     * @return true if the action was executed
     */
    fun testExecuteRule(rule: AutomationRuleEntity): Boolean {
        return actionExecutor.execute(rule)
    }
}
