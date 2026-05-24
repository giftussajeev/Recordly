package com.recordly.app.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.recordly.app.data.UserPreferences
import com.recordly.app.service.RecordingService
import com.recordly.app.service.RecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDashboardScreen(
    viewModel: RecordViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()

    // Bottom sheet state for quick editing
    var editingChip by remember { mutableStateOf<String?>(null) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
                putExtra(RecordingService.EXTRA_RESOLUTION, uiState?.resolution ?: "1080p")
                putExtra(RecordingService.EXTRA_FPS, uiState?.fps ?: 30)
                putExtra(RecordingService.EXTRA_AUDIO_SOURCE, uiState?.audioSource ?: "No audio")
                putExtra(RecordingService.EXTRA_BITRATE, uiState?.bitrate ?: "Auto")
                putExtra(RecordingService.EXTRA_COUNTDOWN, uiState?.countdown ?: 3)
            }
            ContextCompat.startForegroundService(context, intent)
        } else {
            viewModel.resetState()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.RequestingPermission) {
            // Request notification permission first on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            // Request mic if needed
            val audio = uiState?.audioSource ?: "No audio"
            if (audio == "Phone microphone" || audio == "Internal audio + microphone") {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            // Check overlay
            if (uiState?.floatingControls == true && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                viewModel.resetState()
            } else {
                // Launch screen capture consent
                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top bar area
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Recordly",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Status card
        RecordingStatusCard(recordingState)

        Spacer(modifier = Modifier.height(16.dp))

        // Action button
        ActionArea(
            recordingState = recordingState,
            onStart = { viewModel.requestPermission() },
            onStop = {
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                context.startService(intent)
            }
        )

        // Show config only when not recording
        val isIdle = recordingState is RecordingState.Idle ||
                recordingState is RecordingState.Saved ||
                recordingState is RecordingState.Error

        if (isIdle) {
            Spacer(modifier = Modifier.height(16.dp))

            // Current Preset with interactive chips
            ConfigCard(
                uiState = uiState,
                onChipClick = { chipType -> editingChip = chipType }
            )

            // Warning banner if audio is set to internal
            if (uiState?.audioSource == "Internal audio" || uiState?.audioSource == "Internal audio + microphone") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Internal audio is not yet supported. Recording will use no audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Snackbar-like message for saved/error
        if (recordingState is RecordingState.Saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording saved to Movies/Recordly", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // Quick edit bottom sheets
    editingChip?.let { chip ->
        QuickEditSheet(
            chipType = chip,
            currentPrefs = uiState,
            onDismiss = { editingChip = null },
            onSelectResolution = { viewModel.updateResolution(it); editingChip = null },
            onSelectFps = { viewModel.updateFps(it); editingChip = null },
            onSelectQuality = { viewModel.updateQuality(it); editingChip = null },
            onSelectAudio = { viewModel.updateAudioSource(it); editingChip = null },
            onSelectCountdown = { viewModel.updateCountdown(it); editingChip = null }
        )
    }
}

@Composable
fun RecordingStatusCard(state: RecordingState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is RecordingState.Idle, is RecordingState.Saved -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Ready",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ready to record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Press Start to begin", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is RecordingState.Countdown -> {
                    Text(
                        text = "${state.secondsLeft}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Starting soon...", style = MaterialTheme.typography.bodyMedium)
                }
                is RecordingState.Recording -> {
                    Icon(Icons.Rounded.FiberManualRecord, contentDescription = "Recording",
                        tint = Color.Red, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Recording in progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                is RecordingState.Paused -> {
                    Icon(Icons.Default.PauseCircle, contentDescription = "Paused",
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Recording paused", style = MaterialTheme.typography.titleMedium)
                }
                is RecordingState.Stopping -> {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saving...", style = MaterialTheme.typography.titleMedium)
                }
                is RecordingState.Error -> {
                    Icon(Icons.Default.Error, contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Recording failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(state.message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun ActionArea(
    recordingState: RecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isIdle = recordingState is RecordingState.Idle ||
            recordingState is RecordingState.Saved ||
            recordingState is RecordingState.Error
    val isRecording = recordingState is RecordingState.Recording ||
            recordingState is RecordingState.Paused

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isIdle, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = "Start Recording",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        AnimatedVisibility(visible = isRecording, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop Recording",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
fun ConfigCard(uiState: UserPreferences?, onChipClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Current Preset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onChipClick("resolution") },
                    label = { Text(uiState?.resolution ?: "1080p") },
                    leadingIcon = { Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { onChipClick("fps") },
                    label = { Text("${uiState?.fps ?: 30} FPS") },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onChipClick("quality") },
                    label = { Text(uiState?.quality ?: "High") },
                    leadingIcon = { Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { onChipClick("audio") },
                    label = { Text(uiState?.audioSource ?: "No audio") },
                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditSheet(
    chipType: String,
    currentPrefs: UserPreferences?,
    onDismiss: () -> Unit,
    onSelectResolution: (String) -> Unit,
    onSelectFps: (Int) -> Unit,
    onSelectQuality: (String) -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectCountdown: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            when (chipType) {
                "resolution" -> {
                    Text("Resolution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    val options = listOf("720p", "1080p", "1440p", "Native")
                    options.forEach { opt ->
                        OptionRow(
                            label = opt,
                            description = when (opt) {
                                "720p" -> "HD — smaller files, less battery"
                                "1080p" -> "Full HD — good balance"
                                "1440p" -> "QHD — high detail, larger files"
                                "Native" -> "Device native resolution"
                                else -> ""
                            },
                            selected = currentPrefs?.resolution == opt,
                            onClick = { onSelectResolution(opt) }
                        )
                    }
                }
                "fps" -> {
                    Text("Frame Rate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    val options = listOf(30, 60, 90, 120)
                    options.forEach { opt ->
                        OptionRow(
                            label = "$opt FPS",
                            description = when (opt) {
                                30 -> "Standard — most stable"
                                60 -> "Smooth — good for demos"
                                90 -> "High — uses more battery"
                                120 -> "Very high — may not work on all devices"
                                else -> ""
                            },
                            selected = currentPrefs?.fps == opt,
                            onClick = { onSelectFps(opt) }
                        )
                    }
                }
                "quality" -> {
                    Text("Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    val options = listOf("Low", "Balanced", "High", "Max")
                    options.forEach { opt ->
                        OptionRow(
                            label = opt,
                            description = when (opt) {
                                "Low" -> "Smallest files, lower quality"
                                "Balanced" -> "Good quality, reasonable size"
                                "High" -> "Great quality for sharing"
                                "Max" -> "Best quality, largest files"
                                else -> ""
                            },
                            selected = currentPrefs?.quality == opt,
                            onClick = { onSelectQuality(opt) }
                        )
                    }
                }
                "audio" -> {
                    Text("Audio Source", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    val options = listOf("No audio", "Phone microphone", "Internal audio")
                    options.forEach { opt ->
                        OptionRow(
                            label = opt,
                            description = when (opt) {
                                "No audio" -> "Record screen only, no sound"
                                "Phone microphone" -> "Records your voice and surroundings"
                                "Internal audio" -> "Not yet supported — will fall back to no audio"
                                else -> ""
                            },
                            selected = currentPrefs?.audioSource == opt,
                            onClick = { onSelectAudio(opt) },
                            enabled = opt != "Internal audio"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = if (enabled) onClick else null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                if (description.isNotEmpty()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
                }
            }
        }
    }
}
