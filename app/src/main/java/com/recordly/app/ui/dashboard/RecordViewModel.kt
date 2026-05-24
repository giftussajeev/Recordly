package com.recordly.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.PreferencesRepository
import com.recordly.app.data.UserPreferences
import com.recordly.app.service.RecordingService
import com.recordly.app.service.RecordingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    val uiState: StateFlow<UserPreferences?> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val recordingState: StateFlow<RecordingState> = RecordingService.recordingState

    fun requestPermission() {
        RecordingService.requestPermissionState()
    }

    fun resetState() {
        RecordingService.resetToIdle()
    }

    fun updateResolution(value: String) = viewModelScope.launch {
        preferencesRepository.updateResolution(value)
    }

    fun updateFps(value: Int) = viewModelScope.launch {
        preferencesRepository.updateFps(value)
    }

    fun updateQuality(value: String) = viewModelScope.launch {
        preferencesRepository.updateQuality(value)
    }

    fun updateBitrate(value: String) = viewModelScope.launch {
        preferencesRepository.updateBitrate(value)
    }

    fun updateAudioSource(value: String) = viewModelScope.launch {
        preferencesRepository.updateAudioSource(value)
    }

    fun updateCountdown(value: Int) = viewModelScope.launch {
        preferencesRepository.updateCountdown(value)
    }
}
