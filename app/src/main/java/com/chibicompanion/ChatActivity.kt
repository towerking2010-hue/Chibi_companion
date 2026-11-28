package com.chibicompanion

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

class ChatActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var transcriptList: LinearLayout
    private lateinit var transcriptScroll: ScrollView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private var replySequence = 0
    private lateinit var avatar: MikoSpriteView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CompanionUi.applySystemBars(this)
        tts=TextToSpeech(this,this)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(CompanionUi.dp(this@ChatActivity,18),CompanionUi.dp(this@ChatActivity,16),CompanionUi.dp(this@ChatActivity,18),CompanionUi.dp(this@ChatActivity,14))
            setBackgroundColor(CompanionUi.bg)
        }
        val header=LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL
            background=CompanionUi.panel(this@ChatActivity,CompanionUi.white,18)
            setPadding(CompanionUi.dp(this@ChatActivity,12),CompanionUi.dp(this@ChatActivity,8),
                CompanionUi.dp(this@ChatActivity,8),CompanionUi.dp(this@ChatActivity,8))
        }
        val titles=CompanionUi.column(this)
        titles.addView(CompanionUi.eyebrow(this,"Your companion"))
        titles.addView(CompanionUi.heading(this,"Chat with Miko ♡",22f))
        header.addView(titles,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(CompanionUi.button(this,"Clear") {
            MikoSheet.show(this, "CHAT", "Start fresh?", "This clears the saved conversation from this phone. You can keep chatting with Miko afterwards.", actionLabel = "Clear conversation") {
                ChatHistory.clear(this)
                transcript.text = "Miko: Hiii! A fresh start. What would you like to do? ✦"
            }
        })
        header.addView(CompanionUi.button(this,"Done") { finish() })
        header.addView(CompanionUi.button(this,"⚙") { startActivity(Intent(this,SettingsActivity::class.java)) }, LinearLayout.LayoutParams(CompanionUi.dp(this,48),CompanionUi.dp(this,48)))
        root.addView(header)
        avatar = MikoSpriteView(this)
        root.addView(avatar, LinearLayout.LayoutParams(-1, CompanionUi.dp(this,170)).apply {
            topMargin=CompanionUi.dp(this@ChatActivity,8)
        })
        val transcriptCard=CompanionUi.column(this,16).apply {
            background=CompanionUi.panel(this@ChatActivity,CompanionUi.white,24)
            elevation=CompanionUi.dp(this@ChatActivity,2).toFloat()
        }
        transcriptList = CompanionUi.column(this, 4)
        transcriptScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, CompanionUi.dp(this@ChatActivity, 8))
            addView(transcriptList)
        }
        transcriptCard.addView(transcriptScroll,LinearLayout.LayoutParams(-1,0,1f))
        val cardLp=LinearLayout.LayoutParams(-1,0,1f); cardLp.topMargin=CompanionUi.dp(this,12); cardLp.bottomMargin=CompanionUi.dp(this,10)
        root.addView(transcriptCard,cardLp)
        val composer=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; orientation=LinearLayout.HORIZONTAL }
        input=EditText(this).apply {
            hint="Message or give a command…"; textSize=14f; maxLines=3
            setTextColor(CompanionUi.ink); setHintTextColor(CompanionUi.muted)
            background=CompanionUi.panel(this@ChatActivity,CompanionUi.surfaceRaised,18)
            minHeight=CompanionUi.dp(this@ChatActivity,50)
            setPadding(CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,12),CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,12))
        }
        composer.addView(input,LinearLayout.LayoutParams(0,-2,1f))
        composer.addView(CompanionUi.button(this,"🎙") { startVoiceInput() },LinearLayout.LayoutParams(CompanionUi.dp(this,54),CompanionUi.dp(this,52)).apply { leftMargin=CompanionUi.dp(this@ChatActivity,6) })
        composer.addView(CompanionUi.button(this,"➤",true) { sendMessage() },LinearLayout.LayoutParams(CompanionUi.dp(this,54),CompanionUi.dp(this,52)).apply { leftMargin=CompanionUi.dp(this@ChatActivity,6) })
        root.addView(composer)
        root.addView(CompanionUi.text(this,"Voice uses your phone's speech services. Cloud AI is optional; offline replies are basic.",10f,CompanionUi.muted).apply {
            gravity=Gravity.CENTER; setPadding(0,CompanionUi.dp(this@ChatActivity,8),0,0)
        })
        setContentView(root)
        renderTranscript()
        intent.getStringExtra(EXTRA_PROMPT)?.let { prompt ->
            input.setText(prompt); input.setSelection(input.text.length)
            if (prompt.endsWith(" ")) input.requestFocus() else input.post { sendMessage() }
        }
    }

    private fun renderTranscript(showThinking: Boolean = false) {
        if (!::transcriptList.isInitialized) return
        transcriptList.removeAllViews()
        val saved = ChatHistory.read(this)
        val entries = saved.split("\n\n").filter { it.isNotBlank() }
        if (entries.isEmpty()) {
            addMessageBubble("Miko", "Hiii! I'm here. What can I help you with? ✦")
        } else {
            entries.forEach { entry ->
                val separator = entry.indexOf(": ")
                val role = if (separator > 0) entry.substring(0, separator).trim() else "Miko"
                val message = if (separator > 0) entry.substring(separator + 2).trim() else entry
                addMessageBubble(role, message)
            }
        }
        if (showThinking) addMessageBubble("Miko", "Thinking…", isThinking = true)
        transcriptScroll.post { transcriptScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addMessageBubble(role: String, message: String, isThinking: Boolean = false) {
        val fromUser = role.equals("You", true)
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(0, CompanionUi.dp(this@ChatActivity, 4), 0, CompanionUi.dp(this@ChatActivity, 4))
        }
        val bubble = CompanionUi.column(this, 12).apply {
            background = if (fromUser) CompanionUi.panel(this@ChatActivity, Color.rgb(244, 240, 250), 18)
                else CompanionUi.gradient(this@ChatActivity, Color.rgb(255, 237, 246), Color.rgb(239, 234, 255), 18)
        }
        bubble.addView(CompanionUi.eyebrow(this, if (fromUser) "YOU" else "MIKO"))
        bubble.addView(CompanionUi.text(this, message, 14f, CompanionUi.ink, isThinking).apply {
            maxWidth = CompanionUi.dp(this@ChatActivity, 292)
        })
        line.addView(bubble, LinearLayout.LayoutParams(-2, -2))
        transcriptList.addView(line, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendMessage() {
        val message=input.text.toString().trim()
        if(message.isEmpty()) return
        input.text.clear()
        ChatHistory.add(this,"You",message)
        renderTranscript(showThinking = true)
        val sequence=++replySequence
        worker.execute {
            val answer=CompanionMemory.handleExplicitRequest(this,message)
                ?: PhoneActions.tryHandle(this,message)
                ?: runCatching { AiClient.reply(this,message) }.getOrElse { error ->
                    val detail=error.message?.take(150)?.let { " ("+it+")" }.orEmpty()
                    OfflineCompanion.reply(message)
                }
            ChatHistory.add(this,"Miko",answer)
            runOnUiThread {
                if(isFinishing || isDestroyed) return@runOnUiThread
                renderTranscript()
                if(::tts.isInitialized) {
                    CompanionVoice.configure(this,tts)
                    tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"miko-"+sequence)
                }
            }
        }
    }

    private fun startVoiceInput() {
        if(!SpeechRecognizer.isRecognitionAvailable(this)){ toast("Speech recognition isn't available on this phone"); return }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),VOICE_PERMISSION); return
        }
        recognizer?.destroy()
        recognizer=SpeechRecognizer.createSpeechRecognizer(this).also { r ->
            r.setRecognitionListener(object:RecognitionListener {
                override fun onReadyForSpeech(params:Bundle?){toast("Listening…")}
                override fun onBeginningOfSpeech(){}
                override fun onRmsChanged(rmsdB:Float){}
                override fun onBufferReceived(buffer:ByteArray?){}
                override fun onEndOfSpeech(){}
                override fun onError(error:Int){toast("I couldn't hear that clearly. Try again.")}
                override fun onResults(results:Bundle?){
                    val words=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if(!words.isNullOrBlank()) { input.setText(words); sendMessage() }
                }
                override fun onPartialResults(partialResults:Bundle?){}
                override fun onEvent(eventType:Int,params:Bundle?){}
            })
            val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            }
            r.startListening(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==VOICE_PERMISSION && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) startVoiceInput()
        else if(requestCode==VOICE_PERMISSION) toast("Microphone permission is needed for voice input")
    }
    override fun onInit(status:Int) {
        if(status==TextToSpeech.SUCCESS) {
            CompanionVoice.configure(this,tts)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { runOnUiThread { if (::avatar.isInitialized) avatar.startTalking() } }
                override fun onDone(utteranceId: String?) { runOnUiThread { if (::avatar.isInitialized) avatar.stopTalking() } }
                override fun onError(utteranceId: String?) { runOnUiThread { if (::avatar.isInitialized) avatar.stopTalking() } }
            })
        }
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy(){
        worker.shutdownNow(); recognizer?.destroy()
        if(::tts.isInitialized){tts.stop();tts.shutdown()}
        super.onDestroy()
    }
    companion object {
        const val EXTRA_PROMPT="com.chibicompanion.EXTRA_PROMPT"
        private const val VOICE_PERMISSION=702
    }
}
