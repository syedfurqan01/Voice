package com.feathertech.voice.speech

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "voice_language_prefs"
)

/**
 * Persists the user's preferred recognition / TTS language.
 * Defaults to English (India) which works well on the target device.
 */
class LanguagePreferences(private val context: Context) {

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        const val DEFAULT = "en-IN"
        val SUPPORTED = listOf(
            "en-IN" to "English (India)",
            "en-US" to "English (US)",
            "hi-IN" to "Hindi"
        )
    }

    val languageTag: Flow<String> = context.languageDataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: DEFAULT
    }

    suspend fun setLanguage(tag: String) {
        context.languageDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = tag
        }
    }
}
