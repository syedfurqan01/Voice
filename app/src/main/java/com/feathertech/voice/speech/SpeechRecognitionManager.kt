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

    private val _diagnostics = MutableStateFlow(RecognitionDiagnostics())
    val diagnostics: StateFlow<RecognitionDiagnostics> = _diagnostics.asStateFlow()

    val requiresNetwork: Boolean get() = engine.requiresNetwork()
    val isAvailable: Boolean get() = engine.isAvailable()
    val isOnDeviceAvailable: Boolean get() = engine.isOnDeviceAvailable()

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun refreshDiagnostics(languageTag: String) {
        _diagnostics.value = RecognitionDiagnostics(
            recognitionAvailable = engine.isAvailable(),
            onDeviceAvailable = engine.isOnDeviceAvailable(),
            providerPackage = engine.providerPackage(),
            languageTag = languageTag,
            lastErrorCode = _diagnostics.value.lastErrorCode,
            lastErrorMessage = _diagnostics.value.lastErrorMessage
        )
    }

    fun start(languageTag: String, preferOnDevice: Boolean = false) {
        if (!hasMicrophonePermission()) {
            _state.value = SpeechState.RequestingPermission
            return
        }
        if (!engine.isAvailable() && !(preferOnDevice && engine.isOnDeviceAvailable())) {
            _state.value = SpeechState.Error(
                "Speech recognition unavailable on this device",
                recoverable = false
            )
            return
        }

        refreshDiagnostics(languageTag)
        _state.value = SpeechState.Listening

        engine.startListening(
            languageTag = languageTag,
            preferOnDevice = preferOnDevice,
            onPartial = { text ->
                _state.value = SpeechState.PartialResult(text)
            },
            onFinal = { text ->
                _diagnostics.value = _diagnostics.value.copy(
                    lastErrorCode = null,
                    lastErrorMessage = null,
                    providerPackage = engine.providerPackage()
                )
                _state.value = SpeechState.FinalResult(text)
            },
            onError = { code, msg, recoverable ->
                Log.w(TAG, "Recognition error $code: $msg")
                _diagnostics.value = _diagnostics.value.copy(
                    lastErrorCode = code,
                    lastErrorMessage = msg,
                    providerPackage = engine.providerPackage()
                )
                _state.value = SpeechState.Error(msg, recoverable)
            },
            onReady = {
                Log.i(TAG, "Recognizer ready provider=${engine.providerPackage()}")
                _diagnostics.value = _diagnostics.value.copy(
                    providerPackage = engine.providerPackage()
                )
            },
            onEndOfSpeech = {
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

/**
 * Live diagnostics shown in the session UI so device testing is transparent.
 */
data class RecognitionDiagnostics(
    val recognitionAvailable: Boolean = false,
    val onDeviceAvailable: Boolean = false,
    val providerPackage: String? = null,
    val languageTag: String = "",
    val lastErrorCode: Int? = null,
    val lastErrorMessage: String? = null
)
