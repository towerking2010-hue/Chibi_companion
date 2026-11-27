package com.chibicompanion

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import android.text.InputType
import android.graphics.Color

class SettingsActivity : Activity(), android.speech.tts.TextToSpeech.OnInitListener {
    private lateinit var tts: android.speech.tts.TextToSpeech
    private lateinit var providerList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CompanionUi.applySystemBars(this)
        tts = android.speech.tts.TextToSpeech(this, this)

        val scroll = ScrollView(this).apply { setBackgroundColor(CompanionUi.bg); isFillViewport = true }
        val root = CompanionUi.column(this, 18).apply {
            setPadding(CompanionUi.dp(this@SettingsActivity, 20), CompanionUi.dp(this@SettingsActivity, 18),
                CompanionUi.dp(this@SettingsActivity, 20), CompanionUi.dp(this@SettingsActivity, 26))
        }
        scroll.addView(root)
        root.addView(CompanionUi.eyebrow(this, "Personalise"))
        root.addView(CompanionUi.heading(this, "Make her yours ♡", 26f))
        root.addView(CompanionUi.text(this, "Your choices stay on this phone unless a feature says otherwise.", 12f, CompanionUi.muted))
        root.addView(CompanionUi.spacer(this, 8))

        val voice = card("VOICE")
        voice.addView(CompanionUi.text(this, "Choose how Miko sounds", 14f, CompanionUi.ink, true))
        voice.addView(CompanionUi.button(this, "Choose Miko's voice ♡") { showVoicePicker() })
        voice.addView(CompanionUi.button(this, "Test Miko's voice") {
            if (::tts.isInitialized) {
                CompanionVoice.configure(this, tts)
                tts.speak("H-hello! It's me, Miko. How do I sound?", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "miko-voice-test")
            }
        })
        voice.addView(CompanionUi.button(this, "Install more voice options") {
            runCatching { startActivity(android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) }
                .onFailure { toast("Open your phone's Text-to-speech settings to install voices") }
        })
        root.addView(voice)

        val ai = card("AI CONNECTIONS")
        ai.addView(CompanionUi.text(this, "Cloud AI is optional. No provider is called until you add your own key.", 12f, CompanionUi.muted))
        val provider = field("Provider: gemini, groq, openrouter, or custom")
        val key = field("API key — encrypted on this device").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val model = field("Model (optional)")
        val endpoint = field("Custom HTTPS endpoint (custom only)")
        listOf(provider,key,model,endpoint).forEach { ai.addView(it) }
        providerList = CompanionUi.text(this, "", 11f, CompanionUi.muted)
        ai.addView(providerList)
        providerList.setOnClickListener { showProviderManager() }
        ai.addView(CompanionUi.button(this, "Manage saved providers") { showProviderManager() })
        ai.addView(CompanionUi.button(this, "Save provider", true) {
            val p=provider.text.toString().trim().lowercase()
            val k=key.text.toString().trim()
            val url=endpoint.text.toString().trim()
            if(p !in listOf("gemini","groq","openrouter","custom") || k.isBlank() || (p=="custom" && !url.startsWith("https://"))) {
                toast("Choose a supported provider, enter a key, and use HTTPS for custom providers")
            } else {
                ProviderStore.add(this,p,k,model.text.toString().trim(),url)
                key.text.clear(); refreshProviders(); toast("Provider saved securely ♡")
            }
        })
        ai.addView(CompanionUi.button(this, "Clear saved providers") {
            AlertDialog.Builder(this).setTitle("Clear provider keys?")
                .setMessage("This removes all saved cloud provider credentials from this phone.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Clear") { _,_ -> ProviderStore.clear(this); refreshProviders(); toast("Saved provider keys cleared") }.show()
        })
        refreshProviders()
        root.addView(ai)

        val memory = card("MEMORY")
        memory.addView(CompanionUi.text(this, "Miko's memories are stored locally and encrypted.", 12f, CompanionUi.muted))
        memory.addView(CompanionUi.button(this, "View and manage memories") { showMemoryManager() })
        root.addView(memory)

        val behavior = card("BEHAVIOUR")
        behavior.addView(CompanionUi.text(this, "Choose how proactive Miko should be.", 12f, CompanionUi.muted))
        behavior.addView(CompanionUi.button(this, "Proactive mode: " + AssistantPreferences.proactiveMode(this).uppercase()) {
            val modes=arrayOf("Quiet — only respond when I interact","Optional — occasional helpful check-ins","Active — proactively suggest useful actions")
            val values=arrayOf(AssistantPreferences.QUIET,AssistantPreferences.OPTIONAL,AssistantPreferences.ACTIVE)
            AlertDialog.Builder(this).setTitle("How proactive should she be?")
                .setSingleChoiceItems(modes,values.indexOf(AssistantPreferences.proactiveMode(this))) { d,w ->
                    AssistantPreferences.setProactiveMode(this,values[w]); d.dismiss(); recreate()
                }.setNegativeButton("Cancel",null).show()
        })
        root.addView(behavior)
        root.addView(CompanionUi.button(this, "←  Back to Miko") { finish() })
        setContentView(scroll)
    }

    private fun card(label: String) = CompanionUi.column(this,14).apply {
        background=CompanionUi.panel(this@SettingsActivity,CompanionUi.surface,20)
        addView(CompanionUi.text(this@SettingsActivity,label,10f,CompanionUi.pink,true))
        addView(CompanionUi.spacer(this@SettingsActivity,4))
    }
    private fun field(hintText: String) = android.widget.EditText(this).apply {
        hint=hintText; setSingleLine(); textSize=13f
        setTextColor(CompanionUi.ink); setHintTextColor(CompanionUi.muted)
        setPadding(CompanionUi.dp(this@SettingsActivity,12),CompanionUi.dp(this@SettingsActivity,11),CompanionUi.dp(this@SettingsActivity,12),CompanionUi.dp(this@SettingsActivity,11))
        background=CompanionUi.panel(this@SettingsActivity,CompanionUi.surfaceRaised,14)
        val lp=android.widget.LinearLayout.LayoutParams(-1,-2); lp.topMargin=CompanionUi.dp(this@SettingsActivity,6); layoutParams=lp
    }
    private fun refreshProviders() {
        val list=ProviderStore.all(this)
        providerList.text=if(list.isEmpty()) "No providers saved • basic offline fallback only" else
            "Fallback order:\n"+list.mapIndexed { i,v -> "${i+1}. ${v.name} — ${v.model.ifBlank { "default model" }}" }.joinToString("\n")
    }
    private fun showProviderManager() {
        val providers = ProviderStore.all(this)
        if (providers.isEmpty()) { toast("No saved providers yet"); return }
        val labels = providers.mapIndexed { i, p -> "${i + 1}. ${p.name} • ${p.model.ifBlank { "default model" }}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Saved AI providers")
            .setItems(labels) { _, index ->
                AlertDialog.Builder(this).setTitle("Remove ${providers[index].name}?")
                    .setMessage("This deletes its encrypted API key from this device.")
                    .setNegativeButton("Keep", null)
                    .setPositiveButton("Remove") { _, _ -> ProviderStore.removeAt(this, index); refreshProviders(); toast("Provider removed") }
                    .show()
            }.setNegativeButton("Close", null).show()
    }
    private fun showVoicePicker() {
        val voices=CompanionVoice.options(tts)
        if(voices.isEmpty()) {
            AlertDialog.Builder(this).setTitle("No extra voices found")
                .setMessage("Install a voice pack using the button above, then return here.")
                .setPositiveButton("OK",null).show(); return
        }
        val labels=voices.map { CompanionVoice.label(it) }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choose her voice")
            .setSingleChoiceItems(labels,voices.indexOfFirst { it.name==CompanionVoice.selectedName(this) }) { d,w ->
                CompanionVoice.select(this,tts,voices[w]); toast("Voice updated ♡"); d.dismiss()
            }.setNegativeButton("Cancel",null).show()
    }
    private fun showMemoryManager() {
        val items=CompanionMemory.all(this)
        val labels=(items.map { "✦  $it" }+"＋  Add a memory").toTypedArray()
        AlertDialog.Builder(this).setTitle("Her memories — stored on this phone")
            .setItems(labels) { _,which ->
                if(which==items.size) {
                    val input=EditText(this).apply { hint="Something you want her to remember"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES }
                    AlertDialog.Builder(this).setTitle("Add a memory").setView(input)
                        .setPositiveButton("Save") { _,_ -> if(CompanionMemory.add(this,input.text.toString())) toast("Memory saved securely ♡") else toast("Please enter more detail") }
                        .setNegativeButton("Cancel",null).show()
                } else AlertDialog.Builder(this).setTitle("Delete this memory?")
                    .setMessage(items[which]).setPositiveButton("Delete") { _,_ -> CompanionMemory.remove(this,which); toast("Memory deleted") }
                    .setNegativeButton("Keep",null).show()
            }.setNeutralButton("Clear all") { _,_ ->
                AlertDialog.Builder(this).setTitle("Clear all memories?").setMessage("This cannot be undone.")
                    .setPositiveButton("Clear") { _,_ -> CompanionMemory.clear(this); toast("All memories cleared") }
                    .setNegativeButton("Cancel",null).show()
            }.setNegativeButton("Close",null).show()
    }
    override fun onInit(status: Int) { if(status==android.speech.tts.TextToSpeech.SUCCESS) CompanionVoice.configure(this,tts) }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy(){ if(::tts.isInitialized){tts.stop();tts.shutdown()}; super.onDestroy() }
}
