package com.feathertech.voice.assistant

import android.content.Context
import android.util.Log
import com.feathertech.voice.command.CommandResult
import com.feathertech.voice.command.VoiceCommandProcessor
import com.feathertech.voice.speech.LanguagePreferences
import com.feathertech.voice.speech.SpeechRecognitionManager
import com.feathertech.voice.speech.SpeechState
import com.feathertech.voice.speech.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Orchestrates recognition → command processing → TTS for one assistant session.
 * Owned by [VoiceAssistantSession].
 */
class AssistantSessionController(
    private val context: Context,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "AssistantSessionCtrl"
    }

    private val recognition = SpeechRecognitionManager(context)
    private val tts = TextToSpeechManager(context)
    private val commands = VoiceCommandProcessor(context)
    private val languagePrefs = LanguagePreferences(context)

    private var languageJob: Job? = null
    private var currentLanguage = LanguagePreferences.DEFAULT
    private var onStateChanged: ((SpeechState) -> Unit)? = null

    val speechState: StateFlow<SpeechState> = recognition.state

    fun setStateListener(listener: (SpeechState) -> Unit) {
        onStateChanged = listener
        // Also collect flow into the callback
        languageJob?.cancel()
        languageJob = scope.launch {
            recognition.state.collect { state ->
                onStateChanged?.invoke(state)
                if (state is SpeechState.FinalResult) {
                    handleFinalResult(state.text)
                }
            }
        }
        scope.launch {
            currentLanguage = languagePrefs.languageTag.first()
        }
    }

    fun startListening() {
        if (!recognition.hasMicrophonePermission()) {
            recognition.markError("Microphone permission required", recoverable = false)
            return
        }
        recognition.start(currentLanguage)
    }

    fun stopListening() {
        recognition.stop()
    }

    fun stopSpeaking() {
        tts.stop()
        recognition.markIdle()
    }

    fun dismiss() {
        recognition.cancel()
        tts.stop()
        recognition.markIdle()
    }

    fun destroy() {
        languageJob?.cancel()
        recognition.destroy()
        tts.shutdown()
        onStateChanged = null
    }

    fun requiresNetwork(): Boolean = recognition.requiresNetwork

    private fun handleFinalResult(text: String) {
        Log.i(TAG, "Final result: $text")
        recognition.markProcessing()
        val result = commands.process(text)
        when (result) {
            is CommandResult.Speak -> {
                result.action?.invoke()
                recognition.markSpeaking(result.reply)
                tts.speak(
                    text = result.reply,
                    languageTag = currentLanguage,
                    onDone = { recognition.markIdle() },
                    onError = { msg -> recognition.markError(msg) }
                )
            }
            is CommandResult.ActionOnly -> {
                result.action()
                result.feedback?.let { feedback ->
                    recognition.markSpeaking(feedback)
                    tts.speak(feedback, currentLanguage, onDone = { recognition.markIdle() })
                } ?: recognition.markIdle()
            }
            is CommandResult.StopSpeaking -> {
                tts.stop()
                recognition.markIdle()
            }
            is CommandResult.StopListening -> {
                recognition.cancel()
                recognition.markIdle()
            }
            is CommandResult.Unknown -> {
                val reply = "I heard \"$text\". I don't have a command for that yet."
                recognition.markSpeaking(reply)
                tts.speak(reply, currentLanguage, onDone = { recognition.markIdle() })
            }
        }
    }
}
