package com.feathertech.voice.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Android system SpeechRecognizer implementation.
 * On most devices (including Poco X3 Pro) this engine requires network access.
 * The status is reported honestly via [requiresNetwork].
 */
class AndroidSpeechRecognitionEngine(
    private val context: Context
) : SpeechRecognitionEngine {

    companion object {
        private const val TAG = "AndroidSpeechEngine"
    }

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var onPartial: ((String) -> Unit)? = null
    private var onFinal: ((String) -> Unit)? = null
    private var onError: ((String, Boolean) -> Unit)? = null
    private var onReady: (() -> Unit)? = null
    private var onEndOfSpeech: (() -> Unit)? = null

    override fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    override fun requiresNetwork(): Boolean = true // System recognizer on this device class is online

    override fun startListening(
        languageTag: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String, Boolean) -> Unit,
        onReady: () -> Unit,
        onEndOfSpeech: () -> Unit
    ) {
        this.onPartial = onPartial
        this.onFinal = onFinal
        this.onError = onError
        this.onReady = onReady
        this.onEndOfSpeech = onEndOfSpeech

        mainHandler.post {
            try {
                destroyRecognizer()
                if (!isAvailable()) {
                    onError("Speech recognition is not available on this device", false)
                    return@post
                }
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
                    sr.setRecognitionListener(listener)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    }
                    sr.startListening(intent)
                    Log.i(TAG, "Started listening language=$languageTag")
                }
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed", e)
                onError("Could not start listening: ${e.message}", true)
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "stopListening", e)
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                recognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "cancel", e)
            }
        }
    }

    override fun destroy() {
        mainHandler.post { destroyRecognizer() }
        onPartial = null
        onFinal = null
        onError = null
        onReady = null
        onEndOfSpeech = null
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onReady?.invoke()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            onEndOfSpeech?.invoke()
        }

        override fun onError(error: Int) {
            val (msg, recoverable) = mapError(error)
            Log.w(TAG, "Recognition error $error: $msg")
            onError?.invoke(msg, recoverable)
        }

        override fun onResults(results: Bundle?) {
            val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = texts?.firstOrNull()?.trim().orEmpty()
            if (best.isNotEmpty()) {
                onFinal?.invoke(best)
            } else {
                onError?.invoke("No speech recognized", true)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = texts?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                onPartial?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapError(code: Int): Pair<String, Boolean> = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error" to true
        SpeechRecognizer.ERROR_CLIENT -> "Recognition client error" to true
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required" to false
        SpeechRecognizer.ERROR_NETWORK -> "Network required for recognition" to true
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout" to true
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that" to true
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, try again" to true
        SpeechRecognizer.ERROR_SERVER -> "Recognition server error" to true
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected" to true
        else -> "Recognition error ($code)" to true
    }
}
