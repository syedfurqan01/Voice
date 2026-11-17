package com.feathertech.voice.speech

/**
 * Abstraction over speech-to-text engines.
 * Phase 2 uses Android SpeechRecognizer.
 * Future phases can supply a Whisper.cpp implementation without changing callers.
 */
interface SpeechRecognitionEngine {

    fun isAvailable(): Boolean

    /**
     * Whether the current engine requires network connectivity.
     * Always return the truthful value — never claim offline when the service needs the network.
     */
    fun requiresNetwork(): Boolean

    fun startListening(
        languageTag: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String, Boolean) -> Unit,
        onReady: () -> Unit = {},
        onEndOfSpeech: () -> Unit = {}
    )

    fun stopListening()

    fun cancel()

    fun destroy()
}
