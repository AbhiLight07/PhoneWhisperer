package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.phonewhisperer.di.RepositoryEntryPoint
import com.phonewhisperer.execution.TimeRuleScheduler
import com.phonewhisperer.workers.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-schedules WorkManager jobs and rule alarms after device reboot.
 *
 * Phase 1: Re-schedules data collection worker.
 * Phase 4: Also rebuilds all time-based rule alarms from the database.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timeRuleScheduler: TimeRuleScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Device boot completed — re-scheduling everything")

        // Phase 1: Re-schedule data collection worker
        WorkScheduler.scheduleDataCollection(context)

        // Phase 4: Rebuild all time-based rule alarms
        val pendingResult = goAsync()
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    RepositoryEntryPoint::class.java
                )
                val repository = entryPoint.eventRepository()

                val approvedRules = repository.getApprovedRules().first()
                val timeRules = approvedRules.filter { it.triggerType == "TIME" }

                if (timeRules.isNotEmpty()) {
                    timeRuleScheduler.scheduleAllRules(timeRules)
                    Log.d(TAG, "Re-scheduled ${timeRules.size} time-based alarms after reboot")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-schedule alarms after reboot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
