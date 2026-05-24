package com.recordly.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.recordly.app.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsGroupCard(title = "Recording Defaults") {
                    SettingsDropdownRow("Resolution", prefs.resolution, listOf("720p", "1080p", "1440p", "Native")) { viewModel.updateResolution(it) }
                    SettingsDropdownRow("FPS", "${prefs.fps} FPS", listOf("30 FPS", "60 FPS", "90 FPS", "120 FPS")) { viewModel.updateFps(it.split(" ")[0].toInt()) }
                    SettingsDropdownRow("Quality", prefs.quality, listOf("Low", "Balanced", "High", "Max")) { viewModel.updateQuality(it) }
                    SettingsDropdownRow("Bitrate", prefs.bitrate, listOf("Auto", "8 Mbps", "12 Mbps", "20 Mbps", "35 Mbps")) { viewModel.updateBitrate(it) }
                    SettingsDropdownRow("Countdown", "${prefs.countdown}s", listOf("Off", "3s", "5s", "10s")) { 
                        val v = if (it == "Off") 0 else it.replace("s", "").toInt()
                        viewModel.updateCountdown(v) 
                    }
                    SettingsSwitchRow("Show taps during recording", false) {}
                    SettingsSwitchRow("Slow-motion friendly export", false) {}
                }
            }

            item {
                SettingsGroupCard(title = "Audio") {
                    SettingsDropdownRow("Audio Source", prefs.audioSource, listOf("No audio", "Phone microphone", "Internal audio", "Internal audio + microphone")) { viewModel.updateAudioSource(it) }
                    SettingsSwitchRow("Microphone noise reduction", true) {}
                    SettingsDropdownRow("Audio Quality", "High (320kbps)", listOf("Low", "Medium", "High (320kbps)")) {}
                }
            }

            item {
                SettingsGroupCard(title = "Floating Controls") {
                    SettingsSwitchRow("Enable floating overlay", prefs.floatingControls) { viewModel.updateFloatingControls(it) }
                    SettingsDropdownRow("Overlay style", "Pill", listOf("Bubble", "Pill", "Sidebar")) {}
                    SettingsSwitchRow("Confirm before stopping", true) {}
                }
            }
            
            item {
                SettingsGroupCard(title = "Storage") {
                    SettingsRow("Save Location", "Movies/Recordly", onClick = {})
                    SettingsDropdownRow("Filename format", "Recordly_YYYYMMDD_HHMMSS", listOf("Recordly_YYYYMMDD_HHMMSS", "Recording_Date_Time")) {}
                }
            }

            item {
                SettingsGroupCard(title = "Appearance") {
                    SettingsDropdownRow("Theme", "System", listOf("System", "Light", "Dark", "Absolute Dark AMOLED")) {}
                    SettingsSwitchRow("Dynamic color (Material You)", true) {}
                }
            }

            item {
                SettingsGroupCard(title = "Performance") {
                    SettingsSwitchRow("Low-end device mode", false) {}
                    SettingsRow("Screen Refresh Rate", "Detected: 120Hz", onClick = {})
                    SettingsRow("Encoder Compatibility", "Check now", onClick = {})
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownRow(title: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(160.dp)
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
