package com.recordly.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.PreferencesRepository
import com.recordly.app.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

import com.recordly.app.service.RecordingState
import com.recordly.app.service.RecordingService

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
}
