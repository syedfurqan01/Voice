package com.feathertech.voice.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * The system binds to this service when the user selects Voice
 * as the default digital assistant.
 *
 * Phase 1 only registers the service. Real speech + LLM handling
 * begins in Phase 2 / Phase 3.
 */
class VoiceAssistantService : VoiceInteractionService() {

    companion object {
        private const val TAG = "VoiceAssistantService"
    }

    override fun onReady() {
        super.onReady()
        Log.i(TAG, "Voice assistant service is ready")
    }

    override fun onShutdown() {
        Log.i(TAG, "Voice assistant service shutting down")
        super.onShutdown()
    }

    override fun onGetSupportedVoiceActions(voiceActions: MutableSet<String>): MutableSet<String> {
        // Phase 1: no special voice actions yet
        return super.onGetSupportedVoiceActions(voiceActions)
    }
}
