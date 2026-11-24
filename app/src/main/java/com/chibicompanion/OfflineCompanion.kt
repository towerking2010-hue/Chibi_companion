package com.chibicompanion

/** Small local fallback only; it is deliberately labelled as basic offline behavior, not a local LLM. */
object OfflineCompanion {
    fun reply(message: String): String {
        val q = message.lowercase()
        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") -> "Hmph, hello. I was totally not waiting for you… okay, maybe a little. ♡"
            q.contains("how are you") -> "I'm doing fine! Just floating here and keeping you company. What about you?"
            q.contains("thank") -> "D-don't mention it! I only helped because I wanted to, okay?"
            q.contains("sad") || q.contains("upset") || q.contains("lonely") -> "Hey… come here for a second. You don't have to deal with everything alone. Want to tell me what happened?"
            q.contains("joke") -> "Why did the phone need glasses? Because it lost its contacts. …That was a good one. Don't laugh too hard!"
            q.contains("love you") -> "W-wha?! You can't just say things like that out of nowhere! …I'm glad you're here too. ♡"
            else -> "My cloud AIs are unavailable, so I'm in simple offline mode right now. I can still keep you company, but connect a configured provider for full AI conversations."
        }
    }
}
