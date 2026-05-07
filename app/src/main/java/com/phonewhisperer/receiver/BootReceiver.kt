package com.phonewhisperer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.phonewhisperer.workers.WorkScheduler

/**
 * Re-schedules WorkManager jobs after device reboot.
 *
 * WorkManager does persist jobs across reboots natively, but this receiver
 * ensures re-scheduling even in edge cases (e.g., forced stop, OEM-specific
 * background restrictions on Chinese ROMs like MIUI/ColorOS).
 *
 * Registered in AndroidManifest.xml with RECEIVE_BOOT_COMPLETED permission.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed — re-scheduling data collection")
            WorkScheduler.scheduleDataCollection(context)
        }
    }
}
