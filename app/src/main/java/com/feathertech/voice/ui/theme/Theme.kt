package com.feathertech.voice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoiceDarkColorScheme = darkColorScheme(
    primary = VoiceColors.Primary,
    onPrimary = VoiceColors.OnPrimary,
    background = VoiceColors.Background,
    onBackground = VoiceColors.OnBackground,
    surface = VoiceColors.Surface,
    onSurface = VoiceColors.OnSurface,
    surfaceVariant = VoiceColors.SurfaceVariant,
    onSurfaceVariant = VoiceColors.OnSurfaceVariant,
    outline = VoiceColors.Outline,
    error = VoiceColors.Error
)

@Composable
fun VoiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoiceDarkColorScheme,
        typography = VoiceTypography,
        content = content
    )
}
