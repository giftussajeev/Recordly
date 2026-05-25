package com.recordly.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.recordly.app.ui.theme.RecordlyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class FloatingOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private val isPausedFlow = MutableStateFlow(false)
    private val timeSecondsFlow = MutableStateFlow(0L)

    /**
     * Incremented on any user interaction (drag, tap, button press).
     * The Compose auto-collapse timer restarts whenever this changes.
     */
    private val interactionCountFlow = MutableStateFlow(0)

    private fun notifyInteraction() {
        interactionCountFlow.value++
    }

    fun showOverlay(isPaused: Boolean, onPauseToggle: () -> Unit, onStop: () -> Unit) {
        isPausedFlow.value = isPaused
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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 100

        val container = FrameLayout(context)
        val composeView = ComposeView(context).apply {
            setContent {
                val paused = isPausedFlow.collectAsState().value
                val seconds = timeSecondsFlow.collectAsState().value
                val interactionCount = interactionCountFlow.collectAsState().value
                RecordlyTheme(themePreference = "System", dynamicColor = false) {
                    RecordlyOverlayBubble(
                        isPaused = paused,
                        timeSeconds = seconds,
                        interactionKey = interactionCount,
                        onPauseToggle = {
                            notifyInteraction()
                            onPauseToggle()
                        },
                        onStop = {
                            notifyInteraction()
                            onStop()
                        },
                        onExpandTap = {
                            notifyInteraction()
                        }
                    )
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
        
        container.addView(composeView)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    notifyInteraction()
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
            notifyInteraction() // Start the auto-collapse timer
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

    fun updateTime(seconds: Long) {
        timeSecondsFlow.value = seconds
    }
}

/**
 * Floating overlay bubble with auto-collapse.
 *
 * After 4 seconds of no interaction, the overlay collapses to a small dot + timer.
 * Tapping the collapsed bubble expands it back. Any button press or drag resets
 * the 4-second timer.
 *
 * This effectively "auto-hides" in games and fullscreen apps — the collapsed
 * dot is small and unobtrusive, while the user can always tap to expand.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RecordlyOverlayBubble(
    isPaused: Boolean,
    timeSeconds: Long,
    interactionKey: Int,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onExpandTap: () -> Unit
) {
    var isCollapsed by remember { mutableStateOf(false) }

    // Auto-collapse after 4 seconds of no interaction.
    // Each change to interactionKey restarts this effect.
    LaunchedEffect(interactionKey) {
        isCollapsed = false
        delay(4000L)
        isCollapsed = true
    }

    val mm = timeSeconds / 60
    val ss = timeSeconds % 60
    val timerText = String.format("%02d:%02d", mm, ss)

    AnimatedContent(
        targetState = isCollapsed,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.9f))
                .togetherWith(fadeOut() + scaleOut(targetScale = 0.9f))
        },
        label = "overlay_collapse"
    ) { collapsed ->
        if (collapsed) {
            // ── Collapsed: small dot + timer ──
            Row(
                modifier = Modifier
                    .shadow(6.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .clickable { onExpandTap() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.error
                        )
                )
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            // ── Expanded: full controls ──
            Row(
                modifier = Modifier
                    .shadow(8.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Red dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.error
                        )
                )

                // Timer
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
