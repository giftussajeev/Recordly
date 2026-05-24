package com.recordly.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recordly.app.data.MediaRepository
import com.recordly.app.data.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _allRecordings = MutableStateFlow<List<Recording>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Combine recordings with search query for filtering
        viewModelScope.launch {
            combine(_allRecordings, _searchQuery) { recordings, query ->
                if (query.isBlank()) recordings
                else recordings.filter { it.name.contains(query, ignoreCase = true) }
            }.collect { filtered ->
                _recordings.value = filtered
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getRecordings().collect { list ->
                _allRecordings.value = list
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun delete(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteRecording(recording.uri)
                refresh()
            } catch (_: Exception) {
                // Scoped storage may deny deletion
            }
        }
    }
}
