package com.recordly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.drawToBitmap
import com.recordly.app.ui.navigation.RecordlyNavGraph
import com.recordly.app.ui.theme.RecordlyTheme
import kotlinx.coroutines.flow.map
import kotlin.math.hypot

/**
 * MainActivity — single-activity host.
 *
 * Theme switching uses a Telegram-style circular reveal animation:
 * 1. Before applying a new theme, the current view is captured as a bitmap.
 * 2. The new theme is applied (content re-renders underneath).
 * 3. The old-theme bitmap is drawn on top with a circular cutout that
 *    expands from the top-right corner, progressively revealing the new theme.
 *
 * The trick is decoupling the DataStore theme (what the user selected) from the
 * "applied" theme (what RecordlyTheme actually uses). This lets us capture the
 * old theme's bitmap before the new theme takes effect.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: let Compose handle insets
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val appContainer = (application as RecordlyApplication).container

        setContent {
            val view = LocalView.current

            // ── Read theme preferences from DataStore ──
            val dataStoreTheme by appContainer.preferencesRepository.userPreferencesFlow
                .map { it.theme }
                .collectAsState(initial = "System")

            val dataStoreDynamic by appContainer.preferencesRepository.userPreferencesFlow
                .map { it.dynamicColor }
                .collectAsState(initial = false)

            // ── Decoupled applied theme (lags behind DataStore during animation) ──
            var appliedTheme by remember { mutableStateOf<String?>(null) }
            var appliedDynamic by remember { mutableStateOf<Boolean?>(null) }

            // ── Animation state ──
            var overlayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            val revealProgress = remember { Animatable(1f) }
            val launchTime = remember { System.currentTimeMillis() }

            // When DataStore theme changes, capture old bitmap then apply new theme
            LaunchedEffect(dataStoreTheme, dataStoreDynamic) {
                val currentApplied = appliedTheme
                val currentDynamic = appliedDynamic
                val themeChanged = currentApplied != null && currentApplied != dataStoreTheme
                val dynamicChanged = currentDynamic != null && currentDynamic != dataStoreDynamic

                if (themeChanged || dynamicChanged) {
                    // Only animate after the initial DataStore load (>1.5s after launch)
                    val elapsed = System.currentTimeMillis() - launchTime
                    if (elapsed > 1500L) {
                        try {
                            val bitmap = view.drawToBitmap()
                            overlayBitmap = bitmap.asImageBitmap()
                            revealProgress.snapTo(0f)
                        } catch (_: Exception) { /* capture failed, just apply directly */ }
                    }
                }

                // Apply the new theme (whether or not we're animating)
                appliedTheme = dataStoreTheme
                appliedDynamic = dataStoreDynamic

                // Run the reveal animation if we have a captured bitmap
                if (overlayBitmap != null) {
                    try {
                        revealProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = FastOutSlowInEasing
                            )
                        )
                    } finally {
                        overlayBitmap = null
                    }
                }
            }

            RecordlyTheme(
                themePreference = appliedTheme ?: dataStoreTheme,
                dynamicColor = appliedDynamic ?: dataStoreDynamic
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // ── Main content ──
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        RecordlyNavGraph(appContainer = appContainer)
                    }

                    // ── Circular reveal overlay ──
                    // Draws the old-theme bitmap with an expanding circular hole.
                    // The hole grows from the top-right corner, revealing the new theme.
                    overlayBitmap?.let { bitmap ->
                        val progress = revealProgress.value
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxRadius = hypot(
                                size.width.toDouble(),
                                size.height.toDouble()
                            ).toFloat()
                            val radius = maxRadius * progress

                            // Reveal origin: top-right area (like Telegram's sun/moon icon)
                            val revealCenter = Offset(
                                x = size.width * 0.85f,
                                y = size.height * 0.06f
                            )

                            val circlePath = Path().apply {
                                addOval(Rect(center = revealCenter, radius = radius))
                            }

                            // Draw old bitmap everywhere EXCEPT inside the expanding circle.
                            // This reveals the new theme underneath as the circle grows.
                            clipPath(circlePath, ClipOp.Difference) {
                                drawImage(bitmap)
                            }
                        }
                    }
                }
            }
        }
    }
}
