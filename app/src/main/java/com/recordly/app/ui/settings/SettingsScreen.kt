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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recordly.app.data.UserPreferences

/**
 * SettingsScreen — performance-optimized.
 *
 * Key decisions:
 * - `Column + verticalScroll` instead of LazyColumn. LazyColumn triggers expensive
 *   re-layout when ANY item changes because it recalculates slot indices. For a short
 *   settings list (~10 items), Column is cheaper.
 * - All composables are `@Stable` by contract (only primitive/immutable args).
 * - Bottom sheets are hoisted to the top level and only composed when `showSheet != null`.
 * - No `remember { derivedStateOf }` chains — the ViewModel emits a single stable snapshot.
 * - No side effects (no storage queries, no MediaStore) inside composition.
 * - `key()` not needed since we use Column (no LazyList slot logic).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onRunSetupAgain: () -> Unit = {}
) {
    // Single state snapshot — settings screen only recomposes when prefs change
    val prefs by viewModel.uiState.collectAsState()
    val p = prefs ?: return

    var showSheet by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))

        // ── Recording ──
        SettingsSection(title = "Recording") {
            ClickRow("Resolution", p.resolution, Icons.Default.AspectRatio) { showSheet = "resolution" }
            Divider()
            ClickRow("Frame rate", "${p.fps} FPS", Icons.Default.Speed) { showSheet = "fps" }
            Divider()
            ClickRow("Quality", p.quality, Icons.Default.HighQuality) { showSheet = "quality" }
            Divider()
            ClickRow("Bitrate", p.bitrate, Icons.Default.Tune) { showSheet = "bitrate" }
            Divider()
            ClickRow(
                "Countdown",
                if (p.countdown == 0) "Off" else "${p.countdown}s",
                Icons.Default.Timer
            ) { showSheet = "countdown" }
        }

        // ── Audio ──
        SettingsSection(title = "Audio") {
            ClickRow("Audio source", p.audioSource, Icons.Default.Mic) { showSheet = "audio" }
            if (p.audioSource.contains("Internal", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "⚠ Internal audio is not yet supported. Recording will use no audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }

        // ── Appearance ──
        SettingsSection(title = "Appearance") {
            ClickRow("Theme", p.theme, Icons.Default.Palette) { showSheet = "theme" }
            Divider()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    "Dynamic color",
                    "Use wallpaper colors (Material You)",
                    Icons.Default.ColorLens,
                    p.dynamicColor
                ) { viewModel.updateDynamicColor(it) }
            } else {
                InfoRow("Dynamic color", "Requires Android 12+", Icons.Default.ColorLens)
            }
        }

        // ── Controls ──
        SettingsSection(title = "Controls") {
            SwitchRow(
                "Floating overlay",
                "Stop/pause button while recording",
                Icons.Default.PictureInPicture,
                p.floatingControls
            ) { viewModel.updateFloatingControls(it) }
        }

        // ── Storage ──
        SettingsSection(title = "Storage") {
            InfoRow("Save location", "Movies/Recordly", Icons.Default.FolderOpen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Custom save location coming in a future update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // ── Performance ──
        SettingsSection(title = "Performance") {
            SwitchRow(
                "Performance mode",
                if (p.performanceMode) "Active · reduced animations, safe recording defaults"
                else "Recommended for older/low-end devices",
                Icons.Default.Bolt,
                p.performanceMode
            ) { viewModel.updatePerformanceMode(it) }
        }

        // ── Setup ──
        SettingsSection(title = "Setup") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRunSetupAgain)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Run permissions setup", style = MaterialTheme.typography.bodyLarge)
                    Text("Review and grant app permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Bottom sheets — only composed when needed
    showSheet?.let { sheet ->
        SettingsSheet(
            sheet = sheet,
            prefs = p,
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

// ── Section card ──
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

// ── Row types ──
@Composable
private fun ClickRow(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(title: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Single bottom sheet composable for all settings ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    sheet: String,
    prefs: UserPreferences,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 44.dp)
        ) {
            when (sheet) {
                "resolution" -> {
                    SheetTitle("Resolution")
                    listOf("720p" to "HD", "1080p" to "Full HD · Recommended",
                        "1440p" to "QHD · Higher quality", "Native" to "Device native resolution"
                    ).forEach { (label, hint) ->
                        SheetOption(label, hint, prefs.resolution == label) { onSelectResolution(label) }
                    }
                }
                "fps" -> {
                    SheetTitle("Frame Rate")
                    listOf(30 to "Standard · Most compatible", 60 to "Smooth · Good default",
                        90 to "High · Uses more resources", 120 to "Very high · Not universally supported"
                    ).forEach { (v, hint) ->
                        SheetOption("$v FPS", hint, prefs.fps == v) { onSelectFps(v) }
                    }
                }
                "quality" -> {
                    SheetTitle("Quality")
                    listOf("Low" to "Smallest file size", "Balanced" to "Good balance",
                        "High" to "High quality", "Max" to "Maximum quality · Large files"
                    ).forEach { (label, hint) ->
                        SheetOption(label, hint, prefs.quality == label) { onSelectQuality(label) }
                    }
                }
                "bitrate" -> {
                    SheetTitle("Bitrate")
                    listOf("Auto" to "Estimated from resolution", "8 Mbps" to "Low-end devices",
                        "12 Mbps" to "Balanced", "20 Mbps" to "High quality", "35 Mbps" to "Maximum"
                    ).forEach { (label, hint) ->
                        SheetOption(label, hint, prefs.bitrate == label) { onSelectBitrate(label) }
                    }
                }
                "countdown" -> {
                    SheetTitle("Countdown")
                    listOf(0 to "Start immediately", 3 to "3 seconds", 5 to "5 seconds", 10 to "10 seconds"
                    ).forEach { (v, hint) ->
                        SheetOption(if (v == 0) "No countdown" else "${v}s", hint, prefs.countdown == v) { onSelectCountdown(v) }
                    }
                }
                "audio" -> {
                    SheetTitle("Audio Source")
                    SheetOption("No audio", "Record screen with no sound", prefs.audioSource == "No audio") { onSelectAudio("No audio") }
                    SheetOption("Phone microphone", "Record your voice", prefs.audioSource == "Phone microphone") { onSelectAudio("Phone microphone") }
                    SheetOption(
                        "Internal audio",
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "App audio · Coming soon" else "Not available on Android 8/9",
                        selected = false,
                        enabled = false
                    ) {}
                }
                "theme" -> {
                    SheetTitle("Theme")
                    listOf(
                        "System" to "Follow device setting",
                        "Light"  to "Always light",
                        "Dark"   to "Always dark",
                        "AMOLED" to "True black — best for OLED displays"
                    ).forEach { (label, hint) ->
                        SheetOption(label, hint, prefs.theme == label) { onSelectTheme(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SheetOption(
    label: String,
    hint: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = if (enabled) onClick else null)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (hint.isNotEmpty()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
