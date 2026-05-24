package com.recordly.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recordly_settings")

class PreferencesRepository(private val context: Context) {
    
    private val dataStore = context.dataStore
    
    companion object {
        val RESOLUTION = stringPreferencesKey("resolution")
        val FPS = intPreferencesKey("fps")
        val QUALITY = stringPreferencesKey("quality")
        val BITRATE = stringPreferencesKey("bitrate")
        val AUDIO_SOURCE = stringPreferencesKey("audio_source")
        val COUNTDOWN = intPreferencesKey("countdown")
        val SHOW_TAPS = booleanPreferencesKey("show_taps")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FLOATING_OVERLAY = booleanPreferencesKey("floating_overlay")
        val OVERLAY_STYLE = stringPreferencesKey("overlay_style")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .map { preferences ->
            UserPreferences(
                resolution = preferences[RESOLUTION] ?: "1080p",
                fps = preferences[FPS] ?: 60,
                quality = preferences[QUALITY] ?: "High",
                bitrate = preferences[BITRATE] ?: "Auto",
                audioSource = preferences[AUDIO_SOURCE] ?: "Internal Audio",
                countdown = preferences[COUNTDOWN] ?: 3,
                showTaps = preferences[SHOW_TAPS] ?: false,
                theme = preferences[THEME] ?: "System",
                dynamicColor = preferences[DYNAMIC_COLOR] ?: true,
                floatingOverlayEnabled = preferences[FLOATING_OVERLAY] ?: true,
                overlayStyle = preferences[OVERLAY_STYLE] ?: "Bubble"
            )
        }

    suspend fun updateFps(fps: Int) {
        dataStore.edit { preferences ->
            preferences[FPS] = fps
        }
    }
    
    // Add other update functions as needed
}

data class UserPreferences(
    val resolution: String,
    val fps: Int,
    val quality: String,
    val bitrate: String,
    val audioSource: String,
    val countdown: Int,
    val showTaps: Boolean,
    val theme: String,
    val dynamicColor: Boolean,
    val floatingOverlayEnabled: Boolean,
    val overlayStyle: String
)
