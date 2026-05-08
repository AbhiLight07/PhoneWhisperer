package com.phonewhisperer.data.repository

import com.phonewhisperer.data.local.db.dao.EventTypeCount
import com.phonewhisperer.data.local.db.entity.AppUsageEvent
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import com.phonewhisperer.data.local.db.entity.LocationEvent
import com.phonewhisperer.data.local.db.entity.NotificationEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all event data access.
 */
interface EventRepository {

    // ── BehaviorEvent ───────────────────────────────────────────────

    suspend fun insertBehaviorEvent(event: BehaviorEvent): Long
    suspend fun insertBehaviorEvents(events: List<BehaviorEvent>): List<Long>
    fun getAllBehaviorEvents(): Flow<List<BehaviorEvent>>
    fun getBehaviorEventsByTimeRange(start: Long, end: Long): Flow<List<BehaviorEvent>>
    fun getBehaviorEventsByType(eventType: String): Flow<List<BehaviorEvent>>
    suspend fun getUnprocessedBehaviorEvents(): List<BehaviorEvent>
    suspend fun markBehaviorEventsProcessed(ids: List<Long>, clusterId: Int? = null)
    fun getBehaviorEventCount(): Flow<Int>
    suspend fun getEventCountsByType(): List<EventTypeCount>
    fun getLastBehaviorEventTimestamp(): Flow<Long?>

    // ── LocationEvent ───────────────────────────────────────────────

    suspend fun insertLocationEvent(event: LocationEvent): Long
    fun getAllLocationEvents(): Flow<List<LocationEvent>>
    fun getLocationEventsByTimeRange(start: Long, end: Long): Flow<List<LocationEvent>>
    suspend fun getUnlabeledLocations(): List<LocationEvent>
    suspend fun updateLocationLabels(ids: List<Long>, label: String)
    fun getLocationEventCount(): Flow<Int>

    // ── AppUsageEvent ───────────────────────────────────────────────

    suspend fun insertAppUsageEvent(event: AppUsageEvent): Long
    fun getAllAppUsageEvents(): Flow<List<AppUsageEvent>>
    fun getAppUsageEventsByTimeRange(start: Long, end: Long): Flow<List<AppUsageEvent>>
    fun getAppUsageEventCount(): Flow<Int>
    fun getDistinctAppCount(): Flow<Int>
    fun getTotalScreenTimeMs(): Flow<Long?>

    // ── NotificationEvent ───────────────────────────────────────────

    suspend fun insertNotificationEvent(event: NotificationEvent): Long
    fun getAllNotificationEvents(): Flow<List<NotificationEvent>>
    fun getNotificationEventsByTimeRange(start: Long, end: Long): Flow<List<NotificationEvent>>
    fun getNotificationEventCount(): Flow<Int>
    fun getNotificationDistinctAppCount(): Flow<Int>

    // ── AI Engine (Patterns & Rules) ────────────────────────────────

    suspend fun insertBehaviorPattern(pattern: BehaviorPatternEntity): Long
    fun getAllBehaviorPatterns(): Flow<List<BehaviorPatternEntity>>

    suspend fun insertAutomationRule(rule: AutomationRuleEntity): Long
    suspend fun updateAutomationRule(rule: AutomationRuleEntity)
    fun getPendingRules(): Flow<List<AutomationRuleEntity>>
    fun getApprovedRules(): Flow<List<AutomationRuleEntity>>
    suspend fun updateRuleStatus(ruleId: Long, status: String)

    // ── Maintenance ─────────────────────────────────────────────────

    suspend fun pruneOldData(retentionDays: Int = 14)
    suspend fun wipeAllData()
}
