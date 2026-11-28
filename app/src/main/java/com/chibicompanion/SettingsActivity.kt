package com.chibicompanion

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import android.text.InputType
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class SettingsActivity : Activity(), android.speech.tts.TextToSpeech.OnInitListener {
    private lateinit var tts: android.speech.tts.TextToSpeech
    private lateinit var providerList: TextView
    private val importAnimationsRequest = 811

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CompanionUi.applySystemBars(this)
        tts = android.speech.tts.TextToSpeech(this, this)

        val scroll = ScrollView(this).apply { setBackgroundColor(CompanionUi.bg); isFillViewport = true }
        val root = CompanionUi.column(this, 0).apply {
            setPadding(CompanionUi.dp(this@SettingsActivity, 24), CompanionUi.dp(this@SettingsActivity, 26),
                CompanionUi.dp(this@SettingsActivity, 24), CompanionUi.dp(this@SettingsActivity, 40))
        }
        scroll.addView(root)
        root.addView(CompanionUi.eyebrow(this, "Personalise"))
        root.addView(CompanionUi.heading(this, "Make her yours ♡", 30f))
        root.addView(CompanionUi.text(this, "Your choices stay on this phone unless a feature says otherwise.", 14f, CompanionUi.muted))
        root.addView(CompanionUi.spacer(this, 8))

        val voice = card("VOICE")
        voice.addView(CompanionUi.text(this, "Choose how Miko sounds", 16f, CompanionUi.ink, true))
        addSpaced(voice, CompanionUi.button(this, "Choose Miko's voice ♡") { showVoicePicker() }, 12)
        addSpaced(voice, CompanionUi.button(this, "Test Miko's voice") {
            if (::tts.isInitialized) {
                CompanionVoice.configure(this, tts)
                tts.speak("H-hello! It's me, Miko. How do I sound?", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "miko-voice-test")
            }
        }, 10)
        addSpaced(voice, CompanionUi.button(this, "Install more voice options") {
            runCatching { startActivity(android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) }
                .onFailure { toast("Open your phone's Text-to-speech settings to install voices") }
        }, 10)
        addSection(root, voice)

        val animations = card("MIKO'S EXPRESSIONS")
        animations.addView(CompanionUi.text(this,
            "Give her a little life. Preview her expressions here, then install the transparent animation pack.",
            13f, CompanionUi.muted))
        val preview = MikoSpriteView(this)
        animations.addSpaced(preview, 8, 230)
        val previewActions = CompanionUi.row(this)
        listOf("idle" to "Idle", "thinking" to "Think", "happy" to "Happy").forEach { (mood, label) ->
            val action = CompanionUi.button(this, label) { preview.setMood(mood) }
            previewActions.addView(action, LinearLayout.LayoutParams(0, CompanionUi.dp(this, 52), 1f).apply {
                marginStart = CompanionUi.dp(this@SettingsActivity, 4)
                marginEnd = CompanionUi.dp(this@SettingsActivity, 4)
            })
        }
        animations.addSpaced(previewActions, 8)
        animations.addSpaced(CompanionUi.text(this, animationStatus(), 12f, CompanionUi.muted), 8)
        addSpaced(animations, CompanionUi.button(this, "Install / update Miko's 11 animations", true) {
            pickAnimations()
        }, 12)
        addSection(root, animations)

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
        addSpaced(ai, CompanionUi.button(this, "Manage saved providers") { showProviderManager() }, 12)
        addSpaced(ai, CompanionUi.button(this, "Save provider", true) {
            val p=provider.text.toString().trim().lowercase()
            val k=key.text.toString().trim()
            val url=endpoint.text.toString().trim()
            if(p !in listOf("gemini","groq","openrouter","custom") || k.isBlank() || (p=="custom" && !url.startsWith("https://"))) {
                toast("Choose a supported provider, enter a key, and use HTTPS for custom providers")
            } else {
                ProviderStore.add(this,p,k,model.text.toString().trim(),url)
                key.text.clear(); refreshProviders(); toast("Provider saved securely ♡")
            }
        }, 10)
        addSpaced(ai, CompanionUi.button(this, "Clear saved providers") {
            MikoSheet.show(this, "AI CONNECTIONS", "Remove saved providers?", "This removes every saved cloud API credential from this phone. No provider will be called until you add one again.", actionLabel = "Clear saved providers") {
                ProviderStore.clear(this)
                refreshProviders()
                toast("Saved provider keys cleared")
            }
        }, 10)
        refreshProviders()
        addSection(root, ai)

        val memory = card("MEMORY")
        memory.addView(CompanionUi.text(this, "Miko's memories are stored locally and encrypted.", 12f, CompanionUi.muted))
        addSpaced(memory, CompanionUi.button(this, "View and manage memories") { showMemoryManager() }, 12)
        addSection(root, memory)

        val behavior = card("BEHAVIOUR")
        behavior.addView(CompanionUi.text(this, "Choose how proactive Miko should be.", 12f, CompanionUi.muted))
        addSpaced(behavior, CompanionUi.button(this, "Proactive mode: " + AssistantPreferences.proactiveMode(this).uppercase()) {
            val modes=arrayOf("Quiet — only respond when I interact","Optional — occasional helpful check-ins","Active — proactively suggest useful actions")
            val values=arrayOf(AssistantPreferences.QUIET,AssistantPreferences.OPTIONAL,AssistantPreferences.ACTIVE)
            AlertDialog.Builder(this).setTitle("How proactive should she be?")
                .setSingleChoiceItems(modes,values.indexOf(AssistantPreferences.proactiveMode(this))) { d,w ->
                    AssistantPreferences.setProactiveMode(this,values[w]); d.dismiss(); recreate()
                }.setNegativeButton("Cancel",null).show()
        }, 12)
        addSection(root, behavior)
        addSpaced(root, CompanionUi.button(this, "←  Back to Miko") { finish() }, 22)
        setContentView(scroll)
    }

    private fun card(label: String) = CompanionUi.column(this,22).apply {
        background=CompanionUi.panel(this@SettingsActivity,CompanionUi.surface,20)
        addView(CompanionUi.text(this@SettingsActivity,label,11f,CompanionUi.pink,true))
        addView(CompanionUi.spacer(this@SettingsActivity,4))
    }
    private fun field(hintText: String) = android.widget.EditText(this).apply {
        hint=hintText; setSingleLine(); textSize=13f
        setTextColor(CompanionUi.ink); setHintTextColor(CompanionUi.muted)
        setPadding(CompanionUi.dp(this@SettingsActivity,16),CompanionUi.dp(this@SettingsActivity,16),CompanionUi.dp(this@SettingsActivity,16),CompanionUi.dp(this@SettingsActivity,16))
        minHeight=CompanionUi.dp(this@SettingsActivity,58)
        background=CompanionUi.panel(this@SettingsActivity,CompanionUi.surfaceRaised,14)
        val lp=android.widget.LinearLayout.LayoutParams(-1,-2); lp.topMargin=CompanionUi.dp(this@SettingsActivity,12); layoutParams=lp
    }
    private fun addSection(root: LinearLayout, section: View) {
        root.addView(section, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = CompanionUi.dp(this@SettingsActivity, 20)
        })
    }

    private fun addSpaced(parent: LinearLayout, child: View, top: Int = 10, fixedHeight: Int? = null) {
        val params = LinearLayout.LayoutParams(-1, fixedHeight?.let { CompanionUi.dp(this, it) } ?: -2)
        params.topMargin = CompanionUi.dp(this, top)
        parent.addView(child, params)
    }

    private fun animationStatus(): String {
        val dir = File(filesDir, "miko_animations")
        val expected = listOf("idle.webp", "happy.webp", "laughing.webp", "annoyed.webp",
            "astonished.webp", "blush.webp", "sad.webp", "sleepy.webp", "thinking.webp", "wave.webp", "little dance.webp")
        val count = expected.count { File(dir, it).isFile && File(dir, it).length() > 0L }
        return if (count == expected.size) "✓ All 11 transparent animations are installed."
            else "$count of 11 animations installed. Import the ZIP to unlock idle, thinking, happy, wave and more."
    }

    private fun pickAnimations() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        startActivityForResult(pick, importAnimationsRequest)
    }

    @Deprecated("Android document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != importAnimationsRequest || resultCode != RESULT_OK || data?.data == null) return
        installAnimations(data.data!!)
    }

    private fun installAnimations(uri: Uri) {
        val expected = setOf("idle.webp", "happy.webp", "laughing.webp", "annoyed.webp", "astonished.webp",
            "blush.webp", "sad.webp", "sleepy.webp", "thinking.webp", "wave.webp", "little dance.webp")
        val staging = File(cacheDir, "miko_animation_import").apply { deleteRecursively(); mkdirs() }
        val imported = mutableSetOf<String>()
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var totalBytes = 0L
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.substringAfterLast('/')
                        if (!entry.isDirectory && name in expected) {
                            val target = File(staging, name)
                            FileOutputStream(target).use { output ->
                                val buffer = ByteArray(8192)
                                var entryBytes = 0L
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    entryBytes += count
                                    totalBytes += count
                                    require(entryBytes <= 8L * 1024 * 1024 && totalBytes <= 50L * 1024 * 1024) {
                                        "Animation pack exceeds the safe import size"
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                            imported.add(name)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Unable to open the selected ZIP")
            require(imported.containsAll(expected)) { "This ZIP contains \${imported.size} of the 11 required animations." }
            val destination = File(filesDir, "miko_animations").apply { mkdirs() }
            expected.forEach { name -> File(staging, name).copyTo(File(destination, name), overwrite = true) }
            startService(Intent(this, FloatingCompanionService::class.java).setAction("RELOAD_MIKO_ASSETS"))
            Toast.makeText(this, "All 11 Miko animations installed ♡", Toast.LENGTH_LONG).show()
            recreate()
        } catch (error: Exception) {
            Toast.makeText(this, "Animation import failed: \${error.message ?: "invalid ZIP"}", Toast.LENGTH_LONG).show()
        } finally {
            staging.deleteRecursively()
        }
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
        val builder=AlertDialog.Builder(this).setTitle("Her memories — stored on this phone")
        if (items.isEmpty()) {
            builder.setMessage("Miko hasn't saved any memories yet. Add something important and she'll keep it encrypted on this phone. You can review or delete it here anytime.")
                .setPositiveButton("＋  Add a memory") { _,_ -> showAddMemoryDialog() }
                .setNegativeButton("Close",null)
                .show()
            return
        }
        val labels=(items.map { "✦  $it" }+"＋  Add a memory").toTypedArray()
        builder.setItems(labels) { _,which ->
            if(which==items.size) showAddMemoryDialog()
            else AlertDialog.Builder(this).setTitle("Delete this memory?")
                .setMessage(items[which]).setPositiveButton("Delete") { _,_ -> CompanionMemory.remove(this,which); toast("Memory deleted") }
                .setNegativeButton("Keep",null).show()
        }.setNeutralButton("Clear all") { _,_ ->
            AlertDialog.Builder(this).setTitle("Clear all memories?").setMessage("This cannot be undone.")
                .setPositiveButton("Clear") { _,_ -> CompanionMemory.clear(this); toast("All memories cleared") }
                .setNegativeButton("Cancel",null).show()
        }.setNegativeButton("Close",null).show()
    }

    private fun showAddMemoryDialog() {
        val input=EditText(this).apply {
            hint="Something you want her to remember"
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
            inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        AlertDialog.Builder(this).setTitle("Add a memory").setView(input)
            .setPositiveButton("Save") { _,_ ->
                if(CompanionMemory.add(this,input.text.toString())) toast("Memory saved securely ♡")
                else toast("Please enter more detail")
            }.setNegativeButton("Cancel",null).show()
    }
    override fun onInit(status: Int) { if(status==android.speech.tts.TextToSpeech.SUCCESS) CompanionVoice.configure(this,tts) }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy(){ if(::tts.isInitialized){tts.stop();tts.shutdown()}; super.onDestroy() }
}
