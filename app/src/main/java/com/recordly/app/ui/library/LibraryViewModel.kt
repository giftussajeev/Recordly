package com.recordly.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.MediaRepository
import com.recordly.app.data.Recording
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(private val mediaRepository: MediaRepository) : ViewModel() {

    val recordings: StateFlow<List<Recording>> = mediaRepository.getRecordings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteRecording(recording: Recording) {
        // TODO: Implement deletion through MediaRepository
    }
}
