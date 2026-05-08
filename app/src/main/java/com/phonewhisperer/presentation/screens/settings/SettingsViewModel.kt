package com.phonewhisperer.presentation.screens.settings

import androidx.lifecycle.ViewModel
import com.phonewhisperer.data.SettingsManager
import com.phonewhisperer.ai_engine.llm.ModelDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val modelDownloadManager: ModelDownloadManager
) : ViewModel() {
    
    val locationEnabled: StateFlow<Boolean> = settingsManager.locationTrackingEnabled
    val usageStatsEnabled: StateFlow<Boolean> = settingsManager.usageStatsEnabled
    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationTrackingEnabled
    val screenTrackingEnabled: StateFlow<Boolean> = settingsManager.screenTrackingEnabled
    val ringerTrackingEnabled: StateFlow<Boolean> = settingsManager.ringerTrackingEnabled
    val geofencingEnabled: StateFlow<Boolean> = settingsManager.geofencingEnabled
    val aiAutoRunEnabled: StateFlow<Boolean> = settingsManager.aiAutoRunEnabled

    fun setLocationEnabled(enabled: Boolean) = settingsManager.setLocationTrackingEnabled(enabled)
    fun setUsageStatsEnabled(enabled: Boolean) = settingsManager.setUsageStatsEnabled(enabled)
    fun setNotificationsEnabled(enabled: Boolean) = settingsManager.setNotificationTrackingEnabled(enabled)
    fun setScreenTrackingEnabled(enabled: Boolean) = settingsManager.setScreenTrackingEnabled(enabled)
    fun setRingerTrackingEnabled(enabled: Boolean) = settingsManager.setRingerTrackingEnabled(enabled)
    fun setGeofencingEnabled(enabled: Boolean) = settingsManager.setGeofencingEnabled(enabled)
    fun setAiAutoRunEnabled(enabled: Boolean) = settingsManager.setAiAutoRunEnabled(enabled)

    // Model Download
    val isModelDownloaded = MutableStateFlow(modelDownloadManager.isModelDownloaded())
    
    fun startModelDownload(): kotlinx.coroutines.flow.Flow<Int>? {
        if (isModelDownloaded.value) return null
        val downloadId = modelDownloadManager.startDownload()
        return if (downloadId != -1L) {
            modelDownloadManager.observeDownloadProgress(downloadId)
        } else null
    }

    fun refreshModelStatus() {
        isModelDownloaded.value = modelDownloadManager.isModelDownloaded()
    }
}
