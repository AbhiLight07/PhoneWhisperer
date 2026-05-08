package com.phonewhisperer.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.phonewhisperer.data.collector.AlarmCollector
import com.phonewhisperer.data.collector.AppUsageCollector
import com.phonewhisperer.data.collector.CalendarCollector
import com.phonewhisperer.data.collector.LocationCollector
import com.phonewhisperer.data.SettingsManager
import com.phonewhisperer.data.local.db.dao.LocationEventDao
import com.phonewhisperer.data.repository.EventRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background worker that orchestrates all data collection.
 *
 * Runs every 15 minutes (WorkManager minimum interval) and invokes each
 * collector in sequence: Location → App Usage → Calendar.
 *
 * After collection, checks if sufficient location data exists to trigger
 * geofence setup (Phase 2 enhancement).
 *
 * Architecture notes:
 * - Uses @HiltWorker for constructor injection (requires hilt-work dependency).
 * - CoroutineWorker runs on Dispatchers.Default by default — I/O operations
 *   inside Room DAOs already switch to the IO dispatcher via room-ktx.
 * - Returns Result.success() even if individual collectors fail (partial data
 *   is better than no data). Only returns Result.retry() on catastrophic errors.
 * - Stores last run timestamp in SharedPreferences for deduplication.
 */
@HiltWorker
class DataCollectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationCollector: LocationCollector,
    private val appUsageCollector: AppUsageCollector,
    private val calendarCollector: CalendarCollector,
    private val alarmCollector: AlarmCollector,
    private val eventRepository: EventRepository,
    private val locationEventDao: LocationEventDao,
    private val settingsManager: SettingsManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "DataCollectionWorker"
        const val WORK_NAME = "phonewhisperer_data_collection"
        private const val PREFS_NAME = "phonewhisperer_prefs"
        private const val KEY_LAST_COLLECTION = "last_collection_timestamp"
        private const val KEY_GEOFENCES_SETUP = "geofences_setup_complete"
        private const val KEY_LAST_AI_RUN = "last_ai_run_timestamp"
        private const val DATA_RETENTION_DAYS = 14
        private const val MIN_LOCATIONS_FOR_GEOFENCE = 20
        private const val MIN_EVENTS_FOR_AI = 10        // Minimum unprocessed events to trigger AI
        private const val AI_COOLDOWN_HOURS = 6L        // Hours between AI runs
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "=== Data collection cycle starting ===")
        val startTime = System.currentTimeMillis()

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCollection = prefs.getLong(KEY_LAST_COLLECTION, startTime - 15 * 60 * 1000)

        try {
            // 1. Collect location
            val locationSuccess = try {
                if (settingsManager.locationTrackingEnabled.value) locationCollector.collectCurrentLocation() else false
            } catch (e: Exception) {
                Log.e(TAG, "Location collection failed", e)
                false
            }

            // 2. Collect app usage (since last run)
            val usageCount = try {
                if (settingsManager.usageStatsEnabled.value) appUsageCollector.collectUsageSince(lastCollection) else 0
            } catch (e: Exception) {
                Log.e(TAG, "App usage collection failed", e)
                0
            }

            // 3. Collect calendar events
            val calendarCount = try {
                calendarCollector.collectUpcomingEvents() // Not toggleable individually in settings per requirements, but good practice
            } catch (e: Exception) {
                Log.e(TAG, "Calendar collection failed", e)
                0
            }

            // 3.5 Collect alarms
            val alarmCount = try {
                alarmCollector.collectNextAlarm()
            } catch (e: Exception) {
                Log.e(TAG, "Alarm collection failed", e)
                0
            }

            // 4. Periodic data pruning (once per run, cheap operation)
            try {
                eventRepository.pruneOldData(DATA_RETENTION_DAYS)
            } catch (e: Exception) {
                Log.e(TAG, "Data pruning failed", e)
            }

            // 5. Check if we should set up geofences (Phase 2)
            try {
                val geofencesSetup = prefs.getBoolean(KEY_GEOFENCES_SETUP, false)
                if (!geofencesSetup && settingsManager.geofencingEnabled.value) {
                    val locationCount = locationEventDao.getAllLocationsSnapshot().size
                    if (locationCount >= MIN_LOCATIONS_FOR_GEOFENCE) {
                        Log.d(TAG, "Location count ($locationCount) >= $MIN_LOCATIONS_FOR_GEOFENCE — triggering geofence setup")
                        val geofenceWork = OneTimeWorkRequestBuilder<GeofenceSetupWorker>()
                            .addTag(GeofenceSetupWorker.WORK_NAME)
                            .build()
                        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                            GeofenceSetupWorker.WORK_NAME,
                            ExistingWorkPolicy.KEEP,
                            geofenceWork
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geofence setup check failed", e)
            }

            // 6. Auto-trigger AI Pattern Analysis (Phase 3/6)
            //    Runs if: ≥10 unprocessed events AND ≥6 hours since last AI run
            try {
                val lastAiRun = prefs.getLong(KEY_LAST_AI_RUN, 0L)
                val hoursSinceLastAi = (startTime - lastAiRun) / (60 * 60 * 1000L)
                
                if (hoursSinceLastAi >= AI_COOLDOWN_HOURS && settingsManager.aiAutoRunEnabled.value) {
                    val unprocessedCount = eventRepository.getUnprocessedBehaviorEvents().size
                    if (unprocessedCount >= MIN_EVENTS_FOR_AI) {
                        Log.d(TAG, "AI trigger: $unprocessedCount unprocessed events, ${hoursSinceLastAi}h since last run — launching PatternAnalysisWorker")
                        val aiWork = OneTimeWorkRequestBuilder<PatternAnalysisWorker>()
                            .addTag(PatternAnalysisWorker.WORK_NAME)
                            .build()
                        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                            PatternAnalysisWorker.WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            aiWork
                        )
                        prefs.edit().putLong(KEY_LAST_AI_RUN, startTime).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI trigger check failed", e)
            }

            // Update last collection timestamp
            prefs.edit().putLong(KEY_LAST_COLLECTION, startTime).apply()

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, """
                === Data collection cycle complete ===
                Duration: ${elapsed}ms
                Location: ${if (locationSuccess) "✓" else "✗"}
                App usage: $usageCount new sessions
                Calendar: $calendarCount events
                Alarms: $alarmCount new alarms
            """.trimIndent())

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in data collection worker", e)
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
