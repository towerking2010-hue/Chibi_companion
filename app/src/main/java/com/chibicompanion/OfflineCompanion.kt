package com.chibicompanion

/**
 * Lightweight offline conversation patterns. This is intentionally not presented as a local LLM:
 * it handles common social cues and simple requests, then admits uncertainty without looping.
 */
object OfflineCompanion {
    fun reply(message: String): String {
        val q = message.trim().lowercase()
        return when {
            q.isBlank() -> "I'm listening. ♡"
            q.matches(Regex(".*\\b(shut up|shut tf up|be quiet|stop talking|go away)\\b.*")) ->
                "Okay, okay. I'll be quiet. Just tap the mic or send another message when you want me. ♡"
            q.matches(Regex(".*\\b(bruh+|bro+|wtf|what the heck)\\b.*")) ->
                "Hehe, fair reaction. Did I miss something, or are you just messing with me?"
            q.contains("you can't help") || q.contains("you cant help") ->
                "You're right that I might not know how yet. Tell me what you were trying to do, and I'll check whether I can handle it on this phone."
            q.contains("watashi wa miko ga suki") || q.contains("miko ga suki") ->
                "E-eh?! You like Miko? Hmph… you can't just say things like that out of nowhere. ♡"
            q.contains("ano") || q.contains("aishiteru") ->
                "E-eh? I caught a little Japanese there! Are you practicing with me?"
            q.contains("hello") || q.matches(Regex(".*\\b(hi|hey|hii|hiya)\\b.*")) ->
                "Hiii! You called? Ehehe, I'm here! ✨"
            q.contains("how are you") ->
                "I'm doing pretty well! A little more cheerful now that you're here. How about you?"
            q.contains("thank") ->
                "You're welcome! That's what I'm here for. ♡"
            q.contains("sad") || q.contains("upset") || q.contains("lonely") ->
                "Hey, I'm here with you. Want to tell me what happened? I'll listen."
            q.contains("joke") ->
                "Why did the phone wear glasses? It lost all its contacts! …Okay, okay, I'll work on my jokes."
            q.contains("love you") ->
                "A-ah, you're making me blush! I'm really glad you're here too. ♡"
            q.contains("what can you do") || q.contains("help me") ->
                "I can handle supported phone actions, simple questions and conversation starters offline. For open-ended answers, I need a connected AI provider. What would you like to try?"
            else ->
                "I don't know that one yet. I'm limited without a cloud AI, but I don't want to guess. Can you rephrase it or tell me what you're trying to do?"
        }
    }
}
