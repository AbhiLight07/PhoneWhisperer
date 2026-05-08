package com.phonewhisperer.execution

import android.util.Log
import com.phonewhisperer.data.repository.EventRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRuleScheduler @Inject constructor(
    private val eventRepository: EventRepository,
    private val actionExecutor: ActionExecutor
) {
    companion object {
        private const val TAG = "LocationRuleScheduler"
    }

    suspend fun checkAndExecuteLocationRules(locationName: String) {
        try {
            val approvedRules = eventRepository.getApprovedRules().first()
            val locationRules = approvedRules.filter { 
                it.triggerType == "LOCATION" && it.triggerValue.contains(locationName, ignoreCase = true) 
            }

            if (locationRules.isEmpty()) {
                Log.d(TAG, "No automation rules for location: $locationName")
                return
            }

            Log.d(TAG, "Found ${locationRules.size} rules for location: $locationName")
            for (rule in locationRules) {
                val success = actionExecutor.execute(rule)
                if (success) {
                    Log.d(TAG, "✓ Successfully executed location rule: ${rule.name}")
                    // In a real app, we'd log RULE_EXECUTED behavior event here
                } else {
                    Log.w(TAG, "Failed to execute location rule: ${rule.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location rules", e)
        }
    }
}
