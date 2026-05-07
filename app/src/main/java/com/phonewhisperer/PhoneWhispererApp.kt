package com.phonewhisperer

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.phonewhisperer.receiver.RingerModeReceiver
import com.phonewhisperer.receiver.ScreenStateReceiver
import com.phonewhisperer.workers.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for PhoneWhisperer.
 *
 * Responsibilities:
 * 1. Initialize Hilt dependency injection (@HiltAndroidApp)
 * 2. Configure WorkManager with HiltWorkerFactory (enables @HiltWorker injection)
 * 3. Schedule the periodic data collection worker
 * 4. Register dynamic BroadcastReceivers (RingerMode, ScreenState)
 *
 * Implements Configuration.Provider to provide a custom WorkManager configuration
 * that uses Hilt's worker factory for dependency injection into workers.
 */
@HiltAndroidApp
class PhoneWhispererApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Dynamic receivers — hold references for lifecycle management
    private var ringerModeReceiver: RingerModeReceiver? = null
    private var screenStateReceiver: ScreenStateReceiver? = null

    companion object {
        private const val TAG = "PhoneWhispererApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PhoneWhisperer initializing...")

        // Schedule background data collection
        WorkScheduler.scheduleDataCollection(this)

        // Register dynamic broadcast receivers
        registerDynamicReceivers()

        // Initialize ringer mode baseline
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        RingerModeReceiver.previousMode = audioManager.ringerMode

        Log.d(TAG, "PhoneWhisperer initialized — data collection scheduled, receivers registered")
    }

    /**
     * Registers BroadcastReceivers that must be registered dynamically
     * (implicit broadcast restrictions on Android 8+).
     */
    private fun registerDynamicReceivers() {
        // Ringer mode changes (silent/vibrate/normal)
        ringerModeReceiver = RingerModeReceiver()
        val ringerFilter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(ringerModeReceiver, ringerFilter)
        Log.d(TAG, "RingerModeReceiver registered")

        // Screen on/off events
        screenStateReceiver = ScreenStateReceiver()
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenFilter)
        Log.d(TAG, "ScreenStateReceiver registered")
    }

    override fun onTerminate() {
        super.onTerminate()
        // Unregister dynamic receivers
        ringerModeReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        screenStateReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        Log.d(TAG, "Dynamic receivers unregistered")
    }

    /**
     * Provides custom WorkManager configuration with Hilt worker factory.
     * This replaces the default WorkManager initialization.
     *
     * IMPORTANT: Must also add this to AndroidManifest.xml:
     *   <provider android:name="androidx.startup.InitializationProvider" ...
     *       tools:node="remove" />
     * to disable default WorkManager auto-initialization.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}
