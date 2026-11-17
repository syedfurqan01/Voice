package com.feathertech.voice.command

/**
 * Result of processing a spoken command.
 */
sealed class CommandResult {
    /** Speak this reply (and optionally perform a side-effect). */
    data class Speak(val reply: String, val action: (() -> Unit)? = null) : CommandResult()

    /** Perform an action without speaking (or with minimal feedback). */
    data class ActionOnly(val action: () -> Unit, val feedback: String? = null) : CommandResult()

    /** Stop TTS if speaking. */
    data object StopSpeaking : CommandResult()

    /** Stop recognition and end the listening turn. */
    data object StopListening : CommandResult()

    /** No matching command — optional fallback reply. */
    data class Unknown(val original: String) : CommandResult()
}
