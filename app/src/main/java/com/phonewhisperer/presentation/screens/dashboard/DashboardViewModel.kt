package com.phonewhisperer.presentation.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phonewhisperer.data.local.db.dao.EventTypeCount
import com.phonewhisperer.data.repository.EventRepository
import com.phonewhisperer.workers.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the dashboard screen.
 *
 * Exposes reactive state for:
 * - Event counts (behavior, location, app usage, notifications)
 * - Collection status (active/paused)
 * - Last collection timestamp
 * - Event type breakdown
 *
 * Uses StateFlow for Compose integration (collected as State in composables).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    // ── Reactive event counts ───────────────────────────────────────

    val behaviorEventCount: StateFlow<Int> = eventRepository.getBehaviorEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val locationEventCount: StateFlow<Int> = eventRepository.getLocationEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appUsageEventCount: StateFlow<Int> = eventRepository.getAppUsageEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distinctAppCount: StateFlow<Int> = eventRepository.getDistinctAppCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastEventTimestamp: StateFlow<Long?> = eventRepository.getLastBehaviorEventTimestamp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Phase 2: Notification counts ────────────────────────────────

    val notificationEventCount: StateFlow<Int> = eventRepository.getNotificationEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationDistinctAppCount: StateFlow<Int> = eventRepository.getNotificationDistinctAppCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Total events (derived — now includes notifications) ─────────

    val totalEventCount: StateFlow<Int> = combine(
        eventRepository.getBehaviorEventCount(),
        eventRepository.getLocationEventCount(),
        eventRepository.getAppUsageEventCount(),
        eventRepository.getNotificationEventCount()
    ) { behavior, location, usage, notifications -> behavior + location + usage + notifications }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
