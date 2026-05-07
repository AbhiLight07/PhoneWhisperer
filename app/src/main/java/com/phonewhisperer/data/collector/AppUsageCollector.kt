package com.phonewhisperer.data.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.phonewhisperer.data.local.db.dao.AppUsageEventDao
import com.phonewhisperer.data.local.db.entity.AppUsageEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects app usage data from UsageStatsManager.
 *
 * Design decisions:
 * - Queries UsageEvents (not UsageStats) for granular session-level data.
 * - Aggregates MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND pairs into sessions.
 * - Filters out system packages (launchers, systemui) to focus on user-initiated apps.
 * - Deduplication: checks Room before inserting to avoid duplicates across WorkManager runs.
 *
 * Requires PACKAGE_USAGE_STATS permission (granted via Settings, not runtime dialog).
 */
@Singleton
class AppUsageCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageEventDao: AppUsageEventDao
) {
    companion object {
        private const val TAG = "AppUsageCollector"

        // Minimum session duration to record (filter out accidental opens)
        private const val MIN_SESSION_DURATION_MS = 3_000L  // 3 seconds

        // System packages to exclude from tracking
        private val EXCLUDED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher", // Samsung launcher
            "com.android.settings",
            "com.android.packageinstaller"
        )
    }

    /**
     * Collects app usage events since [sinceTimestamp].
     *
     * @param sinceTimestamp Epoch millis — only events after this time are collected.
     *                       Typically the timestamp of the last WorkManager run.
     * @return Number of new usage events inserted.
     */
    suspend fun collectUsageSince(sinceTimestamp: Long): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager

        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager not available")
            return 0
        }

        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "PACKAGE_USAGE_STATS permission not granted")
            return 0
        }

        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(sinceTimestamp, now)

        // Build sessions by pairing MOVE_TO_FOREGROUND with MOVE_TO_BACKGROUND
        val foregroundTimestamps = mutableMapOf<String, Long>() // packageName -> foreground time
        val sessions = mutableListOf<RawAppSession>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val packageName = event.packageName ?: continue
            if (packageName in EXCLUDED_PACKAGES) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundTimestamps[packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val startTime = foregroundTimestamps.remove(packageName) ?: continue
                    val duration = event.timeStamp - startTime

                    if (duration >= MIN_SESSION_DURATION_MS) {
                        sessions.add(
                            RawAppSession(
                                packageName = packageName,
                                startTimestamp = startTime,
                                durationMs = duration
                            )
                        )
                    }
                }
            }
        }

        // Convert to entities and insert (with deduplication)
        var insertedCount = 0
        for (session in sessions) {
            // Check for existing entry (dedup across WorkManager runs)
            val existingCount = appUsageEventDao.countEventsForPackageInRange(
                session.packageName,
                session.startTimestamp - 1000, // 1s tolerance
                session.startTimestamp + 1000
            )

            if (existingCount > 0) continue

            val calendar = Calendar.getInstance().apply { timeInMillis = session.startTimestamp }
            val appName = resolveAppName(session.packageName)

            val appUsageEvent = AppUsageEvent(
                timestamp = session.startTimestamp,
                packageName = session.packageName,
                appName = appName,
                durationMs = session.durationMs,
                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            )

            appUsageEventDao.insert(appUsageEvent)
            insertedCount++
        }

        Log.d(TAG, "Collected $insertedCount new app usage sessions (${sessions.size} total found)")
        return insertedCount
    }

    /**
     * Resolves a package name to a human-readable app name.
     * Falls back to package name if resolution fails.
     */
    private fun resolveAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Checks if the app has PACKAGE_USAGE_STATS permission.
     * This can't be checked via ContextCompat — requires UsageStatsManager query.
     */
    private fun hasUsageStatsPermission(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return false

        val now = System.currentTimeMillis()
        // If we can query stats for the last minute, we have permission
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 60_000,
            now
        )
        return stats != null && stats.isNotEmpty()
    }

    private data class RawAppSession(
        val packageName: String,
        val startTimestamp: Long,
        val durationMs: Long
    )
}
