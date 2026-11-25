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
    private lateinit var avatar: ChibiView
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private var replySequence = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=CompanionUi.bg
        window.navigationBarColor=CompanionUi.bg
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        tts=TextToSpeech(this,this)

        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(CompanionUi.dp(this@ChatActivity,16),CompanionUi.dp(this@ChatActivity,12),CompanionUi.dp(this@ChatActivity,16),CompanionUi.dp(this@ChatActivity,10))
            setBackgroundColor(CompanionUi.bg)
        }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val titles=CompanionUi.column(this)
        titles.addView(CompanionUi.text(this,"MIKO • YOUR COMPANION",10f,CompanionUi.pink,true))
        titles.addView(CompanionUi.text(this,"Let's talk ♡",22f,CompanionUi.ink,true))
        header.addView(titles,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(CompanionUi.button(this,"⚙") { startActivity(Intent(this,SettingsActivity::class.java)) })
        root.addView(header)

        val character=LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL
            background=CompanionUi.gradient(this@ChatActivity,android.graphics.Color.rgb(255,235,245),android.graphics.Color.rgb(231,237,255),22)
            setPadding(CompanionUi.dp(this@ChatActivity,8),CompanionUi.dp(this@ChatActivity,4),CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,4))
        }
        avatar=MikoSpriteView(this).apply { setMood("happy") }
        character.addView(avatar,LinearLayout.LayoutParams(CompanionUi.dp(this,112),CompanionUi.dp(this,150)))
        val greeting=CompanionUi.column(this,4)
        greeting.addView(CompanionUi.text(this,"Miko is here!",17f,CompanionUi.ink,true))
        greeting.addView(CompanionUi.text(this,"Tell me what's on your mind.",12f,CompanionUi.muted))
        character.addView(greeting)
        val charLp=LinearLayout.LayoutParams(-1,CompanionUi.dp(this,158)); charLp.topMargin=CompanionUi.dp(this,10)
        root.addView(character,charLp)

        val transcriptCard=CompanionUi.column(this,14).apply { background=CompanionUi.panel(this@ChatActivity,android.graphics.Color.WHITE,22) }
        transcript=CompanionUi.text(this,ChatHistory.read(this).ifBlank { "Miko: Ehehe! You came to see me! ✦\n\nMiko: What's on your mind?" },14f,CompanionUi.ink)
        transcript.setLineSpacing(CompanionUi.dp(this,4).toFloat(),1f)
        val scroll=ScrollView(this).apply { isFillViewport=true; addView(transcript) }
        transcriptCard.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val cardLp=LinearLayout.LayoutParams(-1,0,1f); cardLp.topMargin=CompanionUi.dp(this,12); cardLp.bottomMargin=CompanionUi.dp(this,10)
        root.addView(transcriptCard,cardLp)

        val composer=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; orientation=LinearLayout.HORIZONTAL }
        input=EditText(this).apply {
            hint="Message Miko…"; textSize=14f; maxLines=3
            setTextColor(CompanionUi.ink); setHintTextColor(CompanionUi.muted)
            background=CompanionUi.panel(this@ChatActivity,android.graphics.Color.WHITE,18)
            setPadding(CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,12),CompanionUi.dp(this@ChatActivity,14),CompanionUi.dp(this@ChatActivity,12))
        }
        composer.addView(input,LinearLayout.LayoutParams(0,-2,1f))
        val mic=CompanionUi.button(this,"🎙") { startVoiceInput() }
        val micLp=LinearLayout.LayoutParams(CompanionUi.dp(this,54),CompanionUi.dp(this,52)); micLp.leftMargin=CompanionUi.dp(this,6)
        composer.addView(mic,micLp)
        val send=CompanionUi.button(this,"➤",true) { sendMessage() }
        val sendLp=LinearLayout.LayoutParams(CompanionUi.dp(this,54),CompanionUi.dp(this,52)); sendLp.leftMargin=CompanionUi.dp(this,6)
        composer.addView(send,sendLp)
        root.addView(composer)
        root.addView(CompanionUi.text(this,"Cloud AI uses only a provider key you add in Settings. Without one, Miko uses her basic offline fallback.",10f,CompanionUi.muted).apply { gravity=Gravity.CENTER; setPadding(0,CompanionUi.dp(this@ChatActivity,8),0,0) })
        setContentView(root)
    }

    private fun sendMessage() {
        val message=input.text.toString().trim()
        if(message.isEmpty()) return
        input.text.clear()
        ChatHistory.add(this,"You",message)
        appendLine("\n\nYou: $message\n\nMiko: thinking…")
        avatar.setMood("happy")
        val sequence=++replySequence
        worker.execute {
            val answer=CompanionMemory.handleExplicitRequest(this,message)
                ?: PhoneActions.tryHandle(this,message)
                ?: runCatching { AiClient.reply(this,message) }.getOrElse { error ->
                    val detail=error.message?.take(150)?.let { " ($it)" }.orEmpty()
                    "I couldn't reach a configured AI provider$detail. I'm using my basic offline fallback for now.\n\n${OfflineCompanion.reply(message)}"
                }
            ChatHistory.add(this,"Miko",answer)
            runOnUiThread {
                if(isFinishing || isDestroyed) return@runOnUiThread
                transcript.text=ChatHistory.read(this)
                avatar.setMood(MikoEmotion.fromReply(answer))
                if(::tts.isInitialized) {
                    CompanionVoice.configure(this,tts)
                    tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"miko-$sequence")
                }
            }
        }
    }

    private fun appendLine(value:String) {
        transcript.append(value)
        (transcript.parent as? ScrollView)?.post { (transcript.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN) }
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
                    if(!words.isNullOrBlank()) input.setText(words)
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
            tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
                override fun onStart(utteranceId:String?){runOnUiThread{avatar.setSpeaking(true)}}
                override fun onDone(utteranceId:String?){runOnUiThread{avatar.setSpeaking(false)}}
                override fun onError(utteranceId:String?){runOnUiThread{avatar.setSpeaking(false)}}
            })
        }
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy(){
        worker.shutdownNow(); recognizer?.destroy()
        if(::tts.isInitialized){tts.stop();tts.shutdown()}
        super.onDestroy()
    }
    companion object { private const val VOICE_PERMISSION=702 }
}
