package com.recordly.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.PreferencesRepository
import com.recordly.app.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    val uiState: StateFlow<UserPreferences?> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateFps(fps: Int) {
        viewModelScope.launch {
            preferencesRepository.updateFps(fps)
        }
    }
}
