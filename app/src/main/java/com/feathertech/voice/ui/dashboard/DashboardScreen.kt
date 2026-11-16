package com.feathertech.voice.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.feathertech.voice.R
import com.feathertech.voice.assistant.AssistantStatus
import com.feathertech.voice.ui.theme.VoiceColors

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(AssistantStatus.isDefaultAssistant(context)) }

    // Re-check when the user returns from system settings
    LaunchedEffect(Unit) {
        isDefault = AssistantStatus.isDefaultAssistant(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoiceColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Brand mark
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.displayLarge,
            color = VoiceColors.OnBackground
        )
        Text(
            text = stringResource(R.string.dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = VoiceColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Status card
        StatusCard(
            isDefault = isDefault,
            onSetDefault = {
                // Open the system screen where the user can select the default assistant
                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                context.startActivity(intent)
            },
            onOpenAssistantSettings = {
                // Deep link into our own settings if needed later
                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Phase indicator
        Text(
            text = stringResource(R.string.phase_label),
            style = MaterialTheme.typography.labelMedium,
            color = VoiceColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun StatusCard(
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onOpenAssistantSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VoiceColors.Surface)
            .border(1.dp, VoiceColors.Outline, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDefault) VoiceColors.Success else VoiceColors.Warning)
            )
            Text(
                text = if (isDefault)
                    stringResource(R.string.status_ready)
                else
                    stringResource(R.string.status_not_default),
                style = MaterialTheme.typography.titleMedium,
                color = VoiceColors.OnSurface
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isDefault) {
            Button(
                onClick = onSetDefault,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoiceColors.Primary,
                    contentColor = VoiceColors.OnPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_set_default),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onOpenAssistantSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VoiceColors.OnSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.action_open_settings),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
