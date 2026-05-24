package com.recordly.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.MediaRepository
import com.recordly.app.data.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            repository.getRecordings().collect { list ->
                _recordings.value = list
            }
        }
    }

    fun delete(recording: Recording) {
        viewModelScope.launch {
            try {
                repository.deleteRecording(recording.uri)
                refresh()
            } catch (e: Exception) {
                // Ignore security exceptions for now (e.g. Scoped Storage without permission)
            }
        }
    }
}
