package com.recordly.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.recordly.app.ui.theme.RecordlyTheme

class FloatingOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isOverlayShowing = false

    fun showOverlay(isRecording: Boolean, onRecordToggle: () -> Unit, onMicToggle: () -> Unit) {
        if (isOverlayShowing) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_SECURE, // Try to hide from recording if possible
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 100

        val container = FrameLayout(context)
        val composeView = ComposeView(context).apply {
            setContent {
                RecordlyTheme {
                    // TODO: Implement Compose Overlay UI here
                    // RecordlyOverlayBubble(isRecording, onRecordToggle, onMicToggle)
                }
            }
        }
        
        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
        
        container.setViewTreeLifecycleOwner(lifecycleOwner)
        container.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        // Note: For ViewModelStoreOwner you might need a ViewTreeViewModelStoreOwner if you use ViewModels in overlay.
        
        container.addView(composeView)

        // Implement simple drag handle logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(container, layoutParams)
            overlayView = container
            isOverlayShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideOverlay() {
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            isOverlayShowing = false
        }
    }
}
