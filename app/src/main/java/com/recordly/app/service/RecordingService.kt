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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var overlayManager: FloatingOverlayManager? = null

    private var isRecording = false

    companion object {
        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        const val ACTION_TOGGLE_MIC = "ACTION_TOGGLE_MIC"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        
        // Settings Extras
        const val EXTRA_RESOLUTION = "EXTRA_RESOLUTION"
        const val EXTRA_FPS = "EXTRA_FPS"
        const val EXTRA_AUDIO_SOURCE = "EXTRA_AUDIO_SOURCE"
        const val EXTRA_BITRATE = "EXTRA_BITRATE"

        private const val CHANNEL_ID = "recordly_recording_channel"
        private const val NOTIFICATION_ID = 112
        private const val TAG = "RecordingService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayManager = FloatingOverlayManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                
                val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"
                val fps = intent.getIntExtra(EXTRA_FPS, 60)
                val audioSource = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: "No audio"
                val bitrate = intent.getStringExtra(EXTRA_BITRATE) ?: "Auto"

                if (resultCode != 0 && resultData != null) {
                    startRecording(resultCode, resultData, resolution, fps, audioSource, bitrate)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> stopRecording()
            ACTION_TOGGLE_MIC -> toggleMic()
        }
        return START_NOT_STICKY
    }

    private fun toggleMic() {
        // Advanced logic to pause/resume audio recording
    }

    private fun startRecording(
        resultCode: Int, 
        resultData: Intent, 
        resolution: String, 
        fps: Int, 
        audioSource: String,
        bitrateConfig: String
    ) {
        if (isRecording) return

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

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
            isRecording = true
            
            // Show Overlay
            overlayManager?.showOverlay(
                isRecording = true,
                onRecordToggle = { stopRecording() },
                onMicToggle = { toggleMic() }
            )

            Log.d(TAG, "Recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            stopRecording()
        }
    }

    private fun setupMediaRecorder(resolution: String, fps: Int, audioSource: String, bitrateConfig: String) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        // Setup Audio Source
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
            else -> width * height * fps / 10 // Auto estimation
        }
        mediaRecorder?.setVideoEncodingBitRate(bitrate)
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayFps = windowManager.defaultDisplay.refreshRate
        val targetFps = if (fps > displayFps.toInt()) {
            Log.w(TAG, "Requested FPS ($fps) exceeds Display Hz ($displayFps).")
            fps
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

    private fun stopRecording() {
        overlayManager?.hideOverlay()
        
        if (!isRecording) {
            stopForeground(true)
            stopSelf()
            return
        }

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
        }
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        isRecording = false
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Recording in progress...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
