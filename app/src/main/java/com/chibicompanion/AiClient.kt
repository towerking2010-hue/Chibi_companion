package com.chibicompanion

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    fun reply(ctx: Context, message: String): String {
        val providers = ProviderStore.all(ctx)
        if (providers.isEmpty()) return OfflineCompanion.reply(message, ctx)
        val errors = mutableListOf<String>()
        for (p in providers) {
            try {
                return when (p.name) {
                    "gemini" -> gemini(ctx, p, message)
                    "groq" -> openAiCompatible(ctx, "https://api.groq.com/openai/v1/chat/completions", p, message)
                    "openrouter" -> openAiCompatible(ctx, "https://openrouter.ai/api/v1/chat/completions", p, message)
                    "custom" -> openAiCompatible(ctx, p.endpoint, p, message)
                    else -> throw IllegalArgumentException("Unsupported provider")
                }
            } catch (e: Exception) {
                errors += "${p.name}: ${e.message ?: "request failed"}"
            }
        }
        throw IllegalStateException(errors.joinToString("; "))
    }


    /** Sends a one-time, user-approved screenshot to a configured vision-capable provider. */
    fun replyWithImage(ctx: Context, message: String, jpeg: ByteArray): String {
        val providers = ProviderStore.all(ctx)
        if (providers.isEmpty()) return "I can capture your screen, but I need a connected vision-capable AI provider to understand the image. Gemini is the easiest option to configure."
        val errors = mutableListOf<String>()
        for (p in providers) {
            try {
                return when (p.name) {
                    "gemini" -> geminiImage(ctx, p, message, jpeg)
                    "groq" -> openAiImage(ctx, "https://api.groq.com/openai/v1/chat/completions", p, message, jpeg)
                    "openrouter" -> openAiImage(ctx, "https://openrouter.ai/api/v1/chat/completions", p, message, jpeg)
                    "custom" -> openAiImage(ctx, p.endpoint, p, message, jpeg)
                    else -> throw IllegalArgumentException("Unsupported provider")
                }
            } catch (e: Exception) {
                errors += p.name + ": " + (e.message ?: "vision request failed")
            }
        }
        throw IllegalStateException(errors.joinToString("; "))
    }

    private fun geminiImage(ctx: Context, p: Provider, message: String, jpeg: ByteArray): String {
        val model = p.model.ifBlank { "gemini-2.5-flash" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + p.key)
        val parts = JSONArray()
            .put(JSONObject().put("text", systemPrompt(ctx) + "\n\nRecent conversation:\n" + ChatHistory.read(ctx).takeLast(5000) + "\n\nUser request: " + message))
            .put(JSONObject().put("inlineData", JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))))
        val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
        val response = request(url, null, body)
        return response.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun openAiImage(ctx: Context, endpoint: String, p: Provider, message: String, jpeg: ByteArray): String {
        val model = p.model.ifBlank {
            when (p.name) { "groq" -> "llama-3.3-70b-versatile"; "openrouter" -> "openrouter/free"; else -> "" }
        }
        require(endpoint.startsWith("https://")) { "Custom provider endpoint must use HTTPS" }
        require(model.isNotBlank()) { "Enter a model name for this provider" }
        val imageUrl = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
        val userContent = JSONArray()
            .put(JSONObject().put("type", "text").put("text",
                "Recent conversation:\n" + ChatHistory.read(ctx).takeLast(5000) + "\n\nUser request: " + message))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageUrl)))
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt(ctx)))
            .put(JSONObject().put("role", "user").put("content", userContent))
        val response = request(URL(endpoint), p.key, JSONObject().put("model", model).put("messages", messages))
        return response.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }

    private fun systemPrompt(ctx: Context) =
        "You are a cute, affectionate but tsundere chibi anime companion. You are playful, caring, concise, " +
        "and emotionally attentive. Never claim you performed phone actions unless the app actually did so. Keep replies natural and concise. " +
        "Respect the user's privacy. Screen context is supplied only when the user asks about their screen. " +
        "Use saved memories only when relevant; do not pretend to remember details that are not supplied.\n\nUser memories:\n" +
        CompanionMemory.promptContext(ctx) + "\n\n" + MikoLanguage.instructions(ctx)

    private fun gemini(ctx: Context, p: Provider, message: String): String {
        val model = p.model.ifBlank { "gemini-2.5-flash" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${p.key}")
        val context = ChatHistory.read(ctx).takeLast(7000)
        val contents = JSONArray().put(JSONObject().put("role", "user").put("parts",
            JSONArray().put(JSONObject().put("text", systemPrompt(ctx) + "\n\nRecent conversation:\n" + context + "\n\nReply to this message: " + message))))
        val body = JSONObject().put("contents", contents)
        val response = request(url, null, body)
        return response.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun openAiCompatible(ctx: Context, endpoint: String, p: Provider, message: String): String {
        val model = p.model.ifBlank {
            when (p.name) { "groq" -> "llama-3.3-70b-versatile"; "openrouter" -> "openrouter/free"; else -> "" }
        }
        require(endpoint.startsWith("https://")) { "Custom provider endpoint must use HTTPS" }
        require(model.isNotBlank()) { "Enter a model name for this provider" }
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt(ctx)))
            .put(JSONObject().put("role", "user").put("content", "Recent conversation:\n" + ChatHistory.read(ctx).takeLast(7000) + "\n\nCurrent message: " + message))
        val body = JSONObject().put("model", model).put("messages", messages)
        val response = request(URL(endpoint), p.key, body)
        return response.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }

    private fun request(url: URL, bearer: String?, body: JSONObject): JSONObject {
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val raw = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${raw.take(300)}")
            return JSONObject(raw)
        } finally { c.disconnect() }
    }
}
