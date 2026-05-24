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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentRed,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentRed,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentRed,
    background = BackgroundAmoled,
    surface = SurfaceAmoled,
    onPrimary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun RecordlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        amoledTheme -> AmoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
