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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.recordly.app.data.UserPreferences
import com.recordly.app.service.RecordingService
import com.recordly.app.service.RecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDashboardScreen(
    viewModel: RecordViewModel,
    onOpenSetup: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()

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
                putExtra(RecordingService.EXTRA_RESOLUTION, uiState?.resolution)
                putExtra(RecordingService.EXTRA_FPS, uiState?.fps)
                putExtra(RecordingService.EXTRA_AUDIO_SOURCE, uiState?.audioSource)
                putExtra(RecordingService.EXTRA_BITRATE, uiState?.bitrate)
                putExtra(RecordingService.EXTRA_COUNTDOWN, uiState?.countdown)
            }
            ContextCompat.startForegroundService(context, intent)
        } else {
            viewModel.resetState()
        }
    }

    // Permission Launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }
    
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.RequestingPermission) {
            // First ask for notification if Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            
            // Then ask for Mic if needed
            if (uiState?.audioSource == "Phone microphone" || uiState?.audioSource == "Internal audio + microphone") {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            
            // Ask for Overlay if needed
            if (uiState?.floatingControls == true && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                // We'll pause requesting state here. Usually requires user to return manually.
                viewModel.resetState()
            } else {
                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Recordly", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
                },
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
                Spacer(modifier = Modifier.height(16.dp))
                RecordingStatusCard(recordingState)
            }

            item {
                ActionArea(
                    recordingState = recordingState,
                    onStart = { viewModel.requestPermission() },
                    onStop = { 
                        val intent = Intent(context, RecordingService::class.java).apply { action = RecordingService.ACTION_STOP }
                        context.startService(intent)
                    }
                )
            }

            if (recordingState is RecordingState.Idle || recordingState is RecordingState.Saved) {
                item {
                    ConfigCard(uiState, onOpenSetup)
                }

                item {
                    PermissionsStatusCard(context, uiState)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RecordingStatusCard(state: RecordingState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is RecordingState.Idle, is RecordingState.Saved -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Ready",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ready to record", style = MaterialTheme.typography.titleLarge)
                    Text("Press Start to begin", style = MaterialTheme.typography.bodyMedium)
                }
                is RecordingState.Countdown -> {
                    Text(
                        text = "${state.secondsLeft}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Starting soon...", style = MaterialTheme.typography.bodyMedium)
                }
                is RecordingState.Recording -> {
                    Icon(
                        imageVector = Icons.Rounded.FiberManualRecord,
                        contentDescription = "Recording",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recording in progress", style = MaterialTheme.typography.titleLarge)
                }
                is RecordingState.Paused -> {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = "Paused",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recording paused", style = MaterialTheme.typography.titleLarge)
                }
                is RecordingState.Stopping -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saving...", style = MaterialTheme.typography.titleLarge)
                }
                is RecordingState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recording failed", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    CircularProgressIndicator()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        val isIdle = recordingState is RecordingState.Idle || recordingState is RecordingState.Saved || recordingState is RecordingState.Error
        val isRecording = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused

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
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = "Start Recording",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        AnimatedVisibility(visible = isRecording, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop Recording",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun ConfigCard(uiState: UserPreferences?, onOpenSetup: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onOpenSetup) {
                    Text("Edit")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(onClick = {}, label = { Text(uiState?.resolution ?: "1080p") })
                SuggestionChip(onClick = {}, label = { Text("${uiState?.fps ?: 60} FPS") })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(onClick = {}, label = { Text(uiState?.quality ?: "High") })
                SuggestionChip(onClick = {}, label = { Text(uiState?.audioSource ?: "No audio") })
            }
        }
    }
}

@Composable
fun PermissionsStatusCard(context: Context, uiState: UserPreferences?) {
    val micNeeded = uiState?.audioSource == "Phone microphone" || uiState?.audioSource == "Internal audio + microphone"
    val overlayNeeded = uiState?.floatingControls == true
    val notifNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (notifNeeded) {
                PermissionRow("Notifications", "Allows background recording status")
            }
            if (micNeeded) {
                PermissionRow("Microphone", "Allows recording external audio")
            }
            if (overlayNeeded) {
                PermissionRow("Display over other apps", "Allows floating control widget")
            }
            PermissionRow("Screen Cast", "Asked automatically when you press start")
        }
    }
}

@Composable
fun PermissionRow(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
