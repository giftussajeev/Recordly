package com.recordly.app.ui.dashboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.recordly.app.data.UserPreferences
import com.recordly.app.service.RecordingService

@Composable
fun RecordDashboardScreen(viewModel: RecordViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    val startMediaProjection = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
                
                uiState?.let { prefs ->
                    putExtra(RecordingService.EXTRA_RESOLUTION, prefs.resolution)
                    putExtra(RecordingService.EXTRA_FPS, prefs.fps)
                    putExtra(RecordingService.EXTRA_AUDIO_SOURCE, prefs.audioSource)
                    putExtra(RecordingService.EXTRA_BITRATE, prefs.bitrate)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState != null) {
            val prefs = uiState!!
            Text(
                text = "${prefs.resolution} • ${prefs.fps} FPS • ${prefs.quality} Quality • ${prefs.audioSource}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
                },
                shape = CircleShape,
                modifier = Modifier.size(120.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start", style = MaterialTheme.typography.titleLarge)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val intent = Intent(context, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_STOP
                    }
                    context.startService(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Stop Recording")
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
