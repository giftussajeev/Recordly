package com.recordly.app.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recordly.app.R

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_round),
            contentDescription = "Recordly Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Recordly",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "A clean, powerful screen recorder for Android.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Version 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("App: Recordly")
                Text("Version: 1.0")
                Text("Package: com.recordly.app")
                Text("Minimum Android: Android 8.0+")
                Text("License: Apache License 2.0")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("GitHub: github.com/GiftusSajeev/Recordly")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Play Store: Coming soon")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Privacy Policy")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Terms & Conditions")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Open-source licenses")
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Vibecoded with love by Giftus Sajeev and Sanjith KS.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Made with ChatGPT, Gemini Pro 3.1, and Claude Opus 4.7.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
