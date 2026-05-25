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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Which preset card was tapped — drives bottom sheet
    var editingChip by remember { mutableStateOf<String?>(null) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    // Capture display metrics from Activity context (NEVER from Service)
    val displayMetrics = remember { context.resources.displayMetrics }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val prefs = uiState
            val refreshRate: Float = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    (context as? Activity)?.display?.refreshRate ?: 60f
                } else {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                        .defaultDisplay.refreshRate
                }
            } catch (_: Exception) { 60f }

            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
                putExtra(RecordingService.EXTRA_RESOLUTION, prefs?.resolution ?: "1080p")
                putExtra(RecordingService.EXTRA_FPS, prefs?.fps ?: 30)
                putExtra(RecordingService.EXTRA_AUDIO_SOURCE, prefs?.audioSource ?: "No audio")
                putExtra(RecordingService.EXTRA_BITRATE, prefs?.bitrate ?: "Auto")
                putExtra(RecordingService.EXTRA_COUNTDOWN, prefs?.countdown ?: 0)
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
    ) { /* proceed */ }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed */ }

    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.RequestingPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            val audio = uiState?.audioSource ?: "No audio"
            if (audio == "Phone microphone") {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            if (uiState?.floatingControls == true && !Settings.canDrawOverlays(context)) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"))
                    )
                } catch (_: Exception) {}
                viewModel.resetState()
                return@LaunchedEffect
            }
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    val scrollState = rememberScrollState()
    val isIdle = recordingState is RecordingState.Idle
            || recordingState is RecordingState.Saved
            || recordingState is RecordingState.Error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Recordly",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Screen Recorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (recordingState is RecordingState.Recording) {
                RecordingBadge()
            }
        }

        // ── Status card ──
        Spacer(modifier = Modifier.height(12.dp))
        DashboardStatusCard(recordingState, modifier = Modifier.padding(horizontal = 16.dp))

        // ── Record button ──
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RecordButton(
                recordingState = recordingState,
                onStart = { viewModel.requestPermission() },
                onStop = {
                    context.startService(
                        Intent(context, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_STOP
                        }
                    )
                }
            )
        }

        // ── Countdown pill ──
        if (uiState != null && isIdle) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val cd = uiState!!.countdown
                SuggestionChip(
                    onClick = { editingChip = "countdown" },
                    label = {
                        Text(
                            if (cd == 0) "No countdown" else "Countdown: ${cd}s",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    icon = { Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        // ── Preset 2×2 grid ──
        if (isIdle && uiState != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Recording Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            PresetGrid(
                prefs = uiState!!,
                onEdit = { editingChip = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Error / saved feedback ──
        AnimatedVisibility(
            visible = recordingState is RecordingState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (recordingState is RecordingState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Recording failed", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                (recordingState as RecordingState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = recordingState is RecordingState.Saved,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved to Movies/Recordly", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // Internal audio warning
        val audioSrc = uiState?.audioSource ?: ""
        if (audioSrc.contains("Internal", ignoreCase = true)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Internal audio not yet supported. Recording will use no audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Quick-edit bottom sheets
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

// ── Recording badge (shown in header while recording) ──
@Composable
private fun RecordingBadge() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
            Text("REC", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Status card ──
@Composable
private fun DashboardStatusCard(state: RecordingState, modifier: Modifier = Modifier) {
    val (bgColor, icon, title, subtitle) = when (state) {
        is RecordingState.Idle            -> StatusAppearance(null, Icons.Default.RadioButtonChecked, "Ready", "Tap the button below to start")
        is RecordingState.Saved           -> StatusAppearance(null, Icons.Default.CheckCircle, "Saved", "Recording saved to Movies/Recordly")
        is RecordingState.Recording       -> StatusAppearance(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), Icons.Rounded.FiberManualRecord, "Recording", "Recording in progress")
        is RecordingState.Paused          -> StatusAppearance(null, Icons.Default.PauseCircle, "Paused", "Recording is paused")
        is RecordingState.Stopping        -> StatusAppearance(null, Icons.Default.Save, "Saving", "Please wait...")
        is RecordingState.Error           -> StatusAppearance(MaterialTheme.colorScheme.errorContainer, Icons.Default.ErrorOutline, "Failed", state.message)
        is RecordingState.RequestingPermission -> StatusAppearance(null, Icons.Default.HourglassTop, "Starting", "Requesting permission...")
        else                              -> StatusAppearance(null, Icons.Default.HourglassTop, "Starting", "")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (state is RecordingState.Countdown) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.secondsLeft}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Starting in...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Get ready!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (state is RecordingState.Stopping) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Text("Saving recording...", style = MaterialTheme.typography.titleSmall)
            }
        } else {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (state is RecordingState.Recording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2)
                    }
                }
            }
        }
    }
}

private data class StatusAppearance(
    val bgColor: Color?,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

// ── Large record/stop button ──
@Composable
private fun RecordButton(
    recordingState: RecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isIdle = recordingState is RecordingState.Idle
            || recordingState is RecordingState.Saved
            || recordingState is RecordingState.Error
    val isRecording = recordingState is RecordingState.Recording
            || recordingState is RecordingState.Paused
    val isBusy = recordingState is RecordingState.Stopping
            || recordingState is RecordingState.RequestingPermission

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 0.92f else 1f,
        animationSpec = tween(200),
        label = "button_scale"
    )

    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    color = when {
                        isRecording -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        isBusy      -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        else        -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    },
                    shape = CircleShape
                )
        )

        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = isIdle,
            enter = scaleIn(tween(200)) + fadeIn(),
            exit = scaleOut(tween(150)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FiberManualRecord,
                    contentDescription = "Start Recording",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isRecording,
            enter = scaleIn(tween(200)) + fadeIn(),
            exit = scaleOut(tween(150)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(buttonScale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Stop,
                    contentDescription = "Stop Recording",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

// ── 2×2 Preset grid ──
@Composable
private fun PresetGrid(
    prefs: UserPreferences,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PresetCard(
                icon = Icons.Outlined.AspectRatio,
                label = "Resolution",
                value = prefs.resolution,
                onClick = { onEdit("resolution") },
                modifier = Modifier.weight(1f)
            )
            PresetCard(
                icon = Icons.Outlined.Speed,
                label = "Frame Rate",
                value = "${prefs.fps} FPS",
                onClick = { onEdit("fps") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PresetCard(
                icon = Icons.Outlined.HighQuality,
                label = "Quality",
                value = prefs.quality,
                onClick = { onEdit("quality") },
                modifier = Modifier.weight(1f)
            )
            PresetCard(
                icon = Icons.Outlined.Mic,
                label = "Audio",
                value = when (prefs.audioSource) {
                    "No audio"         -> "None"
                    "Phone microphone" -> "Microphone"
                    "Internal audio"   -> "Internal"
                    else               -> prefs.audioSource
                },
                onClick = { onEdit("audio") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresetCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Quick-edit bottom sheet ──
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 44.dp)
        ) {
            when (chipType) {
                "resolution" -> {
                    SheetHeader("Resolution")
                    listOf(
                        "720p"   to "HD · Smaller files, less battery drain",
                        "1080p"  to "Full HD · Recommended balance",
                        "1440p"  to "QHD · Sharp, larger files",
                        "Native" to "Device native · Exact screen size"
                    ).forEach { (label, desc) ->
                        SheetOptionRow(label, desc, currentPrefs?.resolution == label) {
                            onSelectResolution(label)
                        }
                    }
                }
                "fps" -> {
                    SheetHeader("Frame Rate")
                    listOf(
                        30  to "Standard · Compatible with all devices",
                        60  to "Smooth · Great for demos and tutorials",
                        90  to "High · Uses more CPU/battery",
                        120 to "Very high · May not work on all devices"
                    ).forEach { (value, desc) ->
                        SheetOptionRow("$value FPS", desc, currentPrefs?.fps == value) {
                            onSelectFps(value)
                        }
                    }
                }
                "quality" -> {
                    SheetHeader("Quality")
                    listOf(
                        "Low"      to "Smallest files, lower detail",
                        "Balanced" to "Good quality, moderate file size",
                        "High"     to "Great quality for sharing",
                        "Max"      to "Best quality, largest files"
                    ).forEach { (label, desc) ->
                        SheetOptionRow(label, desc, currentPrefs?.quality == label) {
                            onSelectQuality(label)
                        }
                    }
                }
                "audio" -> {
                    SheetHeader("Audio Source")
                    SheetOptionRow("No audio", "Record screen only",
                        currentPrefs?.audioSource == "No audio") { onSelectAudio("No audio") }
                    SheetOptionRow("Phone microphone", "Record your voice",
                        currentPrefs?.audioSource == "Phone microphone") { onSelectAudio("Phone microphone") }
                    SheetOptionRow(
                        "Internal audio",
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            "App audio · Coming soon" else "Not available on Android 8/9",
                        selected = false,
                        enabled = false
                    ) {}
                }
                "countdown" -> {
                    SheetHeader("Countdown")
                    listOf(
                        0  to "Start immediately",
                        3  to "3 seconds — good default",
                        5  to "5 seconds",
                        10 to "10 seconds"
                    ).forEach { (value, desc) ->
                        SheetOptionRow(
                            if (value == 0) "No countdown" else "${value}s",
                            desc,
                            currentPrefs?.countdown == value
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
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun SheetOptionRow(
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                else     -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
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
                if (description.isNotEmpty()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
