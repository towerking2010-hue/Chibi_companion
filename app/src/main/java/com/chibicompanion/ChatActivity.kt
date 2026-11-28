package com.chibicompanion

import android.app.Activity
import android.app.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.content.Intent
import android.provider.Settings
import android.text.InputType
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale
import java.util.concurrent.Executors

class ChatActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var log: TextView
    private lateinit var entry: EditText
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private var selectedAttachment: ChatAttachment? = null
    private lateinit var attachmentLabel: TextView
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
        log = CompanionUi.text(this, ChatHistory.read(this), 15f)
        log.setLineSpacing(CompanionUi.dp(this,3).toFloat(), 1f)
        chatCard.addView(ScrollView(this).apply { isFillViewport = true; addView(log) }, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(chatCard, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(CompanionUi.spacer(this, 12))

        entry = EditText(this).apply {
            hint = "Say something to her…"
            textSize = 15f
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
            background = CompanionUi.panel(this@ChatActivity, Color.WHITE, 18)
            setPadding(CompanionUi.dp(this@ChatActivity,16), CompanionUi.dp(this@ChatActivity,13), CompanionUi.dp(this@ChatActivity,16), CompanionUi.dp(this@ChatActivity,13))
        }
        root.addView(entry)
        val attachmentRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        attachmentRow.addView(CompanionUi.button(this, "📎 Attach") { openAttachmentPicker() },
            LinearLayout.LayoutParams(0, -2, 1f))
        attachmentRow.addView(View(this), LinearLayout.LayoutParams(CompanionUi.dp(this, 8), 1))
        attachmentRow.addView(CompanionUi.button(this, "📷 Camera") { openCamera() },
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(attachmentRow)
        attachmentLabel = CompanionUi.text(this, "No attachment selected", 11f, CompanionUi.muted)
        root.addView(attachmentLabel)
        root.addView(CompanionUi.spacer(this, 6))
        root.addView(CompanionUi.button(this, "🎙  Tap to talk") { startVoiceInput() })
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "Send message  ➜", true) {
            val message = entry.text.toString().trim()
            val attachment = selectedAttachment
            if (message.isNotEmpty() || attachment != null) {
                val userLine = (if (message.isNotBlank()) message else "") +
                    (if (attachment != null) "\n📎 " + attachment.description() else "")
                log.append("\n\nYou: " + userLine + "\n\nHer: thinking…")
                ChatHistory.add(this@ChatActivity, "You", userLine)
                entry.text.clear()
                selectedAttachment = null
                attachmentLabel.text = "No attachment selected"
                worker.execute {
                    val answer = when {
                        attachment != null && (attachment.isImage || attachment.isVideo || attachment.isPdf) -> {
                            val jpeg = when {
                                attachment.isImage -> AttachmentStore.jpegForVision(attachment)
                                attachment.isVideo -> AttachmentStore.videoPreviewJpeg(attachment)
                                else -> AttachmentStore.pdfPreviewJpeg(attachment)
                            }
                            val task = when {
                                attachment.isVideo -> "This is a contact sheet of five frames sampled from the attached video. Describe the visible events and sequence. The preview contains no audio."
                                attachment.isPdf -> "This image shows up to the first three pages of the attached PDF. Read the visible content and summarize it. Mention that only the first pages are shown."
                                else -> "Please describe this image and help me understand it."
                            }
                            if (jpeg == null) "I saved the attachment, but couldn't create a preview for analysis. Please try another file."
                            else try {
                                AiClient.replyWithImage(this@ChatActivity,
                                    if (message.isBlank()) task else message + "\\n\\n" + task, jpeg)
                            } catch (e: Exception) {
                                "I saved your attachment, but couldn't reach the vision AI provider. " +
                                    (e.message ?: "Please check your connection and provider settings.")
                            } finally { jpeg.fill(0) }
                        }
                        attachment != null && AttachmentStore.readSmallText(attachment) != null -> {
                            val extracted = AttachmentStore.readSmallText(attachment).orEmpty()
                            runCatching {
                                AiClient.reply(this@ChatActivity,
                                    (if (message.isBlank()) "Please read and summarize this text file." else message) +
                                        "\n\nUSER-ATTACHED TEXT FILE (" + attachment.displayName + "):\n" + extracted)
                            }.getOrElse { OfflineCompanion.reply(message, this@ChatActivity) }
                        }
                        attachment != null -> "I've saved " + attachment.displayName +
                            " in this conversation. I can attach it here, but this format cannot be previewed by Miko yet."
                        else -> {
                            val explicitMemory = CompanionMemory.handleExplicitRequest(this@ChatActivity, message)
                            val asksAboutScreen = message.contains("screen", true) || message.contains("what am I looking at", true) || message.contains("what is on", true)
                            val prompt = if (asksAboutScreen) message + "\n\nSCREEN CONTEXT (user requested this):\n" + ScreenContext.describe() else message
                            explicitMemory ?: PhoneActions.tryHandle(this@ChatActivity, message) ?: runCatching { AiClient.reply(this@ChatActivity, prompt) }
                                .getOrElse { OfflineCompanion.reply(message, this@ChatActivity) }
                        }
                    }
                    ChatHistory.add(this@ChatActivity, "Her", answer)
                    runOnUiThread {
                        log.text = ChatHistory.read(this@ChatActivity)
                        tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "reply")
                    }
                }
            }
        })
        root.addView(CompanionUi.spacer(this, 12))

        val settingsCard = CompanionUi.column(this, 14).apply { background = CompanionUi.panel(this@ChatActivity, Color.rgb(245,242,250), 22) }
        settingsCard.addView(CompanionUi.text(this, "VOICE & AI CONNECTIONS  ✦", 12f, CompanionUi.ink, true))
        settingsCard.addView(CompanionUi.spacer(this, 6))
        settingsCard.addView(CompanionUi.button(this, "Choose companion voice ♡") { showVoicePicker() })
        settingsCard.addView(CompanionUi.spacer(this, 6))
        settingsCard.addView(CompanionUi.button(this, "Install more voice options") {
            runCatching { startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) }
                .onFailure { toast("Open your phone's Text-to-speech settings to install voices") }
        })
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
        root.addView(settingsScroll, LinearLayout.LayoutParams(-1, CompanionUi.dp(this,280)))
        val assistantCard = CompanionUi.column(this, 12).apply {
            background = CompanionUi.panel(this@ChatActivity, Color.rgb(241,239,252), 22)
        }
        assistantCard.addView(CompanionUi.text(this, "PHONE AWARENESS & MEMORY  ✦", 12f, CompanionUi.ink, true))
        assistantCard.addView(CompanionUi.text(this, "Optional permissions stay under your control.", 11f, CompanionUi.muted))
        assistantCard.addView(CompanionUi.button(this, "Reply bubble: " + BubblePreferences.label(BubblePreferences.timeoutMs(this))) {
            val values = longArrayOf(5000L, 9000L, 15000L, BubblePreferences.NEVER)
            val labels = arrayOf("5 seconds", "9 seconds", "15 seconds", "Until I dismiss it")
            val selected = values.indexOf(BubblePreferences.timeoutMs(this)).coerceAtLeast(0)
            AlertDialog.Builder(this@ChatActivity).setTitle("How long should Miko's reply bubble stay?")
                .setSingleChoiceItems(labels, selected) { dialog, which ->
                    BubblePreferences.setTimeoutMs(this@ChatActivity, values[which])
                    dialog.dismiss()
                    toast("Reply bubble setting updated")
                }.setNegativeButton("Cancel", null).show()
        })
        assistantCard.addView(CompanionUi.button(this, "Reply language: " + MikoLanguage.label(MikoLanguage.selected(this))) {
            val values = arrayOf(MikoLanguage.NATURAL, MikoLanguage.ROMAJI, MikoLanguage.ENGLISH)
            val labels = arrayOf("Natural bilingual — English with occasional Japanese", "Japanese / Romaji — mainly short Romaji replies", "English — understand Japanese, reply in English")
            val selected = values.indexOf(MikoLanguage.selected(this)).coerceAtLeast(0)
            AlertDialog.Builder(this@ChatActivity).setTitle("How should Miko reply?")
                .setSingleChoiceItems(labels, selected) { dialog, which ->
                    MikoLanguage.set(this@ChatActivity, values[which])
                    dialog.dismiss()
                    recreate()
                }.setNegativeButton("Cancel", null).show()
        })
        assistantCard.addView(CompanionUi.button(this, "Enable / manage Screen Awareness") {
            AssistantPreferences.setScreenAwarenessEnabled(this@ChatActivity, true)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        assistantCard.addView(CompanionUi.text(this, "To inspect another app, double-tap your floating chibi and choose 'Look at the app behind me'.", 11f, CompanionUi.muted))
        assistantCard.addView(CompanionUi.text(this, "For visual screen analysis, use the floating chibi button: Look at the app behind me.", 11f, CompanionUi.muted))
        assistantCard.addView(CompanionUi.button(this, "View and manage my memories") { showMemoryManager() })
        assistantCard.addView(CompanionUi.button(this, "Proactive behavior: " + AssistantPreferences.proactiveMode(this).uppercase()) {
            val modes = arrayOf("Quiet — only respond when I interact", "Optional — occasional helpful check-ins", "Active — proactively suggest useful actions")
            val values = arrayOf(AssistantPreferences.QUIET, AssistantPreferences.OPTIONAL, AssistantPreferences.ACTIVE)
            val selected = values.indexOf(AssistantPreferences.proactiveMode(this))
            AlertDialog.Builder(this@ChatActivity).setTitle("How proactive should she be?")
                .setSingleChoiceItems(modes, selected) { dialog, which ->
                    AssistantPreferences.setProactiveMode(this@ChatActivity, values[which])
                    dialog.dismiss()
                    toast("Assistant behavior updated")
                }.setNegativeButton("Cancel", null).show()
        })
        settingsCard.addView(assistantCard)
        setContentView(root)
        if (intent.getBooleanExtra(EXTRA_OPEN_ATTACHMENT_PICKER, false)) {
            window.decorView.post { openAttachmentPicker() }
        }
        if (intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)) {
            window.decorView.post { startVoiceInput() }
        }
    }

    private fun openAttachmentPicker() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "image/*", "video/*", "application/pdf", "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }
        runCatching { startActivityForResult(picker, REQUEST_ATTACHMENT) }
            .onFailure { toast("Couldn't open the file picker") }
    }

    private fun openCamera() {
        val camera = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (camera.resolveActivity(packageManager) == null) {
            toast("No camera app is available")
            return
        }
        runCatching { startActivityForResult(camera, REQUEST_CAMERA) }
            .onFailure { toast("Couldn't open the camera") }
    }

    @Deprecated("Legacy activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        try {
            when (requestCode) {
                REQUEST_ATTACHMENT -> {
                    val uri = data?.data ?: return
                    selectedAttachment = AttachmentStore.importUri(this, uri)
                    attachmentLabel.text = "Selected: " + (selectedAttachment?.description() ?: "")
                    toast("Attachment added ♡")
                }
                REQUEST_CAMERA -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    if (bitmap == null) {
                        toast("The camera didn't return a photo")
                        return
                    }
                    selectedAttachment = AttachmentStore.saveCameraBitmap(this, bitmap)
                    bitmap.recycle()
                    attachmentLabel.text = "Selected: " + (selectedAttachment?.description() ?: "")
                    toast("Photo added ♡")
                }
            }
        } catch (e: Exception) {
            toast("Couldn't add that attachment: " + (e.message ?: "unknown error"))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::log.isInitialized) log.text = ChatHistory.read(this)
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Speech recognition is not available on this phone")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), VOICE_PERMISSION_REQUEST)
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.destroy()
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { toast("Listening… speak naturally") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { toast("Finishing…") }
            override fun onError(error: Int) { toast("I couldn't hear that clearly. Try again.") }
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!words.isNullOrBlank()) entry.setText(words)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    @Deprecated("Android permission callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == VOICE_PERMISSION_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else if (requestCode == VOICE_PERMISSION_REQUEST) {
            toast("Microphone permission is needed for voice input")
        }
    }

    private fun isScreenServiceEnabled(): Boolean {
        val expected = android.content.ComponentName(this, CompanionAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals(expected, true) }
    }

    private fun showMemoryManager() {
        val items = CompanionMemory.all(this)
        val labels = (items.map { "✦  $it" } + "＋  Add a memory").toTypedArray()
        AlertDialog.Builder(this).setTitle("Her memories — stored on this phone")
            .setItems(labels) { _, which ->
                if (which == items.size) {
                    val field = EditText(this).apply { hint = "Something you want her to remember"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES }
                    AlertDialog.Builder(this).setTitle("Add a memory").setView(field)
                        .setPositiveButton("Save") { _, _ ->
                            if (CompanionMemory.add(this, field.text.toString())) toast("Memory saved securely ♡")
                            else toast("Please enter a little more detail")
                        }.setNegativeButton("Cancel", null).show()
                } else {
                    AlertDialog.Builder(this).setTitle("Delete this memory?")
                        .setMessage(items[which]).setPositiveButton("Delete") { _, _ ->
                            CompanionMemory.remove(this, which); toast("Memory deleted")
                        }.setNegativeButton("Keep", null).show()
                }
            }.setNeutralButton("Clear all") { _, _ ->
                AlertDialog.Builder(this).setTitle("Clear all memories?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Clear") { _, _ -> CompanionMemory.clear(this); toast("All memories cleared") }
                    .setNegativeButton("Cancel", null).show()
            }.setNegativeButton("Close", null).show()
    }

    private fun showVoicePicker() {
        val voices = CompanionVoice.options(tts)
        if (voices.isEmpty()) {
            AlertDialog.Builder(this).setTitle("No extra voices found")
                .setMessage("Your phone's text-to-speech engine has not exposed any English voices. Use 'Install more voice options' and download a voice pack, then return here.")
                .setPositiveButton("OK", null).show()
            return
        }
        val selected = CompanionVoice.selectedName(this)
        val labels = voices.map { CompanionVoice.label(it) }.toTypedArray()
        val checked = voices.indexOfFirst { it.name == selected }
        AlertDialog.Builder(this)
            .setTitle("Choose her voice")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                CompanionVoice.select(this, tts, voices[which])
                toast("Voice updated ♡")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) CompanionVoice.configure(this, tts)
    }
    companion object {
        private const val VOICE_PERMISSION_REQUEST = 702
        private const val REQUEST_ATTACHMENT = 703
        private const val REQUEST_CAMERA = 704
        const val EXTRA_OPEN_ATTACHMENT_PICKER = "open_attachment_picker"
        const val EXTRA_START_VOICE_INPUT = "start_voice_input"
    }

    private fun toast(s: String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy() {
        worker.shutdownNow(); speechRecognizer?.destroy(); tts.stop(); tts.shutdown(); super.onDestroy()
    }
}
