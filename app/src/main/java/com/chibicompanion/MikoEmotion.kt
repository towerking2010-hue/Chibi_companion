package com.chibicompanion

/**
 * Lightweight, deterministic emotion selection for Miko.
 *
 * This is intentionally a presentation hint, not sentiment analysis or an
 * assertion about the user's feelings. Explicit cues in Miko's own reply win;
 * otherwise the renderer receives a calm neutral state.
 */
object MikoEmotion {
    private val cues = listOf(
        "excited" to listOf("!!!", "let's go", "lets go", "amazing", "that's awesome", "thats awesome", "woohoo", "can't wait", "cant wait", "✨", "🌟"),
        "angry" to listOf("i'm angry", "im angry", "that's unfair", "thats unfair", "how dare", "grr", "baka"),
        "crying" to listOf("i'm crying", "im crying", "😭", "so heartbreaking", "that hurts"),
        "sad" to listOf("i'm sorry", "im sorry", "that's sad", "thats sad", "that sounds painful", "i feel bad"),
        "shy" to listOf("you're making me blush", "you are making me blush", "embarrassed", "don't tease me", "dont tease me"),
        "sleepy" to listOf("good night", "sleep well", "i'm sleepy", "im sleepy", "so sleepy", "zzz"),
        "surprised" to listOf("wait, what", "no way", "seriously?!", "whoa", "oh!"),
        "teasing" to listOf("hehe", "ehehe", "gotcha", "just kidding", "teasing you"),
        "affectionate" to listOf("♡", "♥", "love you", "missed you", "here for you", "proud of you"),
        "happy" to listOf("glad to hear", "that makes me happy", "you're welcome", "you are welcome", "happy to help", "nice work", "well done")
    )

    fun fromReply(reply: String): String {
        val normalized = reply.lowercase()
        return cues.firstOrNull { (_, phrases) -> phrases.any { normalized.contains(it) } }?.first ?: "neutral"
    }
}
