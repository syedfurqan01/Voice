package com.feathertech.voice.speech

/**
 * Finite state machine for the speech pipeline.
 * Used by the session UI and the recognition/TTS managers.
 */
sealed class SpeechState {
    data object Idle : SpeechState()
    data object RequestingPermission : SpeechState()
    data object Listening : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data object Processing : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Speaking(val text: String) : SpeechState()
    data class Error(val message: String, val recoverable: Boolean = true) : SpeechState()
}
