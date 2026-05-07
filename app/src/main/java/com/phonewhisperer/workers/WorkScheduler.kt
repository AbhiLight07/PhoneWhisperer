package com.phonewhisperer.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules and manages all WorkManager periodic tasks.
 *
 * Called from:
 * 1. PhoneWhispererApp.onCreate() — on fresh app start
 * 2. BootReceiver — after device reboot (WorkManager persists but belt-and-suspenders)
 *
 * Uses ExistingPeriodicWorkPolicy.KEEP to avoid resetting the timer if already scheduled.
 */
object WorkScheduler {

    private const val TAG = "WorkScheduler"

    /**
     * Schedules the periodic data collection worker.
     *
     * @param context Application context
     */
    fun scheduleDataCollection(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // Skip if battery critically low
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DataCollectionWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(DataCollectionWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DataCollectionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Don't restart if already scheduled
            workRequest
        )

        Log.d(TAG, "Data collection work scheduled (15-min interval)")
    }

    /**
     * Cancels all scheduled data collection work.
     * Used when the user pauses observation or revokes permissions.
     */
    fun cancelDataCollection(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(DataCollectionWorker.WORK_NAME)
        Log.d(TAG, "Data collection work cancelled")
    }

    /**
     * Checks if data collection work is currently enqueued/running.
     */
    fun isDataCollectionScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(DataCollectionWorker.WORK_NAME)
            .get()
        return workInfos.any { !it.state.isFinished }
    }
}
