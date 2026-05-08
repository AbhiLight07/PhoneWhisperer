package com.phonewhisperer.execution

import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class BlockedApp(
    val packageName: String,
    val startHour: Int?,
    val endHour: Int?
)

/**
 * Manages the list of apps whose notifications are currently being suppressed
 * by the AI based on user routines.
 */
@Singleton
class NotificationBlockManager @Inject constructor() {

    // Thread-safe set of blocked apps
    private val blockedApps = ConcurrentHashMap.newKeySet<BlockedApp>()

    /**
     * Adds an app to the block list for a specific time window.
     */
    fun addBlock(packageName: String, startHour: Int?, endHour: Int?) {
        // Remove existing blocks for this package to prevent duplicates
        removeBlock(packageName)
        blockedApps.add(BlockedApp(packageName, startHour, endHour))
    }

    /**
     * Removes an app from the block list.
     */
    fun removeBlock(packageName: String) {
        val iterator = blockedApps.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().packageName == packageName) {
                iterator.remove()
            }
        }
    }

    /**
     * Checks if a notification from the given package should be blocked right now.
     */
    fun isBlocked(packageName: String): Boolean {
        val block = blockedApps.find { it.packageName == packageName } ?: return false

        // If no time window is specified, it's always blocked
        if (block.startHour == null || block.endHour == null) {
            return true
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Handle standard window (e.g., 9 to 17)
        if (block.startHour <= block.endHour) {
            return currentHour in block.startHour until block.endHour
        }

        // Handle overnight window (e.g., 22 to 6)
        return currentHour >= block.startHour || currentHour < block.endHour
    }
}
