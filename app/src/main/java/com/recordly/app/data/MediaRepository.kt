package com.recordly.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MediaRepository(private val context: Context) {
    
    // Query MediaStore for videos in Movies/Recordly
    fun getRecordings(): Flow<List<Recording>> = flow {
        // Implementation for querying MediaStore
        emit(emptyList())
    }
}

data class Recording(
    val id: Long,
    val uri: String,
    val title: String,
    val date: Long,
    val duration: Long,
    val size: Long,
    val resolution: String,
    val fps: Int
)
