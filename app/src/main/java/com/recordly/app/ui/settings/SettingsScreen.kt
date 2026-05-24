package com.recordly.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState ?: return

    // Track which setting's bottom sheet to show
    var showSheet by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Recording section
        item {
            SettingsGroupCard(title = "Recording") {
                SettingsClickRow("Resolution", prefs.resolution) { showSheet = "resolution" }
                SettingsClickRow("FPS", "${prefs.fps} FPS") { showSheet = "fps" }
                SettingsClickRow("Quality", prefs.quality) { showSheet = "quality" }
                SettingsClickRow("Bitrate", prefs.bitrate) { showSheet = "bitrate" }
                SettingsClickRow("Countdown", if (prefs.countdown == 0) "Off" else "${prefs.countdown}s") { showSheet = "countdown" }
            }
        }

        // Audio section
        item {
            SettingsGroupCard(title = "Audio") {
                SettingsClickRow("Audio Source", prefs.audioSource) { showSheet = "audio" }
            }
        }

        // Controls section
        item {
            SettingsGroupCard(title = "Controls") {
                SettingsSwitchRow("Floating overlay", prefs.floatingControls) {
                    viewModel.updateFloatingControls(it)
                }
            }
        }

        // Storage section
        item {
            SettingsGroupCard(title = "Storage") {
                SettingsInfoRow("Save location", "Movies/Recordly")
            }
        }

        // Appearance section
        item {
            SettingsGroupCard(title = "Appearance") {
                SettingsClickRow("Theme", prefs.theme) { showSheet = "theme" }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitchRow("Dynamic color (Material You)", prefs.dynamicColor) {
                        viewModel.updateDynamicColor(it)
                    }
                } else {
                    SettingsInfoRow("Dynamic color", "Requires Android 12+")
                }
            }
        }

        // Performance section
        item {
            SettingsGroupCard(title = "Performance") {
                SettingsSwitchRow("Performance mode", prefs.performanceMode) {
                    viewModel.updatePerformanceMode(it)
                }
                Text(
                    "Reduces effects and prefers stable recording settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Bottom sheets for each setting
    showSheet?.let { sheet ->
        SettingsBottomSheet(
            sheetType = sheet,
            prefs = prefs,
            onDismiss = { showSheet = null },
            onSelectResolution = { viewModel.updateResolution(it); showSheet = null },
            onSelectFps = { viewModel.updateFps(it); showSheet = null },
            onSelectQuality = { viewModel.updateQuality(it); showSheet = null },
            onSelectBitrate = { viewModel.updateBitrate(it); showSheet = null },
            onSelectCountdown = { viewModel.updateCountdown(it); showSheet = null },
            onSelectAudio = { viewModel.updateAudioSource(it); showSheet = null },
            onSelectTheme = { viewModel.updateTheme(it); showSheet = null }
        )
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsClickRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
fun SettingsBottomSheet(
    sheetType: String,
    prefs: com.recordly.app.data.UserPreferences,
    onDismiss: () -> Unit,
    onSelectResolution: (String) -> Unit,
    onSelectFps: (Int) -> Unit,
    onSelectQuality: (String) -> Unit,
    onSelectBitrate: (String) -> Unit,
    onSelectCountdown: (Int) -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectTheme: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            when (sheetType) {
                "resolution" -> {
                    SheetTitle("Resolution")
                    listOf("720p", "1080p", "1440p", "Native").forEach { opt ->
                        SheetOption(opt, prefs.resolution == opt) { onSelectResolution(opt) }
                    }
                }
                "fps" -> {
                    SheetTitle("Frame Rate")
                    listOf(30, 60, 90, 120).forEach { opt ->
                        SheetOption("$opt FPS", prefs.fps == opt) { onSelectFps(opt) }
                    }
                }
                "quality" -> {
                    SheetTitle("Quality")
                    listOf("Low", "Balanced", "High", "Max").forEach { opt ->
                        SheetOption(opt, prefs.quality == opt) { onSelectQuality(opt) }
                    }
                }
                "bitrate" -> {
                    SheetTitle("Bitrate")
                    listOf("Auto", "8 Mbps", "12 Mbps", "20 Mbps", "35 Mbps").forEach { opt ->
                        SheetOption(opt, prefs.bitrate == opt) { onSelectBitrate(opt) }
                    }
                }
                "countdown" -> {
                    SheetTitle("Countdown")
                    listOf(0, 3, 5, 10).forEach { opt ->
                        SheetOption(
                            if (opt == 0) "Off" else "${opt}s",
                            prefs.countdown == opt
                        ) { onSelectCountdown(opt) }
                    }
                }
                "audio" -> {
                    SheetTitle("Audio Source")
                    listOf("No audio", "Phone microphone").forEach { opt ->
                        SheetOption(opt, prefs.audioSource == opt) { onSelectAudio(opt) }
                    }
                    // Internal audio shown but disabled
                    SheetOption("Internal audio (not yet supported)", false, enabled = false) {}
                }
                "theme" -> {
                    SheetTitle("Theme")
                    listOf("System", "Light", "Dark", "AMOLED").forEach { opt ->
                        SheetOption(opt, prefs.theme == opt) { onSelectTheme(opt) }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SheetOption(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = if (enabled) onClick else null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
