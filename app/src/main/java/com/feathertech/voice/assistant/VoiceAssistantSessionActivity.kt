package com.feathertech.voice.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.feathertech.voice.ui.theme.VoiceColors
import com.feathertech.voice.ui.theme.VoiceTheme

/**
 * Activity that can be launched via ACTION_ASSIST.
 * Keeps the surface extremely minimal for Phase 1.
 */
class VoiceAssistantSessionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VoiceColors.Background.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Voice",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VoiceColors.OnBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Assistant session (Phase 1)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VoiceColors.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
