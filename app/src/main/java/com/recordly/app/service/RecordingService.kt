package com.recordly.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.recordly.app.MainActivity
import com.recordly.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var overlayManager: FloatingOverlayManager? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    companion object {
        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        const val ACTION_PAUSE = "ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME = "ACTION_RESUME_RECORDING"
        const val ACTION_TOGGLE_MIC = "ACTION_TOGGLE_MIC"
        
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        
        const val EXTRA_RESOLUTION = "EXTRA_RESOLUTION"
        const val EXTRA_FPS = "EXTRA_FPS"
        const val EXTRA_AUDIO_SOURCE = "EXTRA_AUDIO_SOURCE"
        const val EXTRA_BITRATE = "EXTRA_BITRATE"
        const val EXTRA_COUNTDOWN = "EXTRA_COUNTDOWN"

        private const val CHANNEL_ID = "recordly_recording_channel"
        private const val NOTIFICATION_ID = 112
        private const val TAG = "RecordingService"
        
        fun requestPermissionState() {
            _recordingState.value = RecordingState.RequestingPermission
        }
        fun resetToIdle() {
            _recordingState.value = RecordingState.Idle
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayManager = FloatingOverlayManager(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                
                val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"
                val fps = intent.getIntExtra(EXTRA_FPS, 60)
                val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: "No audio"
                val bitrate = intent.getStringExtra(EXTRA_BITRATE) ?: "Auto"
                val countdown = intent.getIntExtra(EXTRA_COUNTDOWN, 3)

                if (resultCode != 0 && resultData != null) {
                    startCountdownAndRecord(resultCode, resultData, resolution, fps, audioSource, bitrate, countdown)
                } else {
                    stopRecordingSafely(isError = true, message = "Invalid permission data")
                }
            }
            ACTION_STOP -> {
                stopRecordingSafely(isError = false, message = null)
            }
            ACTION_PAUSE -> {
                pauseRecording()
            }
            ACTION_RESUME -> {
                resumeRecording()
            }
            ACTION_TOGGLE_MIC -> toggleMic()
        }
        return START_NOT_STICKY
    }
    
    private fun startCountdownAndRecord(
        resultCode: Int, 
        resultData: Intent, 
        resolution: String, 
        fps: Int, 
        audioSource: String,
        bitrateConfig: String,
        countdown: Int
    ) {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            for (i in countdown downTo 1) {
                _recordingState.value = RecordingState.Countdown(i)
                delay(1000)
            }
            startRecording(resultCode, resultData, resolution, fps, audioSource, bitrateConfig)
        }
    }

    private fun startRecording(
        resultCode: Int, 
        resultData: Intent, 
        resolution: String, 
        fps: Int, 
        audioSource: String,
        bitrateConfig: String
    ) {
        if (_recordingState.value is RecordingState.Recording) return

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (mediaProjection == null) {
                stopRecordingSafely(true, "Failed to acquire MediaProjection.")
                return
            }

            setupMediaRecorder(resolution, fps, audioSource, bitrateConfig)
            
            val metrics = resources.displayMetrics
            val screenWidth = getResolutionWidth(resolution, metrics)
            val screenHeight = getResolutionHeight(resolution, metrics)
            val screenDensity = metrics.densityDpi

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "RecordlyDisplay",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            _recordingState.value = RecordingState.Recording
            
            overlayManager?.showOverlay(
                isRecording = true,
                onRecordToggle = { 
                    if (_recordingState.value is RecordingState.Recording) {
                        stopRecordingSafely(false, null)
                    } else {
                        // handled by UI flow usually
                    }
                },
                onMicToggle = { toggleMic() }
            )

            Log.d(TAG, "Recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            stopRecordingSafely(true, e.localizedMessage ?: "Unknown error starting recording")
        }
    }

    private fun setupMediaRecorder(resolution: String, fps: Int, audioSource: String, bitrateConfig: String) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        when (audioSource) {
            "Phone microphone", "External microphone" -> {
                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            "Internal audio" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
                }
            }
            "Internal audio + microphone" -> {
                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            }
        }

        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val metrics = resources.displayMetrics
        val width = getResolutionWidth(resolution, metrics)
        val height = getResolutionHeight(resolution, metrics)
        
        mediaRecorder?.setVideoSize(width, height)
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        
        if (audioSource != "No audio" && audioSource != "Internal audio" || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setAudioEncodingBitRate(320000)
            mediaRecorder?.setAudioSamplingRate(44100)
        }

        val bitrate = when (bitrateConfig) {
            "8 Mbps" -> 8000000
            "12 Mbps" -> 12000000
            "20 Mbps" -> 20000000
            "35 Mbps" -> 35000000
            else -> width * height * fps / 10
        }
        mediaRecorder?.setVideoEncodingBitRate(bitrate)
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayFps = windowManager.defaultDisplay.refreshRate
        val targetFps = if (fps > displayFps.toInt()) {
            displayFps.toInt()
        } else {
            fps
        }
        mediaRecorder?.setVideoFrameRate(targetFps)

        val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Recordly")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(outputDir, "Recordly_$timestamp.mp4")
        
        mediaRecorder?.setOutputFile(outputFile.absolutePath)
        mediaRecorder?.prepare()
    }
    
    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordingState.value is RecordingState.Recording) {
            try {
                mediaRecorder?.pause()
                _recordingState.value = RecordingState.Paused
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(isPaused = true))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause", e)
            }
        }
    }
    
    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordingState.value is RecordingState.Paused) {
            try {
                mediaRecorder?.resume()
                _recordingState.value = RecordingState.Recording
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(isPaused = false))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume", e)
            }
        }
    }
    
    private fun toggleMic() {
        // Future mic mute implementation
    }

    private fun getResolutionWidth(resolution: String, metrics: DisplayMetrics): Int {
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val isPortrait = screenHeight > screenWidth
        val width = if (isPortrait) screenWidth else screenHeight

        return when (resolution) {
            "720p" -> 720
            "1080p" -> 1080
            "1440p" -> 1440
            else -> width
        }
    }

    private fun getResolutionHeight(resolution: String, metrics: DisplayMetrics): Int {
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val isPortrait = screenHeight > screenWidth
        val width = if (isPortrait) screenWidth else screenHeight
        val height = if (isPortrait) screenHeight else screenWidth
        val aspectRatio = height.toFloat() / width.toFloat()

        val resWidth = getResolutionWidth(resolution, metrics)
        return (resWidth * aspectRatio).toInt()
    }

    private fun stopRecordingSafely(isError: Boolean, message: String?) {
        _recordingState.value = RecordingState.Stopping
        overlayManager?.hideOverlay()
        
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
        }
        
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media recorder", e)
        }
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        if (isError && message != null) {
            _recordingState.value = RecordingState.Error(message)
        } else {
            _recordingState.value = RecordingState.Saved
        }
        
        serviceScope.launch {
            delay(1500)
            if (_recordingState.value is RecordingState.Saved || _recordingState.value is RecordingState.Error) {
                _recordingState.value = RecordingState.Idle
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val pauseResumeAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPaused) {
                val resumeIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME }
                val resumePending = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Resume", resumePending).build()
            } else {
                val pauseIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE }
                val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pausePending).build()
            }
        } else null

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(this, 3, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Recording Paused" else "Recording Screen")
            .setContentText("Tap to open app")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, // Generic icon
                "Stop", 
                stopPendingIntent
            ).build())
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            
        pauseResumeAction?.let { builder.addAction(it) }

        return builder.build()
    }
}
