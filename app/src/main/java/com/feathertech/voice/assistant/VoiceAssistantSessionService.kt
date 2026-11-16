package com.feathertech.voice.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log

/**
 * Creates a VoiceInteractionSession when the assistant is invoked
 * (long-press home, assist gesture, or voice key).
 */
class VoiceAssistantSessionService : VoiceInteractionSessionService() {

    companion object {
        private const val TAG = "VoiceSessionService"
    }

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.i(TAG, "Creating new VoiceInteractionSession")
        return VoiceAssistantSession(this)
    }
}
