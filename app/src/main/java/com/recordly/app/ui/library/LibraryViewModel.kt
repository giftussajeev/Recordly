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
import kotlinx.coroutines.flow.first
import com.recordly.app.data.PreferencesRepository

class LibraryViewModel(
    private val repository: MediaRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _allRecordings = MutableStateFlow<List<Recording>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRecordingIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRecordingIds: StateFlow<Set<Long>> = _selectedRecordingIds.asStateFlow()

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
            val prefs = preferencesRepository.userPreferencesFlow.first()
            repository.getRecordings(prefs.saveLocationUri).collect { list ->
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

    fun toggleSelection(id: Long) {
        _selectedRecordingIds.value = _selectedRecordingIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun clearSelection() {
        _selectedRecordingIds.value = emptySet()
    }

    fun deleteSelected() {
        val selected = _selectedRecordingIds.value
        val itemsToDelete = _allRecordings.value.filter { it.id in selected }
        viewModelScope.launch(Dispatchers.IO) {
            itemsToDelete.forEach {
                try {
                    repository.deleteRecording(it.uri)
                } catch (_: Exception) {}
            }
            _selectedRecordingIds.value = emptySet()
            refresh()
        }
    }
}
