package com.feathertech.voice.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lifecycle-aware façade over [SpeechRecognitionEngine].
 * Owns the [SpeechState] that the session UI observes.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val engine: SpeechRecognitionEngine = AndroidSpeechRecognitionEngine(context)
) {

    companion object {
        private const val TAG = "SpeechRecognitionMgr"
    }

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    val requiresNetwork: Boolean get() = engine.requiresNetwork()
    val isAvailable: Boolean get() = engine.isAvailable()

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(languageTag: String) {
        if (!hasMicrophonePermission()) {
            _state.value = SpeechState.RequestingPermission
            return
        }
        if (!engine.isAvailable()) {
            _state.value = SpeechState.Error("Speech recognition unavailable", recoverable = false)
            return
        }

        _state.value = SpeechState.Listening
        engine.startListening(
            languageTag = languageTag,
            onPartial = { text ->
                _state.value = SpeechState.PartialResult(text)
            },
            onFinal = { text ->
                _state.value = SpeechState.FinalResult(text)
            },
            onError = { msg, recoverable ->
                _state.value = SpeechState.Error(msg, recoverable)
            },
            onReady = {
                Log.i(TAG, "Recognizer ready")
            },
            onEndOfSpeech = {
                // Transition to processing only if we don't already have a final result
                if (_state.value is SpeechState.Listening || _state.value is SpeechState.PartialResult) {
                    _state.value = SpeechState.Processing
                }
            }
        )
    }

    fun stop() {
        engine.stopListening()
        if (_state.value is SpeechState.Listening || _state.value is SpeechState.PartialResult) {
            _state.value = SpeechState.Idle
        }
    }

    fun cancel() {
        engine.cancel()
        _state.value = SpeechState.Idle
    }

    fun markProcessing() {
        _state.value = SpeechState.Processing
    }

    fun markSpeaking(text: String) {
        _state.value = SpeechState.Speaking(text)
    }

    fun markIdle() {
        _state.value = SpeechState.Idle
    }

    fun markError(message: String, recoverable: Boolean = true) {
        _state.value = SpeechState.Error(message, recoverable)
    }

    fun destroy() {
        engine.destroy()
        _state.value = SpeechState.Idle
    }
}
