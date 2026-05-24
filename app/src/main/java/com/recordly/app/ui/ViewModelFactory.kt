package com.recordly.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recordly.app.di.AppContainer

import com.recordly.app.ui.dashboard.RecordViewModel
import com.recordly.app.ui.library.LibraryViewModel
import com.recordly.app.ui.settings.SettingsViewModel

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordViewModel(appContainer.preferencesRepository) as T
        }
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(appContainer.mediaRepository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(appContainer.preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
