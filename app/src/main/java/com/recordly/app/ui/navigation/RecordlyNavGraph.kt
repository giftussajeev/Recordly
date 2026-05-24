package com.recordly.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recordly.app.di.AppContainer
import com.recordly.app.ui.ViewModelFactory
import com.recordly.app.ui.dashboard.RecordDashboardScreen
import com.recordly.app.ui.dashboard.RecordViewModel
import com.recordly.app.ui.library.LibraryScreen
import com.recordly.app.ui.library.LibraryViewModel
import com.recordly.app.ui.settings.SettingsScreen
import com.recordly.app.ui.settings.SettingsViewModel
import com.recordly.app.ui.about.AboutScreen
import com.recordly.app.ui.about.PrivacyPolicyScreen
import com.recordly.app.ui.about.TermsScreen
import com.recordly.app.ui.about.LicensesScreen

@Composable
fun RecordlyNavGraph(appContainer: AppContainer) {
    val factory = ViewModelFactory(appContainer)
    val recordViewModel: RecordViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    var currentRoute by remember { mutableStateOf("dashboard") }

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf("dashboard", "library", "settings", "about")) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = { currentRoute = "dashboard" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Record") },
                        label = { Text("Record") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "library",
                        onClick = { currentRoute = "library" },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { currentRoute = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "about",
                        onClick = { currentRoute = "about" },
                        icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                        label = { Text("About") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                "dashboard" -> RecordDashboardScreen(viewModel = recordViewModel)
                "library" -> LibraryScreen(viewModel = libraryViewModel)
                "settings" -> SettingsScreen(viewModel = settingsViewModel)
                "about" -> AboutScreen(
                    onNavigateToPrivacy = { currentRoute = "privacy" },
                    onNavigateToTerms = { currentRoute = "terms" },
                    onNavigateToLicenses = { currentRoute = "licenses" }
                )
                "privacy" -> PrivacyPolicyScreen(onBack = { currentRoute = "about" })
                "terms" -> TermsScreen(onBack = { currentRoute = "about" })
                "licenses" -> LicensesScreen(onBack = { currentRoute = "about" })
            }
        }
    }
}
