package com.recordly.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.PreferencesRepository
import com.recordly.app.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PreferencesRepository) : ViewModel() {
    val uiState: StateFlow<UserPreferences?> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateResolution(value: String) = viewModelScope.launch { repository.updateResolution(value) }
    fun updateFps(value: Int) = viewModelScope.launch { repository.updateFps(value) }
    fun updateQuality(value: String) = viewModelScope.launch { repository.updateQuality(value) }
    fun updateBitrate(value: String) = viewModelScope.launch { repository.updateBitrate(value) }
    fun updateCountdown(value: Int) = viewModelScope.launch { repository.updateCountdown(value) }
    fun updateAudioSource(value: String) = viewModelScope.launch { repository.updateAudioSource(value) }
    fun updateFloatingControls(value: Boolean) = viewModelScope.launch { repository.updateFloatingControls(value) }
    fun updateTheme(value: String) = viewModelScope.launch { repository.updateTheme(value) }
    fun updateDynamicColor(value: Boolean) = viewModelScope.launch { repository.updateDynamicColor(value) }
    fun updatePerformanceMode(value: Boolean) = viewModelScope.launch { repository.updatePerformanceMode(value) }
    fun completeOnboarding() = viewModelScope.launch { repository.updateOnboardingComplete(true) }
    fun resetOnboarding() = viewModelScope.launch { repository.updateOnboardingComplete(false) }
}
