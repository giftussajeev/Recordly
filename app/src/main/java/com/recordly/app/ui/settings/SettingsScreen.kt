package com.recordly.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState != null) {
            val prefs = uiState!!
            
            Text("Recording Defaults", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Resolution: ${prefs.resolution}")
            Text("FPS: ${prefs.fps}")
            Text("Quality: ${prefs.quality}")
            Text("Bitrate: ${prefs.bitrate}")
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("Audio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Source: ${prefs.audioSource}")

            Spacer(modifier = Modifier.height(16.dp))

            Text("Floating Controls", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Enabled: ${prefs.floatingOverlayEnabled}")
            Text("Style: ${prefs.overlayStyle}")
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Theme: ${prefs.theme}")
        } else {
            CircularProgressIndicator()
        }
    }
}
