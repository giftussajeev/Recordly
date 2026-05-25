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
import com.recordly.app.data.UserPreferences
import com.recordly.app.di.AppContainer
import com.recordly.app.ui.ViewModelFactory
import com.recordly.app.ui.about.AboutScreen
import com.recordly.app.ui.about.LicensesScreen
import com.recordly.app.ui.about.PrivacyPolicyScreen
import com.recordly.app.ui.about.TermsScreen
import com.recordly.app.ui.dashboard.RecordDashboardScreen
import com.recordly.app.ui.dashboard.RecordViewModel
import com.recordly.app.ui.library.LibraryScreen
import com.recordly.app.ui.library.LibraryViewModel
import com.recordly.app.ui.onboarding.OnboardingScreen
import com.recordly.app.ui.settings.SettingsScreen
import com.recordly.app.ui.settings.SettingsViewModel

// All routes
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_PRIVACY = "privacy"
private const val ROUTE_TERMS = "terms"
private const val ROUTE_LICENSES = "licenses"

private val BOTTOM_NAV_ROUTES = listOf(ROUTE_DASHBOARD, ROUTE_LIBRARY, ROUTE_SETTINGS, ROUTE_ABOUT)

@Composable
fun RecordlyNavGraph(appContainer: AppContainer) {
    val factory = ViewModelFactory(appContainer)
    val recordViewModel: RecordViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    // Read onboarding state — default to false (will show onboarding) until prefs load
    val onboardingComplete by appContainer.preferencesRepository.userPreferencesFlow
        .collectAsState(initial = null)

    // Determine start route based on onboarding state
    // null = not yet loaded (show nothing or splash), false = needs onboarding, true = main app
    var currentRoute by remember { mutableStateOf<String?>(null) }

    // Set initial route once preferences load
    LaunchedEffect(onboardingComplete) {
        if (currentRoute == null && onboardingComplete != null) {
            currentRoute = if (onboardingComplete!!.onboardingComplete) {
                ROUTE_DASHBOARD
            } else {
                ROUTE_ONBOARDING
            }
        }
    }

    val route = currentRoute ?: return // Show nothing while prefs load (avoids flash)

    Scaffold(
        bottomBar = {
            if (route in BOTTOM_NAV_ROUTES) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == ROUTE_DASHBOARD,
                        onClick = { currentRoute = ROUTE_DASHBOARD },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Record") },
                        label = { Text("Record") }
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_LIBRARY,
                        onClick = { currentRoute = ROUTE_LIBRARY },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_SETTINGS,
                        onClick = { currentRoute = ROUTE_SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_ABOUT,
                        onClick = { currentRoute = ROUTE_ABOUT },
                        icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                        label = { Text("About") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (route) {
                ROUTE_ONBOARDING -> OnboardingScreen(
                    onComplete = {
                        settingsViewModel.completeOnboarding()
                        currentRoute = ROUTE_DASHBOARD
                    }
                )
                ROUTE_DASHBOARD -> RecordDashboardScreen(viewModel = recordViewModel)
                ROUTE_LIBRARY -> LibraryScreen(viewModel = libraryViewModel)
                ROUTE_SETTINGS -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onRunSetupAgain = { currentRoute = ROUTE_ONBOARDING }
                )
                ROUTE_ABOUT -> AboutScreen(
                    onNavigateToPrivacy = { currentRoute = ROUTE_PRIVACY },
                    onNavigateToTerms = { currentRoute = ROUTE_TERMS },
                    onNavigateToLicenses = { currentRoute = ROUTE_LICENSES }
                )
                ROUTE_PRIVACY -> PrivacyPolicyScreen(onBack = { currentRoute = ROUTE_ABOUT })
                ROUTE_TERMS -> TermsScreen(onBack = { currentRoute = ROUTE_ABOUT })
                ROUTE_LICENSES -> LicensesScreen(onBack = { currentRoute = ROUTE_ABOUT })
            }
        }
    }
}
