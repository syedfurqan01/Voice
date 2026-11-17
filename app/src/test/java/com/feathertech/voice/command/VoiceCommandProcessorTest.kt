package com.feathertech.voice.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests — no Android runtime required.
 */
class VoiceCommandProcessorTest {

    private var settingsOpened = false
    private val processor = VoiceCommandProcessor(
        context = null,
        openSettings = { settingsOpened = true },
        timeProvider = { "It's 3:45 PM." }
    )

    @Test
    fun helloVoice_returnsSpeak() {
        val r = processor.process("Hello Voice")
        assertTrue(r is CommandResult.Speak)
        assertTrue((r as CommandResult.Speak).reply.contains("Voice", ignoreCase = true))
    }

    @Test
    fun whatIsYourName_returnsSpeak() {
        assertTrue(processor.process("What is your name?") is CommandResult.Speak)
    }

    @Test
    fun whatCanYouDo_returnsSpeak() {
        assertTrue(processor.process("What can you do") is CommandResult.Speak)
    }

    @Test
    fun whatTimeIsIt_returnsSpeak() {
        val r = processor.process("What time is it?")
        assertTrue(r is CommandResult.Speak)
        assertEquals("It's 3:45 PM.", (r as CommandResult.Speak).reply)
    }

    @Test
    fun openSettings_hasAction() {
        settingsOpened = false
        val r = processor.process("Open settings")
        assertTrue(r is CommandResult.Speak)
        (r as CommandResult.Speak).action?.invoke()
        assertTrue(settingsOpened)
    }

    @Test
    fun stopSpeaking() {
        assertTrue(processor.process("Stop speaking") is CommandResult.StopSpeaking)
    }

    @Test
    fun stopListening() {
        assertTrue(processor.process("Stop listening") is CommandResult.StopListening)
    }

    @Test
    fun unknown() {
        val r = processor.process("fly me to the moon")
        assertTrue(r is CommandResult.Unknown)
        assertEquals("fly me to the moon", (r as CommandResult.Unknown).original)
    }

    @Test
    fun caseAndPunctuationInsensitive() {
        assertTrue(processor.process("HELLO VOICE!!!") is CommandResult.Speak)
        assertTrue(processor.process("  what   time  is  it  ") is CommandResult.Speak)
    }
}
