package com.chibicompanion

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/** Chooses a good installed voice without relying on paid cloud speech services. */
object CompanionVoice {
    private const val PREFS = "companion_voice"
    private const val KEY = "voice_name"

    fun options(tts: TextToSpeech): List<Voice> =
        tts.voices.orEmpty()
            .filter { it.locale.language == "en" }
            .sortedWith(
                compareByDescending<Voice> { soundsFeminine(it) }
                    .thenByDescending { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name }
            )

    private fun soundsFeminine(voice: Voice): Boolean {
        val name = voice.name.lowercase(Locale.ROOT)
        return name.contains("female") || name.contains("woman") ||
            name.contains("f1") || name.contains("feminine")
    }

    fun label(voice: Voice): String {
        val locale = voice.locale.getDisplayName(Locale.US)
        val style = if (soundsFeminine(voice)) "Feminine-style" else "Voice"
        val quality = when (voice.quality) {
            Voice.QUALITY_HIGH -> "High quality"
            Voice.QUALITY_NORMAL -> "Standard"
            else -> "Basic"
        }
        val connection = if (voice.isNetworkConnectionRequired) "Online" else "On-device"
        return "$locale · $style · $quality · $connection"
    }

    fun selectedName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun select(context: Context, tts: TextToSpeech, voice: Voice) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, voice.name).apply()
        runCatching { tts.voice = voice }
        tune(tts)
    }

    fun configure(context: Context, tts: TextToSpeech) {
        val voices = options(tts)
        val saved = selectedName(context)
        val selected = voices.firstOrNull { it.name == saved } ?: voices.firstOrNull()
        if (selected != null) runCatching { tts.voice = selected }
        else runCatching { tts.language = Locale.US }
        tune(tts)
    }

    private fun tune(tts: TextToSpeech) {
        // A slightly slower, warmer delivery; the installed voice still determines naturalness.
        tts.setSpeechRate(0.91f)
        tts.setPitch(1.16f)
    }
}
