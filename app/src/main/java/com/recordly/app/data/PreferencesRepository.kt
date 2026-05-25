package com.recordly.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    private val RESOLUTION = stringPreferencesKey("resolution")
    private val FPS = intPreferencesKey("fps")
    private val QUALITY = stringPreferencesKey("quality")
    private val BITRATE = stringPreferencesKey("bitrate")
    private val COUNTDOWN = intPreferencesKey("countdown")
    private val AUDIO_SOURCE = stringPreferencesKey("audio_source")
    private val FLOATING_CONTROLS = booleanPreferencesKey("floating_controls")
    private val THEME = stringPreferencesKey("theme")
    private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
    private val SAVE_LOCATION_URI = stringPreferencesKey("save_location_uri")
    private val SHOW_TOUCHES = booleanPreferencesKey("show_touches")

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            resolution = prefs[RESOLUTION] ?: "1080p",
            fps = prefs[FPS] ?: 30,
            quality = prefs[QUALITY] ?: "Balanced",
            bitrate = prefs[BITRATE] ?: "Auto",
            countdown = prefs[COUNTDOWN] ?: 0,
            audioSource = prefs[AUDIO_SOURCE] ?: "No audio",
            floatingControls = prefs[FLOATING_CONTROLS] ?: true,
            theme = prefs[THEME] ?: "System",
            dynamicColor = prefs[DYNAMIC_COLOR] ?: false,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false,
            performanceMode = prefs[PERFORMANCE_MODE] ?: false,
            saveLocationUri = prefs[SAVE_LOCATION_URI] ?: "",
            showTouches = prefs[SHOW_TOUCHES] ?: false
        )
    }

    suspend fun updateResolution(value: String) {
        context.dataStore.edit { it[RESOLUTION] = value }
    }
    suspend fun updateFps(value: Int) {
        context.dataStore.edit { it[FPS] = value }
    }
    suspend fun updateQuality(value: String) {
        context.dataStore.edit { it[QUALITY] = value }
    }
    suspend fun updateBitrate(value: String) {
        context.dataStore.edit { it[BITRATE] = value }
    }
    suspend fun updateCountdown(value: Int) {
        context.dataStore.edit { it[COUNTDOWN] = value }
    }
    suspend fun updateAudioSource(value: String) {
        context.dataStore.edit { it[AUDIO_SOURCE] = value }
    }
    suspend fun updateFloatingControls(value: Boolean) {
        context.dataStore.edit { it[FLOATING_CONTROLS] = value }
    }
    suspend fun updateTheme(value: String) {
        context.dataStore.edit { it[THEME] = value }
    }
    suspend fun updateDynamicColor(value: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = value }
    }
    suspend fun updateOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = value }
    }
    suspend fun updatePerformanceMode(value: Boolean) {
        context.dataStore.edit { it[PERFORMANCE_MODE] = value }
    }
    suspend fun updateSaveLocationUri(value: String) {
        context.dataStore.edit { it[SAVE_LOCATION_URI] = value }
    }
    suspend fun updateShowTouches(value: Boolean) {
        context.dataStore.edit { it[SHOW_TOUCHES] = value }
    }
}
