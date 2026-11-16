package com.feathertech.voice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.feathertech.voice.ui.dashboard.DashboardScreen
import com.feathertech.voice.ui.theme.VoiceTheme
import com.feathertech.voice.ui.theme.VoiceColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoiceColors.Background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}
