package com.phonewhisperer.presentation.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phonewhisperer.data.local.db.dao.EventTypeCount
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import com.phonewhisperer.data.repository.EventRepository
import com.phonewhisperer.domain.usecase.DashboardStats
import com.phonewhisperer.domain.usecase.GetDashboardStatsUseCase
import com.phonewhisperer.workers.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the dashboard screen.
 *
 * Phase 6: Now also exposes AI intelligence data:
 *   - Detected behavior patterns (from DBSCAN)
 *   - Active automation rules
 *   - Smart insight summaries
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val eventRepository: EventRepository,
    getDashboardStatsUseCase: GetDashboardStatsUseCase
) : AndroidViewModel(application) {

    // ── Reactive event counts ───────────────────────────────────────
    
    val dashboardStats: StateFlow<DashboardStats?> = getDashboardStatsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val behaviorEventCount: StateFlow<Int> = eventRepository.getBehaviorEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val locationEventCount: StateFlow<Int> = eventRepository.getLocationEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appUsageEventCount: StateFlow<Int> = eventRepository.getAppUsageEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distinctAppCount: StateFlow<Int> = eventRepository.getDistinctAppCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalScreenTimeMs: StateFlow<Long> = eventRepository.getTotalScreenTimeMs()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lastEventTimestamp: StateFlow<Long?> = eventRepository.getLastBehaviorEventTimestamp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Phase 2: Notification counts ────────────────────────────────

    val notificationEventCount: StateFlow<Int> = eventRepository.getNotificationEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationDistinctAppCount: StateFlow<Int> = eventRepository.getNotificationDistinctAppCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Total events ────────────────────────────────────────────────

    val totalEventCount: StateFlow<Int> = combine(
        eventRepository.getBehaviorEventCount(),
        eventRepository.getLocationEventCount(),
        eventRepository.getAppUsageEventCount(),
        eventRepository.getNotificationEventCount()
    ) { behavior, location, usage, notifications -> behavior + location + usage + notifications }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Phase 6: AI Intelligence ────────────────────────────────────

    val detectedPatterns: StateFlow<List<BehaviorPatternEntity>> = eventRepository.getAllBehaviorPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRules: StateFlow<List<AutomationRuleEntity>> = eventRepository.getApprovedRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Collection status ───────────────────────────────────────────

    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    private val _eventTypeCounts = MutableStateFlow<List<EventTypeCount>>(emptyList())
    val eventTypeCounts: StateFlow<List<EventTypeCount>> = _eventTypeCounts.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isCollecting.value = WorkScheduler.isDataCollectionScheduled(application)
            _eventTypeCounts.value = eventRepository.getEventCountsByType()
        }
    }

    fun toggleCollection() {
        viewModelScope.launch {
            if (_isCollecting.value) {
                WorkScheduler.cancelDataCollection(application)
            } else {
                WorkScheduler.scheduleDataCollection(application)
            }
            _isCollecting.value = !_isCollecting.value
        }
    }
}
