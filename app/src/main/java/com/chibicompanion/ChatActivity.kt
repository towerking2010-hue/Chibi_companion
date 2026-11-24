package com.chibicompanion

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale
import java.util.concurrent.Executors

class ChatActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var log: TextView
    private lateinit var tts: TextToSpeech
    private val worker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CompanionUi.bg
        window.navigationBarColor = CompanionUi.bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        tts = TextToSpeech(this, this)

        val root = CompanionUi.column(this, 18).apply { setBackgroundColor(CompanionUi.bg) }
        val heading = CompanionUi.column(this)
        heading.addView(CompanionUi.text(this, "YOUR LITTLE CHAT SPACE", 10f, CompanionUi.pink, true))
        heading.addView(CompanionUi.text(this, "Let's talk ♡", 26f, CompanionUi.ink, true))
        heading.addView(CompanionUi.text(this, "She may act tough, but she's happy you're here.", 12f, CompanionUi.muted))
        root.addView(heading)
        root.addView(CompanionUi.spacer(this, 14))

        val chatCard = CompanionUi.column(this, 15).apply { background = CompanionUi.panel(this@ChatActivity, Color.WHITE, 24) }
        log = CompanionUi.text(this, "Ayaya… you finally came to see me? ♡\n\nAdd an AI provider below when you're ready for cloud conversations.", 15f)
        log.setLineSpacing(CompanionUi.dp(this,3).toFloat(), 1f)
        chatCard.addView(ScrollView(this).apply { isFillViewport = true; addView(log) }, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(chatCard, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(CompanionUi.spacer(this, 12))

        val entry = EditText(this).apply {
            hint = "Say something to her…"
            textSize = 15f
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
            background = CompanionUi.panel(this@ChatActivity, Color.WHITE, 18)
            setPadding(CompanionUi.dp(this@ChatActivity,16), CompanionUi.dp(this@ChatActivity,13), CompanionUi.dp(this@ChatActivity,16), CompanionUi.dp(this@ChatActivity,13))
        }
        root.addView(entry)
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "Send message  ➜", true) {
            val message = entry.text.toString().trim()
            if (message.isNotEmpty()) {
                log.append("\n\nYou: $message\n\nHer: thinking…")
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
        })
        root.addView(CompanionUi.spacer(this, 12))

        val settingsCard = CompanionUi.column(this, 14).apply { background = CompanionUi.panel(this@ChatActivity, Color.rgb(245,242,250), 22) }
        val settingsTitle = CompanionUi.text(this, "AI CONNECTIONS  ✦", 12f, CompanionUi.ink, true)
        settingsCard.addView(settingsTitle)
        val provider = EditText(this).apply { hint = "Provider: gemini, groq, openrouter, or custom"; setSingleLine(); textSize=13f }
        val key = EditText(this).apply {
            hint = "API key (encrypted on this device)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(); textSize=13f
        }
        val model = EditText(this).apply { hint = "Model (optional)"; setSingleLine(); textSize=13f }
        val endpoint = EditText(this).apply { hint = "Custom HTTPS endpoint (custom only)"; setSingleLine(); textSize=13f }
        listOf(provider,key,model,endpoint).forEach {
            it.setPadding(CompanionUi.dp(this,12), CompanionUi.dp(this,10), CompanionUi.dp(this,12), CompanionUi.dp(this,10))
            it.background = CompanionUi.panel(this, Color.WHITE, 14)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = CompanionUi.dp(this,6); settingsCard.addView(it,lp)
        }
        val providerList = CompanionUi.text(this, "", 11f, CompanionUi.muted)
        fun refreshProviders() {
            val list = ProviderStore.all(this)
            providerList.text = if (list.isEmpty()) "No providers saved. Add one to enable cloud AI." else
                "Fallback order:\n" + list.mapIndexed { i,v -> "${i+1}. ${v.name} — ${v.model.ifBlank { "default model" }}" }.joinToString("\n")
        }
        settingsCard.addView(CompanionUi.spacer(this,6)); settingsCard.addView(providerList)
        settingsCard.addView(CompanionUi.spacer(this,8))
        settingsCard.addView(CompanionUi.button(this, "Save provider", true) {
            val p = provider.text.toString().trim().lowercase()
            val k = key.text.toString().trim()
            val url = endpoint.text.toString().trim()
            if (p !in listOf("gemini","groq","openrouter","custom") || k.isBlank() || (p=="custom" && !url.startsWith("https://"))) {
                toast("Choose a supported provider, enter a key, and use HTTPS for custom providers")
            } else {
                ProviderStore.add(this@ChatActivity,p,k,model.text.toString().trim(),url)
                toast("Provider saved ♡"); key.text.clear(); refreshProviders()
            }
        })
        settingsCard.addView(CompanionUi.spacer(this,6))
        settingsCard.addView(CompanionUi.button(this, "Clear saved providers") {
            ProviderStore.clear(this@ChatActivity); refreshProviders(); toast("Saved provider keys cleared")
        })
        refreshProviders()
        val settingsScroll = ScrollView(this).apply { addView(settingsCard) }
        root.addView(settingsScroll, LinearLayout.LayoutParams(-1, CompanionUi.dp(this,250)))
        setContentView(root)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { tts.language = Locale.getDefault(); tts.setSpeechRate(0.94f); tts.setPitch(1.12f) }
    }
    private fun toast(s: String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy() {
        worker.shutdownNow(); tts.stop(); tts.shutdown(); super.onDestroy()
    }
}
