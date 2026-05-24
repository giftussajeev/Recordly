package com.recordly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.recordly.app.ui.navigation.RecordlyNavGraph
import com.recordly.app.ui.theme.RecordlyTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val appContainer = (application as RecordlyApplication).container

        setContent {
            val themePreference by appContainer.preferencesRepository.userPreferencesFlow
                .map { it.theme }
                .collectAsState(initial = "System")

            val dynamicColor by appContainer.preferencesRepository.userPreferencesFlow
                .map { it.dynamicColor }
                .collectAsState(initial = true)

            RecordlyTheme(
                themePreference = themePreference,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecordlyNavGraph(appContainer = appContainer)
                }
            }
        }
    }
}
