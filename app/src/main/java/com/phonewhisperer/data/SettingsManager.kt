package com.phonewhisperer.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("phonewhisperer_prefs", Context.MODE_PRIVATE)

    private val _locationTrackingEnabled = MutableStateFlow(prefs.getBoolean("location_tracking", true))
    val locationTrackingEnabled: StateFlow<Boolean> = _locationTrackingEnabled.asStateFlow()

    private val _usageStatsEnabled = MutableStateFlow(prefs.getBoolean("usage_stats", true))
    val usageStatsEnabled: StateFlow<Boolean> = _usageStatsEnabled.asStateFlow()

    private val _notificationTrackingEnabled = MutableStateFlow(prefs.getBoolean("notification_tracking", true))
    val notificationTrackingEnabled: StateFlow<Boolean> = _notificationTrackingEnabled.asStateFlow()

    private val _screenTrackingEnabled = MutableStateFlow(prefs.getBoolean("screen_tracking", true))
    val screenTrackingEnabled: StateFlow<Boolean> = _screenTrackingEnabled.asStateFlow()

    private val _ringerTrackingEnabled = MutableStateFlow(prefs.getBoolean("ringer_tracking", true))
    val ringerTrackingEnabled: StateFlow<Boolean> = _ringerTrackingEnabled.asStateFlow()

    private val _geofencingEnabled = MutableStateFlow(prefs.getBoolean("geofencing", true))
    val geofencingEnabled: StateFlow<Boolean> = _geofencingEnabled.asStateFlow()

    private val _aiAutoRunEnabled = MutableStateFlow(prefs.getBoolean("ai_autorun", true))
    val aiAutoRunEnabled: StateFlow<Boolean> = _aiAutoRunEnabled.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean("onboarding_complete", false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    fun setLocationTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("location_tracking", enabled).apply()
        _locationTrackingEnabled.value = enabled
    }

    fun setUsageStatsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("usage_stats", enabled).apply()
        _usageStatsEnabled.value = enabled
    }

    fun setNotificationTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notification_tracking", enabled).apply()
        _notificationTrackingEnabled.value = enabled
    }

    fun setScreenTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("screen_tracking", enabled).apply()
        _screenTrackingEnabled.value = enabled
    }

    fun setRingerTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ringer_tracking", enabled).apply()
        _ringerTrackingEnabled.value = enabled
    }

    fun setGeofencingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("geofencing", enabled).apply()
        _geofencingEnabled.value = enabled
    }

    fun setAiAutoRunEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_autorun", enabled).apply()
        _aiAutoRunEnabled.value = enabled
    }

    fun setOnboardingComplete(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", completed).apply()
        _onboardingComplete.value = completed
    }
}
