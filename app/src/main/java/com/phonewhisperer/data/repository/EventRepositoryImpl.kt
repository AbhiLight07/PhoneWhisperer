package com.phonewhisperer.data.repository

import com.phonewhisperer.data.local.db.dao.AppUsageEventDao
import com.phonewhisperer.data.local.db.dao.AutomationRuleDao
import com.phonewhisperer.data.local.db.dao.BehaviorEventDao
import com.phonewhisperer.data.local.db.dao.BehaviorPatternDao
import com.phonewhisperer.data.local.db.dao.EventTypeCount
import com.phonewhisperer.data.local.db.dao.LocationEventDao
import com.phonewhisperer.data.local.db.dao.NotificationEventDao
import com.phonewhisperer.data.local.db.entity.AppUsageEvent
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import com.phonewhisperer.data.local.db.entity.LocationEvent
import com.phonewhisperer.data.local.db.entity.NotificationEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [EventRepository].
 */
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val database: com.phonewhisperer.data.local.db.AppDatabase,
    private val behaviorEventDao: BehaviorEventDao,
    private val locationEventDao: LocationEventDao,
    private val appUsageEventDao: AppUsageEventDao,
    private val notificationEventDao: NotificationEventDao,
    private val behaviorPatternDao: BehaviorPatternDao,
    private val automationRuleDao: AutomationRuleDao
) : EventRepository {

    // ── BehaviorEvent ───────────────────────────────────────────────

    override suspend fun insertBehaviorEvent(event: BehaviorEvent): Long =
        behaviorEventDao.insert(event)

    override suspend fun insertBehaviorEvents(events: List<BehaviorEvent>): List<Long> =
        behaviorEventDao.insertAll(events)

    override fun getAllBehaviorEvents(): Flow<List<BehaviorEvent>> =
        behaviorEventDao.getAllEvents()

    override fun getBehaviorEventsByTimeRange(start: Long, end: Long): Flow<List<BehaviorEvent>> =
        behaviorEventDao.getEventsByTimeRange(start, end)

    override fun getBehaviorEventsByType(eventType: String): Flow<List<BehaviorEvent>> =
        behaviorEventDao.getEventsByType(eventType)

    override suspend fun getUnprocessedBehaviorEvents(): List<BehaviorEvent> =
        behaviorEventDao.getUnprocessedEvents()

    override suspend fun markBehaviorEventsProcessed(ids: List<Long>, clusterId: Int?) {
        if (clusterId != null) {
            behaviorEventDao.markAsProcessed(ids, clusterId)
        } else {
            behaviorEventDao.markAsProcessed(ids)
        }
    }

    override fun getBehaviorEventCount(): Flow<Int> =
        behaviorEventDao.getTotalEventCount()

    override suspend fun getEventCountsByType(): List<EventTypeCount> =
        behaviorEventDao.getEventCountsByType()

    override fun getLastBehaviorEventTimestamp(): Flow<Long?> =
        behaviorEventDao.getLastEventTimestamp()

    // ── LocationEvent ───────────────────────────────────────────────

    override suspend fun insertLocationEvent(event: LocationEvent): Long =
        locationEventDao.insert(event)

    override fun getAllLocationEvents(): Flow<List<LocationEvent>> =
        locationEventDao.getAllEvents()

    override fun getLocationEventsByTimeRange(start: Long, end: Long): Flow<List<LocationEvent>> =
        locationEventDao.getEventsByTimeRange(start, end)

    override suspend fun getUnlabeledLocations(): List<LocationEvent> =
        locationEventDao.getUnlabeledLocations()

    override suspend fun updateLocationLabels(ids: List<Long>, label: String) =
        locationEventDao.updateLabels(ids, label)

    override fun getLocationEventCount(): Flow<Int> =
        locationEventDao.getTotalLocationCount()

    // ── AppUsageEvent ───────────────────────────────────────────────

    override suspend fun insertAppUsageEvent(event: AppUsageEvent): Long =
        appUsageEventDao.insert(event)

    override fun getAllAppUsageEvents(): Flow<List<AppUsageEvent>> =
        appUsageEventDao.getAllEvents()

    override fun getAppUsageEventsByTimeRange(start: Long, end: Long): Flow<List<AppUsageEvent>> =
        appUsageEventDao.getEventsByTimeRange(start, end)

    override fun getAppUsageEventCount(): Flow<Int> =
        appUsageEventDao.getTotalUsageCount()

    override fun getDistinctAppCount(): Flow<Int> =
        appUsageEventDao.getDistinctAppCount()

    override fun getTotalScreenTimeMs(): Flow<Long?> =
        appUsageEventDao.getTotalScreenTimeMs()

    // ── NotificationEvent ───────────────────────────────────────────

    override suspend fun insertNotificationEvent(event: NotificationEvent): Long =
        notificationEventDao.insert(event)

    override fun getAllNotificationEvents(): Flow<List<NotificationEvent>> =
        notificationEventDao.getAllEvents()

    override fun getNotificationEventsByTimeRange(start: Long, end: Long): Flow<List<NotificationEvent>> =
        notificationEventDao.getEventsByTimeRange(start, end)

    override fun getNotificationEventCount(): Flow<Int> =
        notificationEventDao.getTotalCount()

    override fun getNotificationDistinctAppCount(): Flow<Int> =
        notificationEventDao.getDistinctAppCount()

    // ── AI Engine (Patterns & Rules) ────────────────────────────────

    override suspend fun insertBehaviorPattern(pattern: BehaviorPatternEntity): Long =
        behaviorPatternDao.insert(pattern)

    override fun getAllBehaviorPatterns(): Flow<List<BehaviorPatternEntity>> =
        behaviorPatternDao.getAllPatterns()

    override suspend fun insertAutomationRule(rule: AutomationRuleEntity): Long =
        automationRuleDao.insert(rule)

    override suspend fun updateAutomationRule(rule: AutomationRuleEntity) =
        automationRuleDao.update(rule)

    override fun getPendingRules(): Flow<List<AutomationRuleEntity>> =
        automationRuleDao.getRulesByStatus(AutomationRuleEntity.STATUS_PENDING)

    override fun getApprovedRules(): Flow<List<AutomationRuleEntity>> =
        automationRuleDao.getRulesByStatus(AutomationRuleEntity.STATUS_APPROVED)

    override suspend fun updateRuleStatus(ruleId: Long, status: String) =
        automationRuleDao.updateStatus(ruleId, status)

    // ── Maintenance ─────────────────────────────────────────────────

    override suspend fun pruneOldData(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        val behaviorDeleted = behaviorEventDao.deleteProcessedOlderThan(cutoff)
        val locationDeleted = locationEventDao.deleteOlderThan(cutoff)
        val usageDeleted = appUsageEventDao.deleteOlderThan(cutoff)
        val notificationDeleted = notificationEventDao.deleteOlderThan(cutoff)

        android.util.Log.d(
            "EventRepository",
            "Pruned old data: $behaviorDeleted behavior, $locationDeleted location, $usageDeleted usage, $notificationDeleted notification events"
        )
    }

    override suspend fun wipeAllData() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            database.clearAllTables()
            android.util.Log.d("EventRepository", "Wiped all AI memory (cleared all tables)")
        }
    }
}
