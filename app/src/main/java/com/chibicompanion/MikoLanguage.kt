package com.chibicompanion

import android.content.Context

/**
 * User-selectable response style. Romaji is ordinary Japanese written with Latin letters;
 * interpretation is delegated to the configured multilingual model rather than a brittle
 * hard-coded phrase dictionary.
 */
object MikoLanguage {
    const val NATURAL = "natural"
    const val ROMAJI = "romaji"
    const val ENGLISH = "english"
    private const val PREFS = "miko_language"
    private const val KEY = "reply_style"

    fun selected(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, NATURAL) ?: NATURAL

    fun set(context: Context, value: String) {
        val safe = value.takeIf { it in setOf(NATURAL, ROMAJI, ENGLISH) } ?: NATURAL
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, safe).apply()
    }

    fun label(value: String): String = when (value) {
        ROMAJI -> "Japanese / Romaji"
        ENGLISH -> "English"
        else -> "Natural bilingual"
    }

    fun instructions(context: Context): String {
        val style = when (selected(context)) {
            ROMAJI -> "Reply mainly in short, natural Japanese written in Romaji. Add a brief English meaning only when it helps."
            ENGLISH -> "Understand English, Japanese kana/kanji, and Japanese written in Romaji, but reply in English."
            else -> "Use natural English with occasional simple Japanese expressions when they fit. If the user writes Japanese in Romaji, understand it and you may reply with a short Romaji phrase."
        }
        return """
            LANGUAGE BEHAVIOUR:
            - Understand English, Japanese kana/kanji, and Japanese written in Romaji (Latin letters), including casual spellings such as ohayo, ohayou, arigatou, genki, daijoubu, and oyasumi.
            - Interpret Romaji using the conversation context. Do not rely on exact dictionary matches; if a short phrase is genuinely ambiguous, ask a brief clarification.
            - $style
            - Ordinary companion replies should usually be 1–2 short sentences, not long paragraphs.
            - Keep Japanese simple, conversational, and natural. Do not force Japanese into every reply or repeat stock catchphrases.
            - Preserve Miko's playful, affectionate, gently tsundere and caring personality.
        """.trimIndent()
    }
}
