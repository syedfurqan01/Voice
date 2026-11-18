package com.feathertech.voice.speech

/**
 * Abstraction over speech-to-text engines.
 * Phase 2 uses Android SpeechRecognizer.
 * Future phases can supply a Whisper.cpp implementation without changing callers.
 */
interface SpeechRecognitionEngine {

    fun isAvailable(): Boolean

    /** API 31+ on-device recognizer availability. */
    fun isOnDeviceAvailable(): Boolean

    /**
     * Whether the current engine requires network connectivity.
     * Always return the truthful value — never claim offline when the service needs the network.
     */
    fun requiresNetwork(): Boolean

    /** Best-effort package name of the bound recognition provider, if known. */
    fun providerPackage(): String?

    fun startListening(
        languageTag: String,
        preferOnDevice: Boolean = false,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (code: Int, message: String, recoverable: Boolean) -> Unit,
        onReady: () -> Unit = {},
        onEndOfSpeech: () -> Unit = {}
    )

    fun stopListening()

    fun cancel()

    fun destroy()
}
