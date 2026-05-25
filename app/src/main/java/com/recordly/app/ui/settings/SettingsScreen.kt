package com.recordly.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onRunSetupAgain: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState ?: return

    var showSheet by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        // Recording section
        SettingsGroupCard(title = "Recording") {
            SettingsClickRow("Resolution", prefs.resolution) { showSheet = "resolution" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsClickRow("Frame rate", "${prefs.fps} FPS") { showSheet = "fps" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsClickRow("Quality", prefs.quality) { showSheet = "quality" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsClickRow("Bitrate", prefs.bitrate) { showSheet = "bitrate" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsClickRow("Countdown", if (prefs.countdown == 0) "Off" else "${prefs.countdown}s") {
                showSheet = "countdown"
            }
        }

        // Audio section
        SettingsGroupCard(title = "Audio") {
            SettingsClickRow("Audio source", prefs.audioSource) { showSheet = "audio" }
            if (prefs.audioSource.contains("Internal", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Internal audio requires Android 10+ (coming soon). Use microphone or no audio for now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        // Controls section
        SettingsGroupCard(title = "Controls") {
            SettingsSwitchRow("Floating overlay controls", prefs.floatingControls) {
                viewModel.updateFloatingControls(it)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Shows a draggable stop button while recording.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Storage section
        SettingsGroupCard(title = "Storage") {
            SettingsInfoRow("Save location", "Movies/Recordly")
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Recordings are saved to your device's Movies/Recordly folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Appearance section
        SettingsGroupCard(title = "Appearance") {
            SettingsClickRow("Theme", prefs.theme) { showSheet = "theme" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSwitchRow("Dynamic color (Material You)", prefs.dynamicColor) {
                    viewModel.updateDynamicColor(it)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Uses your wallpaper colors for the app theme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } else {
                SettingsInfoRow("Dynamic color", "Requires Android 12+")
            }
        }

        // Performance section
        SettingsGroupCard(title = "Performance") {
            SettingsSwitchRow("Performance mode", prefs.performanceMode) {
                viewModel.updatePerformanceMode(it)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                if (prefs.performanceMode)
                    "Active: using safer defaults, reduced effects, and stable recording settings."
                else
                    "Reduces animations and uses stable recording settings to improve reliability on older devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Setup section
        SettingsGroupCard(title = "Setup") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRunSetupAgain)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Run permissions setup again", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Review and grant app permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
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
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
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
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                .padding(bottom = 40.dp)
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
                            if (opt == 0) "Off — start immediately" else "${opt}s",
                            prefs.countdown == opt
                        ) { onSelectCountdown(opt) }
                    }
                }
                "audio" -> {
                    SheetTitle("Audio Source")
                    SheetOption("No audio", prefs.audioSource == "No audio") { onSelectAudio("No audio") }
                    SheetOption("Phone microphone", prefs.audioSource == "Phone microphone") { onSelectAudio("Phone microphone") }
                    SheetOption("Internal audio (coming soon)", false, enabled = false) {}
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
