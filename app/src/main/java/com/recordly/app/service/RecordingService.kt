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
import kotlinx.coroutines.SupervisorJob
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
    private var hasAudioSource = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var countdownJob: Job? = null

    companion object {
        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        const val ACTION_PAUSE = "ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME = "ACTION_RESUME_RECORDING"

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
                @Suppress("DEPRECATION")
                val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

                val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"
                val fps = intent.getIntExtra(EXTRA_FPS, 30)
                val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: "No audio"
                val bitrate = intent.getStringExtra(EXTRA_BITRATE) ?: "Auto"
                val countdown = intent.getIntExtra(EXTRA_COUNTDOWN, 3)

                Log.d(TAG, "ACTION_START: res=$resolution fps=$fps audio=$audioSource bitrate=$bitrate countdown=$countdown")

                if (resultCode != 0 && resultData != null) {
                    startCountdownAndRecord(resultCode, resultData, resolution, fps, audioSource, bitrate, countdown)
                } else {
                    Log.e(TAG, "Invalid permission data: resultCode=$resultCode resultData=$resultData")
                    stopRecordingSafely(isError = true, message = "Screen capture permission was denied.")
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP received")
                stopRecordingSafely(isError = false, message = null)
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
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
        if (countdown <= 0) {
            serviceScope.launch {
                startRecording(resultCode, resultData, resolution, fps, audioSource, bitrateConfig)
            }
            return
        }
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
        if (_recordingState.value is RecordingState.Recording) {
            Log.w(TAG, "Already recording, ignoring start request")
            return
        }

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (mediaProjection == null) {
                Log.e(TAG, "Failed to acquire MediaProjection")
                stopRecordingSafely(true, "Failed to acquire screen capture permission.")
                return
            }
            Log.d(TAG, "MediaProjection acquired successfully")

            // Resolve the actual audio source to use
            val resolvedAudio = resolveAudioSource(audioSource)
            Log.d(TAG, "Resolved audio source: '$audioSource' -> '$resolvedAudio'")

            setupMediaRecorder(resolution, fps, resolvedAudio, bitrateConfig)

            val metrics = resources.displayMetrics
            val screenWidth = getResolutionWidth(resolution, metrics)
            val screenHeight = getResolutionHeight(resolution, metrics)
            val screenDensity = metrics.densityDpi

            Log.d(TAG, "Creating VirtualDisplay: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi")

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
            Log.d(TAG, "Recording started successfully")

            overlayManager?.showOverlay(
                isRecording = true,
                onRecordToggle = {
                    if (_recordingState.value is RecordingState.Recording ||
                        _recordingState.value is RecordingState.Paused) {
                        stopRecordingSafely(false, null)
                    }
                },
                onMicToggle = { /* Future mic mute */ }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            stopRecordingSafely(true, e.localizedMessage ?: "Unknown error starting recording")
        }
    }

    /**
     * Resolves the user-selected audio source to a safe, supported value.
     * Internal audio is NOT supported via simple MediaRecorder — it requires
     * AudioPlaybackCapture API which is complex to implement.
     * We fall back to "No audio" for unsupported sources.
     */
    private fun resolveAudioSource(audioSource: String): String {
        return when (audioSource) {
            "No audio" -> "No audio"
            "Phone microphone", "External microphone" -> "Phone microphone"
            "Internal audio" -> {
                // Internal audio via AudioPlaybackCapture is not yet implemented
                Log.w(TAG, "Internal audio capture not yet supported, falling back to No audio")
                "No audio"
            }
            "Internal audio + microphone" -> {
                // Fall back to mic only
                Log.w(TAG, "Internal+mic not yet supported, falling back to Phone microphone")
                "Phone microphone"
            }
            else -> {
                Log.w(TAG, "Unknown audio source '$audioSource', falling back to No audio")
                "No audio"
            }
        }
    }

    private fun setupMediaRecorder(resolution: String, fps: Int, audioSource: String, bitrateConfig: String) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val metrics = resources.displayMetrics
        val width = getResolutionWidth(resolution, metrics)
        val height = getResolutionHeight(resolution, metrics)

        // IMPORTANT: Sources must be set BEFORE output format
        // Audio source must come before video source
        hasAudioSource = false
        if (audioSource == "Phone microphone") {
            try {
                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                hasAudioSource = true
                Log.d(TAG, "Audio source set: MIC")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set audio source MIC, recording without audio", e)
                hasAudioSource = false
            }
        }
        // "No audio" = don't call setAudioSource at all

        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        // Video settings
        mediaRecorder?.setVideoSize(width, height)
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        val bitrate = when (bitrateConfig) {
            "8 Mbps" -> 8_000_000
            "12 Mbps" -> 12_000_000
            "20 Mbps" -> 20_000_000
            "35 Mbps" -> 35_000_000
            else -> (width * height * fps / 10).coerceIn(2_000_000, 50_000_000)
        }
        mediaRecorder?.setVideoEncodingBitRate(bitrate)
        Log.d(TAG, "Video bitrate: $bitrate")

        @Suppress("DEPRECATION")
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayFps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
        val targetFps = fps.coerceAtMost(displayFps.toInt()).coerceAtLeast(15)
        mediaRecorder?.setVideoFrameRate(targetFps)
        Log.d(TAG, "Video FPS: $targetFps (requested: $fps, display: $displayFps)")

        // Audio encoder — only if audio source was set
        if (hasAudioSource) {
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setAudioEncodingBitRate(128_000)
            mediaRecorder?.setAudioSamplingRate(44100)
            Log.d(TAG, "Audio encoder set: AAC 128kbps 44100Hz")
        }

        // Output file
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Recordly"
        )
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(outputDir, "Recordly_$timestamp.mp4")

        mediaRecorder?.setOutputFile(outputFile.absolutePath)
        Log.d(TAG, "Output file: ${outputFile.absolutePath}")

        mediaRecorder?.prepare()
        Log.d(TAG, "MediaRecorder prepared: ${width}x${height} @ ${targetFps}fps, audio=$hasAudioSource")
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordingState.value is RecordingState.Recording) {
            try {
                mediaRecorder?.pause()
                _recordingState.value = RecordingState.Paused
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isPaused = true))
                Log.d(TAG, "Recording paused")
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
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isPaused = false))
                Log.d(TAG, "Recording resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume", e)
            }
        }
    }

    private fun getResolutionWidth(resolution: String, metrics: DisplayMetrics): Int {
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val smallerDim = minOf(screenWidth, screenHeight)
        return when (resolution) {
            "720p" -> 720
            "1080p" -> 1080
            "1440p" -> 1440
            else -> smallerDim
        }
    }

    private fun getResolutionHeight(resolution: String, metrics: DisplayMetrics): Int {
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val smallerDim = minOf(screenWidth, screenHeight)
        val largerDim = maxOf(screenWidth, screenHeight)
        val aspectRatio = largerDim.toFloat() / smallerDim.toFloat()
        val resWidth = getResolutionWidth(resolution, metrics)
        return (resWidth * aspectRatio).toInt()
    }

    private fun stopRecordingSafely(isError: Boolean, message: String?) {
        Log.d(TAG, "stopRecordingSafely: isError=$isError message=$message")
        _recordingState.value = RecordingState.Stopping
        overlayManager?.hideOverlay()

        try {
            mediaRecorder?.stop()
            Log.d(TAG, "MediaRecorder stopped")
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
        Log.d(TAG, "VirtualDisplay released")

        mediaProjection?.stop()
        mediaProjection = null
        Log.d(TAG, "MediaProjection stopped")

        if (isError && message != null) {
            _recordingState.value = RecordingState.Error(message)
        } else {
            _recordingState.value = RecordingState.Saved
        }

        serviceScope.launch {
            delay(2500)
            if (_recordingState.value is RecordingState.Saved || _recordingState.value is RecordingState.Error) {
                _recordingState.value = RecordingState.Idle
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Recordly is recording your screen"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 3, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Recording Paused" else "Recording Screen")
            .setContentText("Tap to open Recordly")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopPendingIntent
                ).build()
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPaused) {
                val resumeIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME }
                val resumePending = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                builder.addAction(NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Resume", resumePending).build())
            } else {
                val pauseIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE }
                val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                builder.addAction(NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pausePending).build())
            }
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        overlayManager?.hideOverlay()
    }
}
