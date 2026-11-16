package com.feathertech.voice.assistant

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.feathertech.voice.R

/**
 * Minimal session shown when the assistant is invoked.
 * Phase 1 only displays a temporary status overlay.
 * Real conversation UI + speech pipeline arrives in later phases.
 *
 * No permanent floating bubble is created.
 */
class VoiceAssistantSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "VoiceAssistantSession"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Session created")
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "Session shown, flags=$showFlags")

        // Temporary minimal overlay only while the session is active
        val root = FrameLayout(context).apply {
            setBackgroundColor(0xE60A0A0B.toInt()) // semi-transparent dark
            setPadding(48, 48, 48, 48)
        }

        val label = TextView(context).apply {
            text = "Voice is listening…\n(Phase 1 foundation)"
            setTextColor(0xFFE8E8EA.toInt())
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }

        root.addView(label, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)
    }

    override fun onHide() {
        Log.i(TAG, "Session hidden")
        super.onHide()
    }

    override fun onDestroy() {
        Log.i(TAG, "Session destroyed")
        super.onDestroy()
    }
}
