package com.recordly.app.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun RecordDashboardScreen(viewModel: RecordViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()

    var editingChip by remember { mutableStateOf<String?>(null) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    // Capture display metrics from Activity context NOW (before service starts)
    // This avoids the "Context not associated with display" crash in Service.
    val displayMetrics = remember {
        context.resources.displayMetrics
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val prefs = uiState

            // Get refresh rate safely from Activity context (not Service)
            val refreshRate: Float = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    (context as? Activity)?.display?.refreshRate ?: 60f
                } else {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                        .defaultDisplay.refreshRate
                }
            } catch (e: Exception) {
                60f
            }

            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
                putExtra(RecordingService.EXTRA_RESOLUTION, prefs?.resolution ?: "1080p")
                putExtra(RecordingService.EXTRA_FPS, prefs?.fps ?: 30)
                putExtra(RecordingService.EXTRA_AUDIO_SOURCE, prefs?.audioSource ?: "No audio")
                putExtra(RecordingService.EXTRA_BITRATE, prefs?.bitrate ?: "Auto")
                putExtra(RecordingService.EXTRA_COUNTDOWN, prefs?.countdown ?: 0)
                // Pass display info so Service doesn't need to query it
                putExtra(RecordingService.EXTRA_SCREEN_WIDTH, displayMetrics.widthPixels)
                putExtra(RecordingService.EXTRA_SCREEN_HEIGHT, displayMetrics.heightPixels)
                putExtra(RecordingService.EXTRA_SCREEN_DENSITY, displayMetrics.densityDpi)
                putExtra(RecordingService.EXTRA_SCREEN_REFRESH_RATE, refreshRate)
            }
            ContextCompat.startForegroundService(context, intent)
        } else {
            viewModel.resetState()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled, proceed */ }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.RequestingPermission) {
            // 1. Notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            // 2. Mic permission if needed
            val audio = uiState?.audioSource ?: "No audio"
            if (audio == "Phone microphone" || audio == "Internal audio + microphone") {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            // 3. Overlay permission check — if needed and not granted, redirect to settings
            if (uiState?.floatingControls == true && !Settings.canDrawOverlays(context)) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"))
                    )
                }
                viewModel.resetState()
                return@LaunchedEffect
            }

            // 4. Launch screen capture consent
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Recordly",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Status card
        RecordingStatusCard(recordingState)

        Spacer(modifier = Modifier.height(20.dp))

        // Big action button
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

        val isIdle = recordingState is RecordingState.Idle ||
                recordingState is RecordingState.Saved ||
                recordingState is RecordingState.Error

        if (isIdle) {
            Spacer(modifier = Modifier.height(20.dp))

            // Current preset config card
            ConfigCard(
                uiState = uiState,
                onChipClick = { editingChip = it }
            )

            // Internal audio warning
            val audio = uiState?.audioSource ?: ""
            if (audio.contains("Internal", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Internal audio capture not yet supported. Recording will use no audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Save confirmation
        if (recordingState is RecordingState.Saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording saved to Movies/Recordly",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Quick edit sheets
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
    val (icon, iconTint, title, subtitle) = when (state) {
        is RecordingState.Idle -> Quad(Icons.Default.CheckCircle,
            null, "Ready to record", "Tap Start to begin")
        is RecordingState.Saved -> Quad(Icons.Default.CheckCircle,
            null, "Recording saved", "Tap Start to record again")
        is RecordingState.Recording -> Quad(Icons.Rounded.FiberManualRecord,
            Color.Red, "Recording", "Recording in progress...")
        is RecordingState.Paused -> Quad(Icons.Default.PauseCircle,
            null, "Paused", "Recording is paused")
        is RecordingState.Stopping -> Quad(Icons.Default.Save,
            null, "Saving...", "Please wait")
        is RecordingState.Error -> Quad(Icons.Default.ErrorOutline,
            null, "Recording failed", state.message)
        else -> Quad(Icons.Default.HourglassTop, null, "Starting...", "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is RecordingState.Recording -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                is RecordingState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state is RecordingState.Countdown) {
                Text(
                    "${state.secondsLeft}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Starting in...", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Get ready!", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state is RecordingState.Stopping) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Saving recording...", style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// Helper data class to avoid destructuring issues
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun ActionArea(
    recordingState: RecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isIdle = recordingState is RecordingState.Idle ||
            recordingState is RecordingState.Saved ||
            recordingState is RecordingState.Error
    val isActiveRecording = recordingState is RecordingState.Recording ||
            recordingState is RecordingState.Paused
    val isBusy = recordingState is RecordingState.Stopping ||
            recordingState is RecordingState.Countdown

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
        }

        AnimatedVisibility(visible = isIdle, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FiberManualRecord,
                    contentDescription = "Start Recording",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        AnimatedVisibility(visible = isActiveRecording, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Stop,
                    contentDescription = "Stop Recording",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun ConfigCard(uiState: UserPreferences?, onChipClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Current Preset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onChipClick("resolution") },
                    label = { Text(uiState?.resolution ?: "1080p") },
                    leadingIcon = {
                        Icon(Icons.Default.AspectRatio, null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { onChipClick("fps") },
                    label = { Text("${uiState?.fps ?: 30} FPS") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onChipClick("quality") },
                    label = { Text(uiState?.quality ?: "Balanced") },
                    leadingIcon = {
                        Icon(Icons.Default.HighQuality, null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { onChipClick("audio") },
                    label = {
                        Text(
                            when (uiState?.audioSource) {
                                "Phone microphone" -> "Mic"
                                "No audio" -> "No audio"
                                "Internal audio" -> "Internal"
                                else -> uiState?.audioSource ?: "No audio"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            AssistChip(
                onClick = { onChipClick("countdown") },
                label = {
                    val cd = uiState?.countdown ?: 0
                    Text(if (cd == 0) "No countdown" else "Countdown ${cd}s")
                },
                leadingIcon = {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                }
            )
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
                .padding(bottom = 40.dp)
        ) {
            when (chipType) {
                "resolution" -> {
                    SheetHeader("Resolution")
                    listOf(
                        Triple("720p", "HD — smaller files, less battery drain", true),
                        Triple("1080p", "Full HD — recommended balance", true),
                        Triple("1440p", "QHD — sharp, larger files", true),
                        Triple("Native", "Use device native resolution", true)
                    ).forEach { (label, desc, enabled) ->
                        OptionRow(label, desc, currentPrefs?.resolution == label, enabled) {
                            onSelectResolution(label)
                        }
                    }
                }
                "fps" -> {
                    SheetHeader("Frame Rate")
                    listOf(
                        Triple(30, "Standard — most compatible", true),
                        Triple(60, "Smooth — good for tutorials/demos", true),
                        Triple(90, "High — uses more CPU/battery", true),
                        Triple(120, "Very high — may not work on all devices", true)
                    ).forEach { (value, desc, enabled) ->
                        OptionRow("$value FPS", desc, currentPrefs?.fps == value, enabled) {
                            onSelectFps(value)
                        }
                    }
                }
                "quality" -> {
                    SheetHeader("Quality")
                    listOf(
                        "Low" to "Smallest files, lower detail",
                        "Balanced" to "Good quality, reasonable size",
                        "High" to "Great quality for sharing",
                        "Max" to "Best quality, largest files"
                    ).forEach { (label, desc) ->
                        OptionRow(label, desc, currentPrefs?.quality == label, true) {
                            onSelectQuality(label)
                        }
                    }
                }
                "audio" -> {
                    SheetHeader("Audio Source")
                    OptionRow("No audio", "Record screen only, no sound",
                        currentPrefs?.audioSource == "No audio", true) { onSelectAudio("No audio") }
                    OptionRow("Phone microphone", "Record your voice and ambient sound",
                        currentPrefs?.audioSource == "Phone microphone", true) { onSelectAudio("Phone microphone") }
                    OptionRow(
                        "Internal audio",
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            "App audio — requires Android 10+ (coming soon)"
                        else
                            "Not available on Android 8/9",
                        false, false
                    ) {}
                }
                "countdown" -> {
                    SheetHeader("Countdown")
                    listOf(0, 3, 5, 10).forEach { value ->
                        OptionRow(
                            if (value == 0) "No countdown" else "${value}s",
                            if (value == 0) "Start recording immediately" else "Wait $value seconds before recording",
                            currentPrefs?.countdown == value,
                            true
                        ) { onSelectCountdown(value) }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun OptionRow(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = if (enabled) onClick else null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                if (description.isNotEmpty()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
