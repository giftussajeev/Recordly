package com.recordly.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.PreferencesRepository
import com.recordly.app.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RecordViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    val uiState: StateFlow<UserPreferences?> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun startRecording(resultCode: Int, resultData: android.content.Intent) {
        // This will be called from the UI when MediaProjection consent is granted
    }
}
