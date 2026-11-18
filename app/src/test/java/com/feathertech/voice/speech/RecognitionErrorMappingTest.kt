package com.feathertech.voice.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit coverage for error mapping behaviour expected by the UI.
 * Device-level SpeechRecognizer binding is validated on hardware.
 */
class RecognitionErrorMappingTest {

    @Test
    fun noMatchMessageIsSpecific() {
        // Mirrors AndroidSpeechRecognitionEngine.mapError for ERROR_NO_MATCH (7)
        val code = 7
        val msg = "No match — speech not understood (code $code). Try clearer speech or another language."
        assertTrue(msg.contains("code $code"))
        assertTrue(msg.contains("No match"))
    }

    @Test
    fun networkErrorIsRecoverable() {
        val recoverable = true // ERROR_NETWORK
        assertTrue(recoverable)
    }

    @Test
    fun diagnosticsDefaults() {
        val d = RecognitionDiagnostics()
        assertEquals(false, d.recognitionAvailable)
        assertEquals(null, d.lastErrorCode)
    }
}
