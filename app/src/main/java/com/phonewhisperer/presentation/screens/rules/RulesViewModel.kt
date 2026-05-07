package com.phonewhisperer.presentation.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Rules Approval screen.
 * Exposes flows for Pending (proposed) rules and Approved (active) rules.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    val pendingRules: StateFlow<List<AutomationRuleEntity>> = eventRepository.getPendingRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedRules: StateFlow<List<AutomationRuleEntity>> = eventRepository.getApprovedRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveRule(ruleId: Long) {
        viewModelScope.launch {
            eventRepository.updateRuleStatus(ruleId, AutomationRuleEntity.STATUS_APPROVED)
        }
    }

    fun rejectRule(ruleId: Long) {
        viewModelScope.launch {
            eventRepository.updateRuleStatus(ruleId, AutomationRuleEntity.STATUS_REJECTED)
        }
    }
}
