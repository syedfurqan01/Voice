package com.feathertech.voice.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android system SpeechRecognizer implementation with diagnostics.
 *
 * Important for MIUI / Poco X3 Pro:
 * - Package visibility requires <queries> for RecognitionService.
 * - Prefer explicit Google component when available.
 * - Do not destroy the recognizer on end-of-speech before results arrive.
 * - Surface real error codes (never collapse everything to "Didn't catch that").
 */
class AndroidSpeechRecognitionEngine(
    private val context: Context
) : SpeechRecognitionEngine {

    companion object {
        private const val TAG = "AndroidSpeechEngine"

        // Common recognition providers on Android 12 devices
        private val PREFERRED_PACKAGES = listOf(
            "com.google.android.googlequicksearchbox",
            "com.google.android.tts",
            "com.google.android.as", // Android System Intelligence / on-device
            "com.xiaomi.mibrain.speech" // MIUI (often limited)
        )
    }

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listening = AtomicBoolean(false)
    private var boundProvider: String? = null

    private var onPartial: ((String) -> Unit)? = null
    private var onFinal: ((String) -> Unit)? = null
    private var onError: ((Int, String, Boolean) -> Unit)? = null
    private var onReady: (() -> Unit)? = null
    private var onEndOfSpeech: (() -> Unit)? = null

    override fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    override fun isOnDeviceAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "isOnDeviceRecognitionAvailable failed", e)
                false
            }
        } else {
            false
        }
    }

    override fun requiresNetwork(): Boolean {
        // On-device path does not need network; system online recognizer does.
        // We report true unless the caller explicitly preferred on-device and it is available.
        return true
    }

    override fun providerPackage(): String? = boundProvider

    override fun startListening(
        languageTag: String,
        preferOnDevice: Boolean,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (Int, String, Boolean) -> Unit,
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
                if (listening.get()) {
                    Log.w(TAG, "Already listening — cancelling previous session first")
                    safeCancel()
                }

                destroyRecognizer()

                if (!isAvailable() && !(preferOnDevice && isOnDeviceAvailable())) {
                    onError(
                        SpeechRecognizer.ERROR_CLIENT,
                        "Speech recognition is not available on this device",
                        false
                    )
                    return@post
                }

                val useOnDevice = preferOnDevice && isOnDeviceAvailable()
                recognizer = createRecognizer(useOnDevice)
                if (recognizer == null) {
                    onError(
                        SpeechRecognizer.ERROR_CLIENT,
                        "Could not create SpeechRecognizer",
                        false
                    )
                    return@post
                }

                recognizer!!.setRecognitionListener(listener)

                val intent = buildIntent(languageTag, useOnDevice)
                listening.set(true)
                recognizer!!.startListening(intent)
                Log.i(
                    TAG,
                    "startListening lang=$languageTag onDevice=$useOnDevice provider=$boundProvider"
                )
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed", e)
                listening.set(false)
                onError(
                    SpeechRecognizer.ERROR_CLIENT,
                    "Could not start listening: ${e.message}",
                    true
                )
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                if (listening.get()) {
                    recognizer?.stopListening()
                }
            } catch (e: Exception) {
                Log.w(TAG, "stopListening", e)
            }
        }
    }

    override fun cancel() {
        mainHandler.post { safeCancel() }
    }

    override fun destroy() {
        mainHandler.post {
            safeCancel()
            destroyRecognizer()
            onPartial = null
            onFinal = null
            onError = null
            onReady = null
            onEndOfSpeech = null
        }
    }

    private fun safeCancel() {
        try {
            recognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "cancel", e)
        }
        listening.set(false)
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.setRecognitionListener(null)
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
        boundProvider = null
        listening.set(false)
    }

    private fun createRecognizer(useOnDevice: Boolean): SpeechRecognizer? {
        return try {
            if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                boundProvider = "on-device"
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else {
                // Prefer an explicit Google component when resolvable (helps on MIUI)
                val component = resolvePreferredComponent()
                if (component != null) {
                    boundProvider = component.packageName
                    Log.i(TAG, "Using explicit component $component")
                    SpeechRecognizer.createSpeechRecognizer(appContext, component)
                } else {
                    boundProvider = "default"
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "createRecognizer failed", e)
            null
        }
    }

    private fun resolvePreferredComponent(): ComponentName? {
        val pm = appContext.packageManager
        val intent = Intent(RecognitionServiceIntentAction)
        val services = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentServices(intent, 0)
        }

        if (services.isNullOrEmpty()) {
            Log.w(TAG, "No RecognitionService providers visible (check <queries>)")
            return null
        }

        Log.i(TAG, "Visible RecognitionService providers: ${services.map { it.serviceInfo?.packageName }}")

        for (pkg in PREFERRED_PACKAGES) {
            val match = services.firstOrNull { it.serviceInfo?.packageName == pkg }
            if (match?.serviceInfo != null) {
                return ComponentName(match.serviceInfo.packageName, match.serviceInfo.name)
            }
        }
        // Fall back to first visible provider
        val first = services.firstOrNull()?.serviceInfo ?: return null
        return ComponentName(first.packageName, first.name)
    }

    private fun buildIntent(languageTag: String, useOnDevice: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            // Give the user more time to start / finish speaking
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.i(TAG, "onReadyForSpeech")
            onReady?.invoke()
        }

        override fun onBeginningOfSpeech() {
            Log.i(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.i(TAG, "onEndOfSpeech")
            // Do NOT destroy here — results or error will follow.
            onEndOfSpeech?.invoke()
        }

        override fun onError(error: Int) {
            listening.set(false)
            val (msg, recoverable) = mapError(error)
            Log.w(TAG, "onError code=$error msg=$msg")
            onError?.invoke(error, msg, recoverable)
            // Destroy after error so the next startListening is clean
            mainHandler.post { destroyRecognizer() }
        }

        override fun onResults(results: Bundle?) {
            listening.set(false)
            val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            Log.i(TAG, "onResults texts=$texts confidences=${confidences?.contentToString()}")
            val best = texts?.firstOrNull()?.trim().orEmpty()
            if (best.isNotEmpty()) {
                onFinal?.invoke(best)
            } else {
                onError?.invoke(
                    SpeechRecognizer.ERROR_NO_MATCH,
                    "No speech recognized (empty result list)",
                    true
                )
            }
            mainHandler.post { destroyRecognizer() }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = texts?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                Log.d(TAG, "partial: $partial")
                onPartial?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapError(code: Int): Pair<String, Boolean> = when (code) {
        SpeechRecognizer.ERROR_AUDIO ->
            "Microphone / audio error (code $code)" to true
        SpeechRecognizer.ERROR_CLIENT ->
            "Recognition client error (code $code)" to true
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission required (code $code)" to false
        SpeechRecognizer.ERROR_NETWORK ->
            "Network required for recognition (code $code)" to true
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Network timeout (code $code)" to true
        SpeechRecognizer.ERROR_NO_MATCH ->
            "No match — speech not understood (code $code). Try clearer speech or another language." to true
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "Recognizer busy — try again (code $code)" to true
        SpeechRecognizer.ERROR_SERVER ->
            "Recognition server error (code $code)" to true
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "No speech detected — timed out (code $code)" to true
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
            "Language not supported by this provider (code $code)" to true
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "Language data unavailable (code $code)" to true
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "Recognition server disconnected (code $code)" to true
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
            "Too many requests (code $code)" to true
        else ->
            "Recognition error (code $code)" to true
    }

    // Intent action constant used for package visibility queries
    private val RecognitionServiceIntentAction = "android.speech.RecognitionService"
}
