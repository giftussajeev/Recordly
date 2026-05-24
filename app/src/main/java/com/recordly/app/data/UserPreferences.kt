package com.recordly.app.data

data class UserPreferences(
    val resolution: String = "1080p",
    val fps: Int = 60,
    val quality: String = "High",
    val bitrate: String = "Auto",
    val countdown: Int = 3,
    val audioSource: String = "No audio",
    val floatingControls: Boolean = true
)
