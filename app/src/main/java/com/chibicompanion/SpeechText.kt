package com.chibicompanion

/** Removes pictographs and decorative symbols before sending text to Android TTS. */
object SpeechText {
    private val emojiAndSymbols = Regex("[\\p{So}\\p{Sk}\\uFE0F\\u200D\\u20E3]+")
    private val extraSpacing = Regex("\\s{2,}")

    fun forTts(text: String): String = extraSpacing
        .replace(emojiAndSymbols.replace(text, " "), " ")
        .trim()
}
