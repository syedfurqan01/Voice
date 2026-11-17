package com.feathertech.voice.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Thin wrapper around Android TextToSpeech.
 * Supports Hindi and English when voices are installed.
 * Prevents duplicate consecutive utterances and cleans up properly.
 */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val DEFAULT_RATE = 1.0f
        private const val DEFAULT_PITCH = 1.0f
    }

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var speechRate = DEFAULT_RATE
    private var speechPitch = DEFAULT_PITCH
    private val lastSpoken = AtomicReference<String?>(null)
    private var onDone: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onDone?.invoke()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onError?.invoke("TTS error")
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    onError?.invoke("TTS error ($errorCode)")
                }
            })
            Log.i(TAG, "TTS initialized")
        } else {
            Log.e(TAG, "TTS init failed status=$status")
        }
    }

    fun isReady(): Boolean = ready

    fun setLanguage(languageTag: String): Boolean {
        if (!ready) return false
        val locale = localeFromTag(languageTag)
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        val ok = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ok) {
            // Fallback to English
            tts?.setLanguage(Locale.ENGLISH)
        }
        return ok
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(speechPitch)
    }

    /**
     * Speak [text]. Skips if it is identical to the last spoken utterance
     * (prevents accidental double-speak).
     */
    fun speak(
        text: String,
        languageTag: String = "en-IN",
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onDone?.invoke()
            return
        }
        if (trimmed == lastSpoken.get()) {
            Log.i(TAG, "Skipping duplicate utterance")
            onDone?.invoke()
            return
        }
        if (!ready) {
            onError?.invoke("Text-to-speech not ready")
            return
        }

        this.onDone = onDone
        this.onError = onError
        setLanguage(languageTag)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)

        val utteranceId = UUID.randomUUID().toString()
        lastSpoken.set(trimmed)
        val params = Bundle()
        val result = tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            onError?.invoke("Failed to start speech")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "stop", e)
        }
        lastSpoken.set(null)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "shutdown", e)
        }
        tts = null
        ready = false
        lastSpoken.set(null)
        onDone = null
        onError = null
    }

    private fun localeFromTag(tag: String): Locale {
        return when {
            tag.startsWith("hi", ignoreCase = true) -> Locale("hi", "IN")
            tag.startsWith("en-IN", ignoreCase = true) -> Locale("en", "IN")
            tag.startsWith("en", ignoreCase = true) -> Locale.ENGLISH
            else -> Locale.forLanguageTag(tag.replace('_', '-'))
        }
    }
}
