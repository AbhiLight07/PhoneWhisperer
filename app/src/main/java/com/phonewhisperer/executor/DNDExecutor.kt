package com.phonewhisperer.executor

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes Do Not Disturb and audio profile automations.
 *
 * Phase 4 implementation. Requires ACCESS_NOTIFICATION_POLICY permission
 * (requested at runtime when user approves a DND rule).
 *
 * TODO: Phase 4 — implement full DND/Audio automation
 */
@Singleton
class DNDExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DNDExecutor"
    }

    fun enableDND() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            Log.d(TAG, "DND enabled")
        } else {
            Log.w(TAG, "Notification policy access not granted")
        }
    }

    fun disableDND() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Log.d(TAG, "DND disabled")
        }
    }

    fun setRingerMode(mode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = mode // RINGER_MODE_SILENT, VIBRATE, NORMAL
        Log.d(TAG, "Ringer mode set to $mode")
    }
}
