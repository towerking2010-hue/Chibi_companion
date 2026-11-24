package com.chibicompanion

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import java.util.Locale
import java.util.concurrent.Executors

class ChatActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var log: TextView
    private lateinit var tts: TextToSpeech
    private val worker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 18)
        }
        root.addView(TextView(this).apply {
            text = "Your companion ♡"
            textSize = 24f
        })
        log = TextView(this).apply {
            text = "Hmph! You finally came to talk to me?\nAdd an API key below to enable AI replies."
            textSize = 16f
            setPadding(0, 18, 0, 18)
        }
        root.addView(ScrollView(this).apply { addView(log) },
            LinearLayout.LayoutParams(-1, 0, 1f))

        val provider = EditText(this).apply {
            hint = "Provider: gemini, groq, openrouter, or custom"
            setSingleLine()
        }
        val key = EditText(this).apply {
            hint = "API key (encrypted on this device)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine()
        }
        val model = EditText(this).apply { hint = "Model (optional for built-in providers)"; setSingleLine() }
        val endpoint = EditText(this).apply { hint = "Custom HTTPS endpoint (custom provider only)"; setSingleLine() }
        root.addView(provider); root.addView(key); root.addView(model); root.addView(endpoint)
        val providerList = TextView(this).apply { textSize = 13f; setPadding(0, 8, 0, 8) }
        fun refreshProviders() {
            val list = ProviderStore.all(this)
            providerList.text = if (list.isEmpty()) "No providers saved yet. Add providers in priority order." else
                "Fallback order (first available wins):\n" + list.mapIndexed { i, v -> "${i + 1}. ${v.name} — ${v.model.ifBlank { "default model" }}" }.joinToString("\n")
        }
        root.addView(providerList)
        refreshProviders()
        root.addView(Button(this).apply {
            text = "Save provider"
            setOnClickListener {
                val p = provider.text.toString().trim().lowercase()
                val k = key.text.toString().trim()
                val url = endpoint.text.toString().trim()
                if (p !in listOf("gemini", "groq", "openrouter", "custom") || k.isBlank() || (p == "custom" && !url.startsWith("https://"))) {
                    toast("Choose a supported provider, enter a key, and give custom providers an HTTPS endpoint")
                } else {
                    ProviderStore.add(this@ChatActivity, p, k, model.text.toString().trim(), url)
                    toast("Provider saved. It will be tried in this order.")
                    key.text.clear()
                    refreshProviders()
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Clear all saved providers"
            setOnClickListener { ProviderStore.clear(this@ChatActivity); refreshProviders(); toast("Provider keys cleared") }
        })
        val entry = EditText(this).apply { hint = "Say something to her…" }
        root.addView(entry)
        root.addView(Button(this).apply {
            text = "Send"
            setOnClickListener {
                val message = entry.text.toString().trim()
                if (message.isNotEmpty()) {
                    log.append("\n\nYou: $message\nHer: thinking…")
                    entry.text.clear()
                    worker.execute {
                        val answer = runCatching { AiClient.reply(this@ChatActivity, message) }
                            .getOrElse { OfflineCompanion.reply(message) }
                        runOnUiThread {
                            log.text = log.text.toString().replace("Her: thinking…", "Her: $answer")
                            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "reply")
                        }
                    }
                }
            }
        })
        setContentView(root)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { tts.language = Locale.getDefault(); tts.setSpeechRate(0.94f); tts.setPitch(1.12f) }
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    override fun onDestroy() {
        worker.shutdownNow()
        tts.stop(); tts.shutdown()
        super.onDestroy()
    }
}
