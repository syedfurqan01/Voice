package com.feathertech.voice.assistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.util.Log

/**
 * Placeholder RecognitionService required by the voice_interaction_service metadata.
 * Real on-device / system speech recognition will be wired in Phase 2.
 */
class VoiceRecognitionService : RecognitionService() {

    companion object {
        private const val TAG = "VoiceRecognitionService"
    }

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        Log.i(TAG, "onStartListening (Phase 1 stub)")
        // Phase 2 will start the actual recognizer here
        listener?.readyForSpeech(Bundle())
    }

    override fun onCancel(listener: Callback?) {
        Log.i(TAG, "onCancel")
        listener?.endOfSpeech()
    }

    override fun onStopListening(listener: Callback?) {
        Log.i(TAG, "onStopListening")
        listener?.endOfSpeech()
    }
}
