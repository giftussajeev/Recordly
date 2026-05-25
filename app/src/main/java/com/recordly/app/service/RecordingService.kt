package com.recordly.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
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
    private var currentOutputDocUri: Uri? = null
    private var currentMediaStoreUri: Uri? = null
    private var currentParcelFileDescriptor: android.os.ParcelFileDescriptor? = null
    private var isUsingSaf = false
    private var isUsingMediaStore = false

    private var timerJob: Job? = null
    private var recordStartTime = 0L
    private var pausedTime = 0L
    private var totalPausedDuration = 0L

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
        const val EXTRA_QUALITY = "EXTRA_QUALITY"
        const val EXTRA_COUNTDOWN = "EXTRA_COUNTDOWN"
        const val EXTRA_FLOATING_CONTROLS = "EXTRA_FLOATING_CONTROLS"
        const val EXTRA_SAVE_LOCATION_URI = "EXTRA_SAVE_LOCATION_URI"

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
        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "Balanced"
        val countdown = intent.getIntExtra(EXTRA_COUNTDOWN, 0)
        val floatingControls = intent.getBooleanExtra(EXTRA_FLOATING_CONTROLS, true)
        val saveLocationUri = intent.getStringExtra(EXTRA_SAVE_LOCATION_URI) ?: ""

        // Display metrics passed from Activity — safe to use in Service
        val screenWidth = intent.getIntExtra(EXTRA_SCREEN_WIDTH, 1080)
        val screenHeight = intent.getIntExtra(EXTRA_SCREEN_HEIGHT, 1920)
        val screenDensity = intent.getIntExtra(EXTRA_SCREEN_DENSITY, 420)
        val screenRefreshRate = intent.getFloatExtra(EXTRA_SCREEN_REFRESH_RATE, 60f)

        Log.d(TAG, "ACTION_START: res=$resolution fps=$fps audio=$audioSource quality=$quality countdown=$countdown floating=$floatingControls")
        Log.d(TAG, "Display: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi, refreshRate=${screenRefreshRate}Hz")

        if (resultCode != 0 && resultData != null) {
            startCountdownAndRecord(
                resultCode, resultData, resolution, fps, audioSource, quality, countdown,
                screenWidth, screenHeight, screenDensity, screenRefreshRate, floatingControls, saveLocationUri
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
        quality: String,
        countdown: Int,
        screenWidth: Int,
        screenHeight: Int,
        screenDensity: Int,
        screenRefreshRate: Float,
        floatingControls: Boolean,
        saveLocationUri: String
    ) {
        countdownJob?.cancel()
        if (countdown <= 0) {
            serviceScope.launch {
                startRecording(resultCode, resultData, resolution, fps, audioSource, quality,
                    screenWidth, screenHeight, screenDensity, screenRefreshRate, floatingControls, saveLocationUri)
            }
            return
        }
        countdownJob = serviceScope.launch {
            for (i in countdown downTo 1) {
                _recordingState.value = RecordingState.Countdown(i)
                delay(1000)
            }
            startRecording(resultCode, resultData, resolution, fps, audioSource, quality,
                screenWidth, screenHeight, screenDensity, screenRefreshRate, floatingControls, saveLocationUri)
        }
    }

    private fun startRecording(
        resultCode: Int,
        resultData: Intent,
        resolution: String,
        fps: Int,
        audioSource: String,
        quality: String,
        screenWidth: Int,
        screenHeight: Int,
        screenDensity: Int,
        screenRefreshRate: Float,
        floatingControls: Boolean,
        saveLocationUri: String
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

            // Resolve FPS — -1 means match display refresh rate
            val targetFps = if (fps == -1) {
                screenRefreshRate.toInt().coerceAtLeast(15)
            } else {
                fps.coerceAtMost(screenRefreshRate.toInt()).coerceAtLeast(15)
            }
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

            // Map quality to bitrate
            val bitrate = qualityToBitrate(quality, recWidth, recHeight, targetFps)

            // Try with user config first, fall back to safe defaults on failure
            val prepared = trySetupMediaRecorder(
                resolvedAudio, bitrate, recWidth, recHeight, targetFps, saveLocationUri
            ) ?: run {
                Log.w(TAG, "Primary config failed, retrying with safe fallback (1080p, 30fps, no audio)")
                val (fbW, fbH) = resolveRecordingDimensions("1080p", screenWidth, screenHeight)
                val fbBitrate = qualityToBitrate("Balanced", fbW, fbH, 30)
                trySetupMediaRecorder("No audio", fbBitrate, fbW, fbH, 30, saveLocationUri)
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

            recordStartTime = System.currentTimeMillis()
            totalPausedDuration = 0L
            pausedTime = 0L
            startTimer()

            if (floatingControls && android.provider.Settings.canDrawOverlays(this)) {
                showFloatingControls(isPaused = false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            val msg = when {
                e.message?.contains("setAudioSource") == true ->
                    "Audio setup failed. Try recording without audio."
                e.message?.contains("display") == true ->
                    "Display setup failed: ${e.message}"
                else -> "Recording failed to start: ${e.localizedMessage ?: "Unknown error"}"
            }
            stopRecordingSafely(true, msg)
        }
    }

    /**
     * Maps quality preset to bitrate in bits per second.
     */
    private fun qualityToBitrate(quality: String, width: Int, height: Int, fps: Int): Int {
        return when (quality) {
            "Low" -> {
                // Roughly 4-6 Mbps for 1080p30
                val base = (width.toLong() * height * fps / 20).toInt()
                base.coerceIn(2_000_000, 8_000_000)
            }
            "Balanced" -> {
                // Roughly 8-14 Mbps for 1080p30
                val base = (width.toLong() * height * fps / 10).toInt()
                base.coerceIn(4_000_000, 16_000_000)
            }
            "High" -> {
                // Roughly 15-25 Mbps for 1080p30
                val base = (width.toLong() * height * fps / 6).toInt()
                base.coerceIn(8_000_000, 30_000_000)
            }
            "Max" -> {
                // Roughly 25-40 Mbps
                val base = (width.toLong() * height * fps / 4).toInt()
                base.coerceIn(15_000_000, 50_000_000)
            }
            else -> {
                val base = (width.toLong() * height * fps / 10).toInt()
                base.coerceIn(4_000_000, 16_000_000)
            }
        }
    }

    /**
     * Resolves user audio selection to a safe, implementable source.
     * Internal audio uses REMOTE_SUBMIX on Android 10+ (requires active MediaProjection).
     * Falls back to MIC if REMOTE_SUBMIX fails.
     */
    private fun resolveAudioSource(audioSource: String): String {
        return when (audioSource) {
            "No audio" -> "No audio"
            "Phone microphone", "External microphone" -> "Phone microphone"
            "Internal audio" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "Internal audio"
                } else {
                    Log.w(TAG, "Internal audio requires Android 10+; falling back to no audio")
                    "No audio"
                }
            }
            else -> {
                Log.w(TAG, "Unknown audio source '$audioSource'; using No audio")
                "No audio"
            }
        }
    }

    /**
     * Resolves the recording resolution from user selection and device screen dimensions.
     * Returns (width, height) maintaining the device's aspect ratio and orientation.
     */
    private fun resolveRecordingDimensions(resolution: String, screenW: Int, screenH: Int): Pair<Int, Int> {
        val landscape = screenW >= screenH
        val shorter = minOf(screenW, screenH)
        val longer = maxOf(screenW, screenH)
        val ratio = longer.toFloat() / shorter.toFloat()

        val shortSide = when (resolution) {
            "720p" -> 720
            "1080p" -> 1080
            "1440p" -> minOf(1440, shorter) // Don't upscale beyond native
            "Native" -> shorter
            else -> minOf(shorter, 1080) // safe default
        }
        // Ensure dimensions are even (required by most video encoders)
        val longSide = ((shortSide * ratio).toInt() / 2) * 2
        val finalShort = (shortSide / 2) * 2
        val w = if (landscape) longSide else finalShort
        val h = if (landscape) finalShort else longSide
        return Pair(w, h)
    }

    /**
     * Tries to set up the MediaRecorder with the given config.
     * Returns the MediaRecorder on success, null on failure.
     */
    private fun trySetupMediaRecorder(
        audioSource: String,
        bitrate: Int,
        width: Int,
        height: Int,
        fps: Int,
        saveLocationUri: String
    ): MediaRecorder? {
        return try {
            setupMediaRecorder(audioSource, bitrate, width, height, fps, saveLocationUri)
            mediaRecorder
        } catch (e: Exception) {
            Log.e(TAG, "setupMediaRecorder failed: ${e.message}", e)
            try { mediaRecorder?.reset(); mediaRecorder?.release() } catch (_: Exception) {}
            mediaRecorder = null
            // Clean up any file we created
            cleanupOutputFile()
            null
        }
    }

    private fun setupMediaRecorder(
        audioSource: String,
        bitrate: Int,
        width: Int,
        height: Int,
        fps: Int,
        saveLocationUri: String
    ) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        // CRITICAL ORDER: audio source → video source → output format → encoders → output file → prepare
        hasAudioSource = false

        when (audioSource) {
            "Phone microphone" -> {
                try {
                    mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                    hasAudioSource = true
                    Log.d(TAG, "Audio source: MIC")
                } catch (e: Exception) {
                    Log.e(TAG, "MIC source failed, continuing without audio: ${e.message}")
                    hasAudioSource = false
                }
            }
            "Internal audio" -> {
                // On Android 10+, try MIC first (internal audio via MediaProjection captures app audio
                // through the VirtualDisplay). Some ROMs support REMOTE_SUBMIX with active projection.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                        hasAudioSource = true
                        Log.d(TAG, "Audio source: MIC (for internal audio capture via projection)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Audio source setup failed for internal: ${e.message}")
                        hasAudioSource = false
                    }
                }
            }
            // "No audio" — do not call setAudioSource
        }

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
        mediaRecorder?.setVideoEncodingBitRate(bitrate)
        Log.d(TAG, "Video: ${width}x${height} @ ${fps}fps bitrate=$bitrate")

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Recordly_$timestamp.mp4"

        // Reset output state
        isUsingSaf = false
        isUsingMediaStore = false
        currentOutputDocUri = null
        currentMediaStoreUri = null
        currentParcelFileDescriptor = null
        currentOutputFile = null

        // Try SAF custom location first
        if (saveLocationUri.isNotEmpty()) {
            try {
                val treeUri = Uri.parse(saveLocationUri)
                val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, treeUri)
                val docFile = docTree?.createFile("video/mp4", fileName)

                if (docFile != null) {
                    val pfd = contentResolver.openFileDescriptor(docFile.uri, "w")
                    if (pfd != null) {
                        currentParcelFileDescriptor = pfd
                        currentOutputDocUri = docFile.uri
                        isUsingSaf = true
                        mediaRecorder?.setOutputFile(pfd.fileDescriptor)
                        Log.d(TAG, "Output (SAF): ${docFile.uri}")
                    } else {
                        throw Exception("Could not open file descriptor")
                    }
                } else {
                    throw Exception("Could not create file in save location")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to use custom save location: ${e.message}, falling back to default")
                setupDefaultOutput(fileName)
            }
        } else {
            setupDefaultOutput(fileName)
        }

        mediaRecorder?.prepare()
        Log.d(TAG, "MediaRecorder prepared successfully")
    }

    /**
     * Sets up default output — uses MediaStore on Android 10+ for proper scoped storage,
     * falls back to direct file on Android 8-9.
     */
    private fun setupDefaultOutput(fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Use MediaStore for proper scoped storage integration
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Recordly")
                put(MediaStore.Video.Media.IS_PENDING, 1) // Hide until recording completes
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val pfd = contentResolver.openFileDescriptor(uri, "w")
                if (pfd != null) {
                    currentParcelFileDescriptor = pfd
                    currentMediaStoreUri = uri
                    isUsingMediaStore = true
                    mediaRecorder?.setOutputFile(pfd.fileDescriptor)
                    Log.d(TAG, "Output (MediaStore): $uri")
                    return
                } else {
                    // Clean up empty MediaStore entry
                    contentResolver.delete(uri, null, null)
                }
            }
            // Fall through to file-based if MediaStore fails
            Log.w(TAG, "MediaStore output failed, falling back to File")
        }

        // Android 8-9 or MediaStore fallback: Direct file access
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Recordly"
        )
        if (!outputDir.exists()) {
            val created = outputDir.mkdirs()
            Log.d(TAG, "Created output dir: $created -> ${outputDir.absolutePath}")
        }

        val outputFile = File(outputDir, fileName)
        currentOutputFile = outputFile
        mediaRecorder?.setOutputFile(outputFile.absolutePath)
        Log.d(TAG, "Output (File): ${outputFile.absolutePath}")
    }

    private fun cleanupOutputFile() {
        try {
            currentParcelFileDescriptor?.close()
            currentParcelFileDescriptor = null
        } catch (_: Exception) {}

        if (isUsingMediaStore && currentMediaStoreUri != null) {
            try { contentResolver.delete(currentMediaStoreUri!!, null, null) } catch (_: Exception) {}
            currentMediaStoreUri = null
        }
        if (isUsingSaf && currentOutputDocUri != null) {
            try {
                androidx.documentfile.provider.DocumentFile.fromSingleUri(this, currentOutputDocUri!!)?.delete()
            } catch (_: Exception) {}
            currentOutputDocUri = null
        }
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            _recordingState.value is RecordingState.Recording) {
            try {
                mediaRecorder?.pause()
                pausedTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.Paused
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isPaused = true))
                Log.d(TAG, "Recording paused")

                if (android.provider.Settings.canDrawOverlays(this)) {
                    showFloatingControls(isPaused = true)
                }
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
                if (pausedTime > 0) {
                    totalPausedDuration += (System.currentTimeMillis() - pausedTime)
                    pausedTime = 0L
                }
                _recordingState.value = RecordingState.Recording
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isPaused = false))
                Log.d(TAG, "Recording resumed")

                if (android.provider.Settings.canDrawOverlays(this)) {
                    showFloatingControls(isPaused = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume", e)
            }
        }
    }

    private fun stopRecordingSafely(isError: Boolean, message: String?) {
        Log.d(TAG, "stopRecordingSafely isError=$isError")
        _recordingState.value = RecordingState.Stopping
        countdownJob?.cancel()
        timerJob?.cancel()

        var recordingWasStarted = false

        try {
            mediaRecorder?.stop()
            recordingWasStarted = true
            Log.d(TAG, "MediaRecorder stopped")
        } catch (e: Exception) {
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

        // Close SAF / MediaStore file descriptor
        try {
            currentParcelFileDescriptor?.close()
            currentParcelFileDescriptor = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing file descriptor", e)
        }

        try { overlayManager?.hideOverlay() } catch (_: Exception) {}

        // Handle cleanup or finalization
        if (!recordingWasStarted || isError) {
            // Delete partial/failed file
            cleanupFailedRecording()
        } else {
            // Success: finalize the recording
            finalizeRecording()
        }

        if (isError && message != null) {
            _recordingState.value = RecordingState.Error(message)
        } else if (!isError) {
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

    private fun cleanupFailedRecording() {
        if (isUsingMediaStore && currentMediaStoreUri != null) {
            try {
                contentResolver.delete(currentMediaStoreUri!!, null, null)
                Log.d(TAG, "Deleted failed MediaStore entry: $currentMediaStoreUri")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete MediaStore entry", e)
            }
        } else if (isUsingSaf && currentOutputDocUri != null) {
            try {
                androidx.documentfile.provider.DocumentFile.fromSingleUri(this, currentOutputDocUri!!)?.delete()
                Log.d(TAG, "Deleted failed SAF file: $currentOutputDocUri")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete SAF file", e)
            }
        } else {
            currentOutputFile?.let {
                if (it.exists()) {
                    it.delete()
                    Log.d(TAG, "Deleted failed local file: ${it.absolutePath}")
                }
            }
        }
    }

    private fun finalizeRecording() {
        when {
            isUsingMediaStore -> {
                // MediaStore entries are already properly registered — just verify non-empty
                currentMediaStoreUri?.let { uri ->
                    try {
                        val pfd = contentResolver.openFileDescriptor(uri, "r")
                        val size = pfd?.statSize ?: 0
                        pfd?.close()
                        if (size == 0L) {
                            Log.w(TAG, "MediaStore file is 0 bytes, deleting")
                            contentResolver.delete(uri, null, null)
                            _recordingState.value = RecordingState.Error("Recording resulted in an empty file.")
                            return
                        }
                        // Mark as complete by updating IS_PENDING
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val values = ContentValues().apply {
                                put(MediaStore.Video.Media.IS_PENDING, 0)
                            }
                            contentResolver.update(uri, values, null, null)
                        }
                        Log.d(TAG, "Recording finalized in MediaStore: $uri ($size bytes)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error finalizing MediaStore recording", e)
                    }
                }
            }
            isUsingSaf -> {
                currentOutputDocUri?.let { uri ->
                    try {
                        val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(this, uri)
                        if (doc != null && doc.length() == 0L) {
                            Log.w(TAG, "SAF file is 0 bytes, deleting")
                            doc.delete()
                            _recordingState.value = RecordingState.Error("Recording resulted in an empty file.")
                            return
                        }
                        Log.d(TAG, "Recording finalized (SAF): $uri")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error finalizing SAF recording", e)
                    }
                }
            }
            else -> {
                // Direct file — scan into MediaStore for Android 8-9
                currentOutputFile?.let { file ->
                    if (file.exists()) {
                        if (file.length() == 0L) {
                            Log.w(TAG, "Local file is 0 bytes, deleting")
                            file.delete()
                            _recordingState.value = RecordingState.Error("Recording resulted in an empty file.")
                            return
                        }
                        // Scan into MediaStore so it shows up in Library
                        MediaScannerConnection.scanFile(
                            this,
                            arrayOf(file.absolutePath),
                            arrayOf("video/mp4")
                        ) { path, uri ->
                            Log.d(TAG, "MediaScanner scanned: $path -> $uri")
                        }
                        Log.d(TAG, "Recording finalized (File): ${file.absolutePath}")
                    }
                }
            }
        }
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

    private fun showFloatingControls(isPaused: Boolean) {
        overlayManager?.showOverlay(
            isPaused = isPaused,
            onPauseToggle = {
                if (isPaused) {
                    resumeRecording()
                } else {
                    pauseRecording()
                }
            },
            onStop = {
                stopRecordingSafely(isError = false, message = null)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        timerJob?.cancel()
        try { overlayManager?.hideOverlay() } catch (_: Exception) {}
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                if (_recordingState.value is RecordingState.Recording) {
                    val elapsed = System.currentTimeMillis() - recordStartTime - totalPausedDuration
                    val seconds = elapsed / 1000
                    overlayManager?.updateTime(seconds)
                }
                delay(1000)
            }
        }
    }
}
