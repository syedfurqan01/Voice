package com.feathertech.voice.assistant

import android.content.Context
import android.provider.Settings
import android.service.voice.VoiceInteractionService

/**
 * Helper to detect whether Voice is currently the default digital assistant.
 */
object AssistantStatus {

    /**
     * Returns true if the currently selected voice interaction service
     * belongs to this package.
     */
    fun isDefaultAssistant(context: Context): Boolean {
        return try {
            val current = Settings.Secure.getString(
                context.contentResolver,
                "voice_interaction_service"
            )
            // Format is usually "package/component"
            current?.startsWith(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }
}
