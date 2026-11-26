package com.chibicompanion

import android.app.Activity
import android.Manifest
import android.content.Intent
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
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private var replySequence = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=CompanionUi.bg
        window.navigationBarColor=CompanionUi.bg
        window.decorView.systemUiVisibility=0
        tts=TextToSpeech(this,this)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(CompanionUi.dp(this@ChatActivity,16),CompanionUi.dp(this@ChatActivity,12),CompanionUi.dp(this@ChatActivity,16),CompanionUi.dp(this@ChatActivity,10))
            setBackgroundColor(CompanionUi.bg)
        }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val titles=CompanionUi.column(this)
        titles.addView(CompanionUi.text(this,"MIKO • PERSONAL ASSISTANT",10f,CompanionUi.pink,true))
        titles.addView(CompanionUi.text(this,"Chat with Miko ♡",22f,CompanionUi.ink,true))
        header.addView(titles,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(CompanionUi.button(this,"Clear") {
            android.app.AlertDialog.Builder(this).setTitle("Clear chat history?")
                .setMessage("This removes the saved conversation from this phone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ -> ChatHistory.clear(this); transcript.text = "Miko: Hiii! A fresh start. What would you like to do? ✦" }
                .show()
        })
        header.addView(CompanionUi.button(this,"×") { stopService(Intent(this,FloatingCompanionService::class.java)); toast("Floating Miko stopped") }, LinearLayout.LayoutParams(CompanionUi.dp(this,44),CompanionUi.dp(this,48)))
        header.addView(CompanionUi.button(this,"⚙") { startActivity(Intent(this,SettingsActivity::class.java)) }, LinearLayout.LayoutParams(CompanionUi.dp(this,48),CompanionUi.dp(this,48)))
        root.addView(header)
        root.addView(ModelSlotView(this), LinearLayout.LayoutParams(-1, CompanionUi.dp(this,112)).apply {
            topMargin=CompanionUi.dp(this@ChatActivity,8)
        })
        val transcriptCard=CompanionUi.column(this,14).apply { background=CompanionUi.panel(this@ChatActivity,android.graphics.Color.rgb(30,36,65),22) }
        transcript=CompanionUi.text(this,ChatHistory.read(this).ifBlank { "Miko: Hiii! I'm here. What can I help you with? ✦" },14f,CompanionUi.ink)
        transcript.setLineSpacing(CompanionUi.dp(this,4).toFloat(),1f)
        val scroll=ScrollView(this).apply { isFillViewport=true; addView(transcript) }
        transcriptCard.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val cardLp=LinearLayout.LayoutParams(-1,0,1f); cardLp.topMargin=CompanionUi.dp(this,12); cardLp.bottomMargin=CompanionUi.dp(this,10)
        root.addView(transcriptCard,cardLp)
        val composer=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; orientation=LinearLayout.HORIZONTAL }
        input=EditText(this).apply {
            hint="Message or give a command…"; textSize=14f; maxLines=3
            setTextColor(CompanionUi.ink); setHintTextColor(CompanionUi.muted)
            background=CompanionUi.panel(this@ChatActivity,android.graphics.Color.rgb(37,43,76),18)
            setPadding(CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,12),CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,12))
        }
        composer.addView(input,LinearLayout.LayoutParams(0,-2,1f))
        composer.addView(CompanionUi.button(this,"🎙") { startVoiceInput() },LinearLayout.LayoutParams(CompanionUi.dp(this,54),CompanionUi.dp(this,52)).apply { leftMargin=CompanionUi.dp(this@ChatActivity,6) })
        composer.addView(CompanionUi.button(this,"➤",true) { sendMessage() },LinearLayout.LayoutParams(CompanionUi.dp(this,54),CompanionUi.dp(this,52)).apply { leftMargin=CompanionUi.dp(this@ChatActivity,6) })
        root.addView(composer)
        root.addView(CompanionUi.text(this,"Voice uses Android speech recognition and installed text-to-speech. Cloud AI requires your own provider key; offline mode is basic.",10f,CompanionUi.muted).apply {
            gravity=Gravity.CENTER; setPadding(0,CompanionUi.dp(this@ChatActivity,8),0,0)
        })
        setContentView(root)
        intent.getStringExtra(EXTRA_PROMPT)?.let { prompt ->
            input.setText(prompt); input.setSelection(input.text.length)
            if (prompt.endsWith(" ")) input.requestFocus() else input.post { sendMessage() }
        }
    }

    private fun sendMessage() {
        val message=input.text.toString().trim()
        if(message.isEmpty()) return
        input.text.clear()
        ChatHistory.add(this,"You",message)
        transcript.text=ChatHistory.read(this)+"\n\nMiko: thinking…"
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
                transcript.text=ChatHistory.read(this)
                (transcript.parent as? ScrollView)?.post { (transcript.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN) }
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
        if(status==TextToSpeech.SUCCESS) CompanionVoice.configure(this,tts)
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
