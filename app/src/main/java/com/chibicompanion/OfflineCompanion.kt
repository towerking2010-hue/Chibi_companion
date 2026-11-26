package com.chibicompanion

/** Small local fallback only; it is deliberately labelled as basic offline behavior, not a local LLM. */
object OfflineCompanion {
    fun reply(message: String): String {
        val q = message.lowercase()
        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") -> "Hiii! You called? Ehehe, I'm here! ✨"
            q.contains("how are you") -> "I'm doing great now that you're here! What are we getting into today? ✨"
            q.contains("thank") -> "You're welcome! That's what I'm here for. Don't forget to give yourself some credit too! ♡"
            q.contains("sad") || q.contains("upset") || q.contains("lonely") -> "Hey, I'm here with you. You don't have to carry everything alone. Want to tell me what's going on?"
            q.contains("joke") -> "Why did the phone wear glasses? It lost all its contacts! ...Hehe, okay, I'll work on my jokes."
            q.contains("love you") -> "A-ah, you're making me blush! I'm really glad you're here too. ♡"
            else -> "Hmm, I'm not sure about that one yet. Tell me a little more, or give me a task I can help with! ♡"
        }
    }
}
