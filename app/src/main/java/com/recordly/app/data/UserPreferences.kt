package com.recordly.app.data

data class UserPreferences(
    val resolution: String = "1080p",
    val fps: Int = 30,               // 30 = safest default; -1 = match display refresh rate
    val quality: String = "Balanced", // Maps internally to bitrate: Low=8M, Balanced=12M, High=20M, Max=35M
    val countdown: Int = 0,           // 0 = no countdown, >0 = seconds
    val audioSource: String = "No audio",  // "No audio", "Phone microphone", "Internal audio"
    val floatingControls: Boolean = true,
    val theme: String = "System",     // "System", "Light", "Dark", "AMOLED"
    val dynamicColor: Boolean = false,
    val onboardingComplete: Boolean = false,
    val saveLocationUri: String = ""
)
