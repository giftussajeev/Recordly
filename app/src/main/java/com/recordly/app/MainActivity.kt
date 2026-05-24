package com.recordly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.recordly.app.ui.theme.RecordlyTheme
import com.recordly.app.ui.navigation.RecordlyNavGraph

import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize AppContainer (Manual DI)
        val appContainer = (application as RecordlyApplication).container

        setContent {
            RecordlyTheme {
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
