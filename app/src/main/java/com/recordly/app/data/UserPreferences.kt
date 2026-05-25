package com.recordly.app.data

data class UserPreferences(
    val resolution: String = "1080p",
    val fps: Int = 30,               // default 30 — safest, most compatible
    val quality: String = "Balanced",
    val bitrate: String = "Auto",
    val countdown: Int = 0,          // default no countdown — immediate start
    val audioSource: String = "No audio",  // default no audio — avoids mic permission flow on first run
    val floatingControls: Boolean = true,
    val theme: String = "System",
    val dynamicColor: Boolean = false,   // off by default — use stable Recordly palette
    val onboardingComplete: Boolean = false,
    val performanceMode: Boolean = false
)
