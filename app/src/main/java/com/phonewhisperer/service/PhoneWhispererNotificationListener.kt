package com.phonewhisperer.service

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.data.local.db.entity.NotificationEvent
import com.phonewhisperer.data.repository.EventRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * System-level notification listener that tracks all notifications across the device.
 *
 * Receives callbacks when ANY notification is posted or removed. This is the richest
 * passive signal for understanding user behavior patterns:
 *   - Which apps notify the user and when
 *   - Which notifications are dismissed vs. opened
 *   - Notification frequency patterns by time of day
 *
 * IMPORTANT: Requires user to manually enable in:
 *   Settings > Special app access > Notification access
 *
 * Privacy: We store only app name, category, and priority — NOT notification content/title.
 */
@AndroidEntryPoint
class PhoneWhispererNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var eventRepository: EventRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationListener"

        // System packages to filter out (low-value noise)
        private val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.providers.downloads",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.phonewhisperer",
            "com.phonewhisperer.debug"
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected — actively monitoring")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (shouldFilter(sbn)) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        val appName = resolveAppName(sbn.packageName)
        val category = sbn.notification?.category
        val priority = sbn.notification?.priority ?: 0
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        serviceScope.launch {
            try {
                // 1. Insert into dedicated notification table
                val notifEvent = NotificationEvent(
                    timestamp = now,
                    packageName = sbn.packageName,
                    appName = appName,
                    category = category,
                    priority = priority,
                    action = NotificationEvent.ACTION_POSTED,
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hourOfDay
                )
                eventRepository.insertNotificationEvent(notifEvent)

                // 2. Also create a BehaviorEvent for the unified DBSCAN pipeline
                val behaviorEvent = BehaviorEvent(
                    timestamp = now,
                    eventType = BehaviorEvent.TYPE_NOTIFICATION,
                    payload = """{"packageName":"${sbn.packageName}","appName":"$appName","category":"${category ?: "unknown"}","action":"POSTED"}""",
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hourOfDay
                )
                eventRepository.insertBehaviorEvent(behaviorEvent)

                Log.d(TAG, "Notification POSTED: $appName (${sbn.packageName})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record notification posted", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        sbn ?: return
        if (shouldFilter(sbn)) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        val appName = resolveAppName(sbn.packageName)
        // reason == REASON_CLICK means user tapped, otherwise it was dismissed/cancelled
        val action = if (reason == REASON_CLICK) {
            NotificationEvent.ACTION_CLICKED
        } else {
            NotificationEvent.ACTION_DISMISSED
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toIsoDayOfWeek()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        serviceScope.launch {
            try {
                val notifEvent = NotificationEvent(
                    timestamp = now,
                    packageName = sbn.packageName,
                    appName = appName,
                    category = sbn.notification?.category,
                    priority = sbn.notification?.priority ?: 0,
                    action = action,
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hourOfDay
                )
                eventRepository.insertNotificationEvent(notifEvent)

                Log.d(TAG, "Notification $action: $appName (${sbn.packageName})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record notification removed", e)
            }
        }
    }

    /**
     * Filter out system/low-value notifications to reduce noise.
     */
    private fun shouldFilter(sbn: StatusBarNotification): Boolean {
        // Filter system packages
        if (sbn.packageName in SYSTEM_PACKAGES) return true
        // Filter ongoing notifications (media players, download progress, etc.)
        if (sbn.isOngoing) return true
        return false
    }

    /**
     * Resolves package name to human-readable app name.
     */
    private fun resolveAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        }
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (Sunday=1) to ISO-8601 (Monday=1, Sunday=7).
     */
    private fun Int.toIsoDayOfWeek(): Int = when (this) {
        Calendar.SUNDAY -> 7
        else -> this - 1
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Notification listener service destroyed")
    }
}
