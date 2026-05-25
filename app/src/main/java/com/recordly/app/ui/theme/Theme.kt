package com.recordly.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme using Recordly stable palette.
 * Primary = vivid blue (readable on white backgrounds).
 */
private val RecordlyLightColors = lightColorScheme(
    primary              = RecordlyPrimary,
    onPrimary            = RecordlyOnPrimary,
    secondary            = RecordlySecondary,
    onSecondary          = RecordlyOnSecondary,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    outline              = LightOutline,
    error                = Color(0xFFDC2626),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFF7F1D1D),
    primaryContainer     = Color(0xFFE0E7FF),
    onPrimaryContainer   = Color(0xFF1E3A8A),
    secondaryContainer   = Color(0xFFFEE2E2),
    onSecondaryContainer = Color(0xFF7F1D1D),
)

/**
 * Dark color scheme using Recordly stable palette.
 * Primary = lighter blue so it's readable on dark backgrounds.
 */
private val RecordlyDarkColors = darkColorScheme(
    primary              = RecordlyPrimaryDark,
    onPrimary            = Color(0xFF0F172A),
    secondary            = RecordlySecondary,
    onSecondary          = RecordlyOnSecondary,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    outline              = DarkOutline,
    error                = Color(0xFFF87171),
    onError              = Color(0xFF7F1D1D),
    errorContainer       = Color(0xFF450A0A),
    onErrorContainer     = Color(0xFFFCA5A5),
    primaryContainer     = Color(0xFF1E3A5F),
    onPrimaryContainer   = Color(0xFFBFD0FF),
    secondaryContainer   = Color(0xFF7F1D1D),
    onSecondaryContainer = Color(0xFFFCA5A5),
)

/**
 * AMOLED color scheme — true black backgrounds for OLED displays.
 * Identical to dark but with pure black backgrounds and very dark surfaces.
 */
private val RecordlyAmoledColors = darkColorScheme(
    primary              = RecordlyPrimaryDark,
    onPrimary            = Color(0xFF000000),
    secondary            = RecordlySecondary,
    onSecondary          = RecordlyOnSecondary,
    background           = AmoledBackground,
    onBackground         = AmoledOnBackground,
    surface              = AmoledSurface,
    onSurface            = AmoledOnSurface,
    surfaceVariant       = AmoledSurfaceVariant,
    onSurfaceVariant     = AmoledOnSurfaceVariant,
    outline              = AmoledOutline,
    error                = Color(0xFFF87171),
    onError              = Color(0xFF7F1D1D),
    errorContainer       = Color(0xFF1A0000),
    onErrorContainer     = Color(0xFFFCA5A5),
    primaryContainer     = Color(0xFF0A1628),
    onPrimaryContainer   = Color(0xFFBFD0FF),
    secondaryContainer   = Color(0xFF1A0000),
    onSecondaryContainer = Color(0xFFFCA5A5),
)

/**
 * Root theme composable.
 *
 * themePreference values: "System" | "Light" | "Dark" | "AMOLED"
 * dynamicColor: true = use Material You (Android 12+), false = Recordly stable palette
 *
 * Priority:
 *   1. dynamicColor + Android 12+ → dynamic scheme (ignores AMOLED)
 *   2. AMOLED → black OLED scheme
 *   3. Dark → dark scheme
 *   4. Light → light scheme
 *   5. System → follow OS
 */
@Composable
fun RecordlyTheme(
    themePreference: String = "System",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()

    val useDark = when (themePreference) {
        "Light"  -> false
        "Dark"   -> true
        "AMOLED" -> true
        else     -> systemInDark // "System"
    }
    val isAmoled = (themePreference == "AMOLED")

    val colorScheme = when {
        // Dynamic color (Material You) — Android 12+ only
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        // AMOLED — true black
        isAmoled -> RecordlyAmoledColors
        // Standard dark
        useDark  -> RecordlyDarkColors
        // Standard light
        else     -> RecordlyLightColors
    }

    // Sync status bar and nav bar with theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window

            // Make status bar transparent, handled by WindowCompat
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
