package com.chibicompanion

/** Small deterministic mood mapper used when the AI provider does not return structured emotion metadata. */
object MikoEmotion {
    fun fromReply(reply: String): String {
        val text = reply.lowercase()
        return when {
            listOf("i'm sorry", "im sorry", "that sounds hard", "i'm here for you", "you aren't alone",
                "you are not alone", "take your time", "it's okay to cry").any { it in text } -> "comfort"
            listOf("surprise", "whoa", "what?!", "really?!", "no way", "oh my gosh").any { it in text } -> "surprised"
            listOf("good night", "oyasumi", "sleep well", "time to rest").any { it in text } -> "sleepy"
            listOf("baka", "hmph", "don't get the wrong idea", "do not get the wrong idea",
                "not like i", "annoying", "whatever!").any { it in text } -> "tsundere"
            listOf("hehe", "ehehe", "so happy", "that makes me happy", "glad to hear",
                "wonderful", "that's amazing", "that is amazing", "♡").any { it in text } -> "happy"
            listOf("i'm proud of you", "im proud of you", "you did it", "congratulations",
                "おめでとう", "omedetou").any { it in text } -> "excited"
            listOf("i miss you", "missed you", "welcome back", "hello", "ohayo", "good morning").any { it in text } -> "greeting"
            listOf("let me think", "thinking", "one moment", "let's see", "lets see").any { it in text } -> "thinking"
            else -> "listening"
        }
    }

    fun fromUserMessage(message: String): String {
        val text = message.lowercase()
        return when {
            listOf("sad", "lonely", "upset", "depressed", "bad day", "crying", "hurt").any { it in text } -> "comfort"
            listOf("good night", "oyasumi", "sleepy", "going to bed").any { it in text } -> "sleepy"
            listOf("hello", "hi", "hey", "ohayo", "ohayou", "good morning").any { it in text } -> "greeting"
            listOf("guess what", "you know what", "surprise").any { it in text } -> "surprised"
            else -> "thinking"
        }
    }
}
