package com.phonewhisperer.presentation.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Activity Timeline screen.
 * Exposes the most recent behavior events as a reactive flow.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    eventRepository: EventRepository
) : ViewModel() {

    val recentEvents: StateFlow<List<BehaviorEvent>> = eventRepository.getAllBehaviorEvents()
        .map { events -> events.take(100) } // Limit to 100 most recent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
