package com.feathertech.voice.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.feathertech.voice.speech.SpeechState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Phase 2 session: minimal listening overlay with real STT + TTS.
 * No permanent floating bubble. Resources released on hide/destroy.
 */
class VoiceAssistantSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "VoiceAssistantSession"
        private const val BG = 0xF00A0A0B.toInt()
        private const val SURFACE = 0xFF141416.toInt()
        private const val TEXT_PRIMARY = 0xFFE8E8EA.toInt()
        private const val TEXT_SECONDARY = 0xFF9A9A9F.toInt()
        private const val ACCENT = 0xFF7C8AFF.toInt()
        private const val SUCCESS = 0xFF4ADE80.toInt()
        private const val ERROR = 0xFFF87171.toInt()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: AssistantSessionController? = null

    private lateinit var statusView: TextView
    private lateinit var transcriptView: TextView
    private lateinit var hintView: TextView
    private lateinit var networkNote: TextView

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Session created")
        controller = AssistantSessionController(context, scope)
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "Session shown")

        val root = buildUi()
        setContentView(root)

        controller?.setStateListener { state -> updateUi(state) }

        if (hasMicPermission()) {
            controller?.startListening()
        } else {
            statusView.text = "Microphone permission needed"
            statusView.setTextColor(ERROR)
            transcriptView.text = "Grant microphone access in system settings, then invoke Voice again."
            hintView.text = "Tap outside or press back to dismiss"
        }
    }

    override fun onHide() {
        Log.i(TAG, "Session hidden")
        controller?.dismiss()
        super.onHide()
    }

    override fun onDestroy() {
        Log.i(TAG, "Session destroyed")
        controller?.destroy()
        controller = null
        scope.cancel()
        super.onDestroy()
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun buildUi(): View {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(32), dp(28), dp(32))
            background = GradientDrawable().apply {
                setColor(SURFACE)
                cornerRadius = dp(20).toFloat()
            }
            elevation = dp(8).toFloat()
        }

        statusView = TextView(context).apply {
            text = "Listening…"
            setTextColor(ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        card.addView(statusView)

        transcriptView = TextView(context).apply {
            text = ""
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(8))
            minHeight = dp(48)
        }
        card.addView(transcriptView)

        networkNote = TextView(context).apply {
            text = if (controller?.requiresNetwork() == true)
                "Recognition uses the system service (network required)"
            else ""
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        card.addView(networkNote)

        hintView = TextView(context).apply {
            text = "Say a command · back to dismiss"
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        }
        card.addView(hintView)

        val outer = FrameLayout(context).apply {
            setBackgroundColor(BG)
            setOnClickListener { hide() } // tap outside card dismisses
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            marginStart = dp(24)
            marginEnd = dp(24)
        }
        // Prevent card clicks from dismissing
        card.setOnClickListener { /* consume */ }
        outer.addView(card, lp)
        return outer
    }

    private fun updateUi(state: SpeechState) {
        when (state) {
            is SpeechState.Idle -> {
                statusView.text = "Ready"
                statusView.setTextColor(TEXT_SECONDARY)
                transcriptView.text = ""
                hintView.text = "Invoke again to listen"
            }
            is SpeechState.RequestingPermission -> {
                statusView.text = "Permission needed"
                statusView.setTextColor(ERROR)
            }
            is SpeechState.Listening -> {
                statusView.text = "Listening…"
                statusView.setTextColor(ACCENT)
                transcriptView.text = ""
            }
            is SpeechState.PartialResult -> {
                statusView.text = "Listening…"
                statusView.setTextColor(ACCENT)
                transcriptView.text = state.text
            }
            is SpeechState.Processing -> {
                statusView.text = "Processing…"
                statusView.setTextColor(TEXT_SECONDARY)
            }
            is SpeechState.FinalResult -> {
                statusView.text = "Got it"
                statusView.setTextColor(SUCCESS)
                transcriptView.text = state.text
            }
            is SpeechState.Speaking -> {
                statusView.text = "Speaking…"
                statusView.setTextColor(ACCENT)
                transcriptView.text = state.text
            }
            is SpeechState.Error -> {
                statusView.text = "Error"
                statusView.setTextColor(ERROR)
                transcriptView.text = state.message
                hintView.text = if (state.recoverable) "Try again" else "Check settings"
            }
        }
    }
}
