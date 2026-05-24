package com.recordly.app.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Recordly respects your privacy. All screen recordings are processed and stored locally on your device. We do not collect, track, or transmit any video, audio, or usage data to external servers.\n\n" +
                "Permissions:\n" +
                "- Microphone: Used exclusively for recording audio within your videos.\n" +
                "- Screen Capture: Used exclusively to record your screen.\n" +
                "- Storage: Used to save your final MP4 files to your device.\n\n" +
                "Since everything remains on your device, you maintain full ownership and control over your media."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "By using Recordly, you agree to these terms.\n\n" +
                "Recordly is provided \"as is\" without any warranty. We are not responsible for any data loss, performance issues, or legal consequences arising from your screen recordings.\n\n" +
                "Do not use Recordly to capture copyrighted content, illegal material, or private conversations without explicit consent from all parties involved. You are solely responsible for how you use the app and the media it produces."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open-Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Recordly uses the following open-source libraries:\n\n")
            Text("AndroidX / Jetpack Compose\nLicense: Apache 2.0\n\n", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Kotlin Standard Library\nLicense: Apache 2.0\n\n", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Coil (Image Loading)\nLicense: Apache 2.0\n\n", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Material Components for Android\nLicense: Apache 2.0\n", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}
