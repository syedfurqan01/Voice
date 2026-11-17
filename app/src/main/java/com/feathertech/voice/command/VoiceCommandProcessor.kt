package com.feathertech.voice.command

import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure command matching and response generation.
 * No dependency on speech recognition or TTS — easy to unit-test.
 */
class VoiceCommandProcessor(
    private val context: Context? = null,
    private val openSettings: () -> Unit = {
        context?.let { ctx ->
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    },
    private val timeProvider: () -> String = {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        "It's ${fmt.format(Date())}."
    }
) {

    fun process(raw: String): CommandResult {
        val text = normalize(raw)
        if (text.isEmpty()) return CommandResult.Unknown(raw)

        return when {
            matches(text, "hello voice", "hi voice", "hey voice", "hello", "hi") ->
                CommandResult.Speak("Hello. I'm Voice, your local assistant.")

            matches(text, "what is your name", "what's your name", "who are you") ->
                CommandResult.Speak("My name is Voice, by Spark, from FeatherTech.")

            matches(text, "what can you do", "help", "capabilities") ->
                CommandResult.Speak(
                    "Right now I can greet you, tell the time, open settings, " +
                    "and stop speaking or listening. More capabilities are coming soon."
                )

            matches(text, "what time is it", "what's the time", "tell me the time", "time") ->
                CommandResult.Speak(timeProvider())

            matches(text, "open settings", "settings") ->
                CommandResult.Speak("Opening settings.") { openSettings() }

            matches(text, "stop speaking", "be quiet", "shut up", "stop talking") ->
                CommandResult.StopSpeaking

            matches(text, "stop listening", "cancel", "never mind") ->
                CommandResult.StopListening

            else -> CommandResult.Unknown(raw)
        }
    }

    private fun normalize(s: String): String =
        s.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s']"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun matches(text: String, vararg phrases: String): Boolean =
        phrases.any { phrase ->
            text == phrase || text.startsWith("$phrase ") || text.endsWith(" $phrase") || text.contains(" $phrase ")
        }
}
