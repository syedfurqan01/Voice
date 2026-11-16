package com.feathertech.voice

import android.app.Application

/**
 * Application entry point.
 * Future phases will initialize local model runtime, accessibility helpers, etc. here.
 */
class VoiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Phase 0/1: nothing heavy yet
    }
}
