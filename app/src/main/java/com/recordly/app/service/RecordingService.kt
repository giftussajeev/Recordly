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

/**
 * RecordingService — foreground service that performs screen capture.
 *
 * CRITICAL: This service must NOT access display/window metrics via its own context.
 * Service contexts are not associated with a display on Android 11+ and will throw
 * "Tried to obtain display from a Context not associated with one."
 *
 * All display info (width, height, density, refreshRate) MUST be passed as Intent extras
 * from the Activity that holds a valid display context.
 */
class RecordingService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var overlayManager: FloatingOverlayManager? = null
    private var hasAudioSource = false
    private var currentOutputFile: File? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var countdownJob: Job? = null

    companion object {
        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        const val ACTION_START = "com.recordly.ACTION_START_RECORDING"
        const val ACTION_STOP = "com.recordly.ACTION_STOP_RECORDING"
        const val ACTION_PAUSE = "com.recordly.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME = "com.recordly.ACTION_RESUME_RECORDING"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"

        // Recording config
        const val EXTRA_RESOLUTION = "EXTRA_RESOLUTION"
        const val EXTRA_FPS = "EXTRA_FPS"
        const val EXTRA_AUDIO_SOURCE = "EXTRA_AUDIO_SOURCE"
        const val EXTRA_BITRATE = "EXTRA_BITRATE"
        const val EXTRA_COUNTDOWN = "EXTRA_COUNTDOWN"

        // Display metrics — MUST be passed from Activity context (not Service)
        const val EXTRA_SCREEN_WIDTH = "EXTRA_SCREEN_WIDTH"
        const val EXTRA_SCREEN_HEIGHT = "EXTRA_SCREEN_HEIGHT"
        const val EXTRA_SCREEN_DENSITY = "EXTRA_SCREEN_DENSITY"
        const val EXTRA_SCREEN_REFRESH_RATE = "EXTRA_SCREEN_REFRESH_RATE"

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
            ACTION_START -> handleStartAction(intent)
            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP received")
                stopRecordingSafely(isError = false, message = null)
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_NOT_STICKY
    }

    private fun handleStartAction(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        @Suppress("DEPRECATION")
        val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

        val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"
        val fps = intent.getIntExtra(EXTRA_FPS, 30)
        val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: "No audio"
        val bitrate = intent.getStringExtra(EXTRA_BITRATE) ?: "Auto"
        val countdown = intent.getIntExtra(EXTRA_COUNTDOWN, 0)

        // Display metrics passed from Activity — safe to use in Service
        val screenWidth = intent.getIntExtra(EXTRA_SCREEN_WIDTH, 1080)
        val screenHeight = intent.getIntExtra(EXTRA_SCREEN_HEIGHT, 1920)
        val screenDensity = intent.getIntExtra(EXTRA_SCREEN_DENSITY, 420)
        val screenRefreshRate = intent.getFloatExtra(EXTRA_SCREEN_REFRESH_RATE, 60f)

        Log.d(TAG, "ACTION_START: res=$resolution fps=$fps audio=$audioSource bitrate=$bitrate countdown=$countdown")
        Log.d(TAG, "Display: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi, refreshRate=${screenRefreshRate}Hz")

        if (resultCode != 0 && resultData != null) {
            startCountdownAndRecord(
                resultCode, resultData, resolution, fps, audioSource, bitrate, countdown,
                screenWidth, screenHeight, screenDensity, screenRefreshRate
            )
        } else {
            Log.e(TAG, "Invalid permission data: resultCode=$resultCode")
            stopRecordingSafely(isError = true, message = "Screen capture permission was denied.")
        }
    }

    private fun startCountdownAndRecord(
        resultCode: Int,
        resultData: Intent,
        resolution: String,
        fps: Int,
        audioSource: String,
        bitrateConfig: String,
        countdown: Int,
        screenWidth: Int,
        screenHeight: Int,
        screenDensity: Int,
        screenRefreshRate: Float
    ) {
        countdownJob?.cancel()
        if (countdown <= 0) {
            serviceScope.launch {
                startRecording(resultCode, resultData, resolution, fps, audioSource, bitrateConfig,
                    screenWidth, screenHeight, screenDensity, screenRefreshRate)
            }
            return
        }
        countdownJob = serviceScope.launch {
            for (i in countdown downTo 1) {
                _recordingState.value = RecordingState.Countdown(i)
                delay(1000)
            }
            startRecording(resultCode, resultData, resolution, fps, audioSource, bitrateConfig,
                screenWidth, screenHeight, screenDensity, screenRefreshRate)
        }
    }

    private fun startRecording(
        resultCode: Int,
        resultData: Intent,
        resolution: String,
        fps: Int,
        audioSource: String,
        bitrateConfig: String,
        screenWidth: Int,
        screenHeight: Int,
        screenDensity: Int,
        screenRefreshRate: Float
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
                stopRecordingSafely(true, "Screen capture permission was not granted. Please try again.")
                return
            }
            Log.d(TAG, "MediaProjection acquired")

            // Resolve safe audio source
        val resolvedAudio = resolveAudioSource(audioSource)
            Log.d(TAG, "Audio: '$audioSource' -> '$resolvedAudio'")

            // Calculate actual recording dimensions from the passed screen info
            val (recWidth, recHeight) = resolveRecordingDimensions(
                resolution, screenWidth, screenHeight
            )
            Log.d(TAG, "Recording dimensions: ${recWidth}x${recHeight}")

            // Cap fps to display refresh rate
            val targetFps = fps.coerceAtMost(screenRefreshRate.toInt()).coerceAtLeast(15)
            Log.d(TAG, "Target FPS: $targetFps (requested=$fps, display=${screenRefreshRate}Hz)")

            // Register callback BEFORE createVirtualDisplay (Android 14+ requirement)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        Log.d(TAG, "MediaProjection stopped by system")
                        if (_recordingState.value is RecordingState.Recording ||
                            _recordingState.value is RecordingState.Paused) {
                            stopRecordingSafely(isError = false, message = null)
                        }
                    }
                }, null)
            }

            // Try with user config first, fall back to safe defaults on failure
            val prepared = trySetupMediaRecorder(
                resolvedAudio, bitrateConfig, recWidth, recHeight, targetFps
            ) ?: run {
                Log.w(TAG, "Primary config failed, retrying with safe fallback (1080p, 30fps, no audio)")
                val (fbW, fbH) = resolveRecordingDimensions("1080p", screenWidth, screenHeight)
                trySetupMediaRecorder("No audio", "Auto", fbW, fbH, 30)
            }

            if (prepared == null) {
                Log.e(TAG, "MediaRecorder prepare failed with all configs")
                stopRecordingSafely(true, "Recording setup failed. Try lower resolution or restart the app.")
                return
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "RecordlyDisplay",
                recWidth,
                recHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            if (virtualDisplay == null) {
                Log.e(TAG, "Failed to create VirtualDisplay")
                stopRecordingSafely(true, "Failed to create virtual display for recording.")
                return
            }

            mediaRecorder?.start()
            _recordingState.value = RecordingState.Recording
            Log.d(TAG, "Recording started: ${recWidth}x${recHeight} @ ${targetFps}fps audio=$resolvedAudio")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            val msg = when {
                e.message?.contains("setAudioSource") == true ->
                    "Audio setup failed. Retrying without audio is recommended."
                e.message?.contains("display") == true ->
                    "Display setup failed: ${e.message}"
                else -> "Recording failed to start: ${e.localizedMessage ?: "Unknown error"}"
            }
            stopRecordingSafely(true, msg)
        }
    }

    /**
     * Resolves user audio selection to a safe, implementable source.
     * REMOTE_SUBMIX requires system permission — never use it.
     * Internal audio (AudioPlaybackCapture) requires Android 10+ and MediaProjection.
     * For now, only MIC is implemented. Internal audio can be added as a future enhancement.
     */
    private fun resolveAudioSource(audioSource: String): String {
        return when (audioSource) {
            "No audio" -> "No audio"
            "Phone microphone", "External microphone" -> "Phone microphone"
            "Internal audio" -> {
                Log.w(TAG, "Internal audio requested but not yet implemented; using No audio")
                "No audio"
            }
            "Internal audio + microphone" -> {
                Log.w(TAG, "Internal+mic requested but not yet implemented; falling back to MIC")
                "Phone microphone"
            }
            else -> {
                Log.w(TAG, "Unknown audio source '$audioSource'; using No audio")
                "No audio"
            }
        }
    }

    /**
     * Resolves the recording resolution from user selection and device screen dimensions.
     * Returns (width, height) in landscape orientation (width >= height).
     */
    private fun resolveRecordingDimensions(resolution: String, screenW: Int, screenH: Int): Pair<Int, Int> {
        val landscape = screenW >= screenH
        val shorter = minOf(screenW, screenH)
        val longer = maxOf(screenW, screenH)
        val ratio = longer.toFloat() / shorter.toFloat()

        val shortSide = when (resolution) {
            "720p" -> 720
            "1080p" -> 1080
            "1440p" -> 1440
            "Native" -> shorter
            else -> minOf(shorter, 1080) // safe default
        }
        // Ensure dimensions are even (required by most video encoders)
        val longSide = ((shortSide * ratio).toInt() / 2) * 2
        val w = if (landscape) longSide else shortSide
        val h = if (landscape) shortSide else longSide
        return Pair(w, h)
    }

    /**
     * Tries to set up the MediaRecorder with the given config.
     * Returns the MediaRecorder on success, null on failure.
     * On failure, cleans up the recorder so it can be re-created.
     */
    private fun trySetupMediaRecorder(
        audioSource: String,
        bitrateConfig: String,
        width: Int,
        height: Int,
        fps: Int
    ): MediaRecorder? {
        return try {
            setupMediaRecorder(audioSource, bitrateConfig, width, height, fps)
            mediaRecorder
        } catch (e: Exception) {
            Log.e(TAG, "setupMediaRecorder failed: ${e.message}")
            try { mediaRecorder?.reset(); mediaRecorder?.release() } catch (_: Exception) {}
            mediaRecorder = null
            null
        }
    }

    private fun setupMediaRecorder(
        audioSource: String,
        bitrateConfig: String,
        width: Int,
        height: Int,
        fps: Int
    ) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        // CRITICAL ORDER: audio source → video source → output format → encoders → output file → prepare
        hasAudioSource = false
        if (audioSource == "Phone microphone") {
            try {
                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                hasAudioSource = true
                Log.d(TAG, "Audio source: MIC")
            } catch (e: Exception) {
                // Permission denied or hardware unavailable — continue without audio
                Log.e(TAG, "MIC source failed, continuing without audio: ${e.message}")
                hasAudioSource = false
            }
        }
        // "No audio" — do not call setAudioSource

        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        // Audio encoder — only when source was set
        if (hasAudioSource) {
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setAudioEncodingBitRate(128_000)
            mediaRecorder?.setAudioSamplingRate(44100)
            Log.d(TAG, "Audio: AAC 128kbps 44100Hz")
        }

        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder?.setVideoSize(width, height)
        mediaRecorder?.setVideoFrameRate(fps)

        val bitrate = when (bitrateConfig) {
            "8 Mbps" -> 8_000_000
            "12 Mbps" -> 12_000_000
            "20 Mbps" -> 20_000_000
            "35 Mbps" -> 35_000_000
            else -> {
                // Auto: estimate based on resolution and fps, clamped to safe range
                val base = (width.toLong() * height * fps / 8).toInt()
                base.coerceIn(2_000_000, 30_000_000)
            }
        }
        mediaRecorder?.setVideoEncodingBitRate(bitrate)
        Log.d(TAG, "Video: ${width}x${height} @ ${fps}fps bitrate=$bitrate")

        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Recordly"
        )
        if (!outputDir.exists()) {
            val created = outputDir.mkdirs()
            Log.d(TAG, "Created output dir: $created -> ${outputDir.absolutePath}")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(outputDir, "Recordly_$timestamp.mp4")
        currentOutputFile = outputFile

        mediaRecorder?.setOutputFile(outputFile.absolutePath)
        Log.d(TAG, "Output: ${outputFile.absolutePath}")

        mediaRecorder?.prepare()
        Log.d(TAG, "MediaRecorder prepared")
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            _recordingState.value is RecordingState.Recording) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            _recordingState.value is RecordingState.Paused) {
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

    private fun stopRecordingSafely(isError: Boolean, message: String?) {
        Log.d(TAG, "stopRecordingSafely isError=$isError")
        _recordingState.value = RecordingState.Stopping
        countdownJob?.cancel()

        var recordingWasStarted = false

        try {
            mediaRecorder?.stop()
            recordingWasStarted = true
            Log.d(TAG, "MediaRecorder stopped")
        } catch (e: Exception) {
            // stop() throws if start() was never called (e.g. error before start)
            Log.w(TAG, "MediaRecorder stop exception (may be normal if never started): ${e.message}")
        }

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder release error", e)
        }
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null
        Log.d(TAG, "VirtualDisplay released")

        mediaProjection?.stop()
        mediaProjection = null
        Log.d(TAG, "MediaProjection stopped")

        // Delete partial file if recording never properly started
        if (!recordingWasStarted && !isError) {
            currentOutputFile?.let {
                if (it.exists() && it.length() == 0L) it.delete()
            }
        }

        if (isError && message != null) {
            _recordingState.value = RecordingState.Error(message)
        } else {
            _recordingState.value = RecordingState.Saved
        }

        serviceScope.launch {
            delay(3000)
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
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPI = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Recording Paused" else "Recording Screen")
            .setContentText("Tap to return to Recordly")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPI)
            .addAction(0, "Stop", stopPI)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val pauseResumeAction = if (isPaused) {
                val resumeIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME }
                val resumePI = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action(0, "Resume", resumePI)
            } else {
                val pauseIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE }
                val pausePI = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action(0, "Pause", pausePI)
            }
            builder.addAction(pauseResumeAction)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try { overlayManager?.hideOverlay() } catch (_: Exception) {}
    }
}
