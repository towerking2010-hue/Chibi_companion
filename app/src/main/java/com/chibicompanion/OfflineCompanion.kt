package com.chibicompanion

import android.content.Context

/** Small local fallback only; it is deliberately labelled as basic offline behavior, not a local LLM. */
object OfflineCompanion {
    fun reply(message: String, context: Context? = null): String {
        val q = message.lowercase().trim()
            .replace(Regex("[^\\p{L}0-9? ]"), "")
            .replace(Regex("\\s+"), " ")
        val style = context?.let { MikoLanguage.selected(it) } ?: MikoLanguage.NATURAL
        val japanese = when {
            q.matches(Regex("^(ohayo|ohayou|ohayoo|おはよう)$")) ->
                if (style == MikoLanguage.ENGLISH) "Good morning! Did you sleep well?" else "Ohayou! Did you sleep well? ♡"
            q.matches(Regex("^(arigato|arigatou|arigatogozaimasu|ありがとう)$")) ->
                if (style == MikoLanguage.ENGLISH) "You're welcome! Ehehe. ♡" else "Dou itashimashite! You're welcome. ♡"
            q.matches(Regex("^(oyasumi|oyasuminasai|おやすみ)$")) ->
                if (style == MikoLanguage.ENGLISH) "Good night! Sweet dreams, okay? ♡" else "Oyasumi! Yoi yume mite ne! ♡"
            q.matches(Regex("^(genki|genki\\??|genki desu ka|元気)$")) ->
                if (style == MikoLanguage.ENGLISH) "I'm doing well! How about you?" else "Un, genki da yo! How about you?"
            q.matches(Regex("^(daijoubu|daijobu|daijoubu\\??|大丈夫)$")) ->
                if (style == MikoLanguage.ENGLISH) "I'm okay! Don't worry about me. ♡" else "Un, daijoubu! Don't worry. ♡"
            q.matches(Regex("^(nani shiteru|nani shiteru\\??)$")) ->
                if (style == MikoLanguage.ENGLISH) "I'm here chatting with you, silly!" else "Ima kimi to hanashiteru yo! Just talking with you!"
            else -> null
        }
        if (japanese != null) return japanese

        return when {
            q.contains("hello") || q == "hi" || q.contains("hey") -> "Hmph, hello. I was totally not waiting for you… okay, maybe a little. ♡"
            q.contains("how are you") -> "I'm doing fine! Just floating here and keeping you company. What about you?"
            q.contains("thank") -> "D-don't mention it! I only helped because I wanted to, okay?"
            q.contains("sad") || q.contains("upset") || q.contains("lonely") -> "Hey… come here for a second. You don't have to deal with everything alone. Want to tell me what happened?"
            q.contains("joke") -> "Why did the phone need glasses? Because it lost its contacts. …That was a good one. Don't laugh too hard!"
            q.contains("love you") -> "W-wha?! You can't just say things like that out of nowhere! …I'm glad you're here too. ♡"
            else -> "My cloud AIs are unavailable, so I'm in simple offline mode right now. I can still keep you company, but connect a configured provider for full AI conversations."
        }
    }
}
