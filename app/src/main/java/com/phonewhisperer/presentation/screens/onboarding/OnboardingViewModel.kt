package com.phonewhisperer.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import com.phonewhisperer.data.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    fun completeOnboarding() {
        settingsManager.setOnboardingComplete(true)
    }
}
