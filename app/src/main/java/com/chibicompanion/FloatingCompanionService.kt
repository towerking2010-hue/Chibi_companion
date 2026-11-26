package com.chibicompanion

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.util.Locale
import java.util.concurrent.Executors

class FloatingCompanionService : Service(), TextToSpeech.OnInitListener {
    private lateinit var wm: WindowManager
    private var avatar: ModelSlotView? = null
    private var chatPanel: LinearLayout? = null
    private var chatParams: WindowManager.LayoutParams? = null
    private var avatarParams: WindowManager.LayoutParams? = null
    private var lastTap = 0L
    private var downX = 0f; private var downY = 0f; private var originX = 0; private var originY = 0; private var downAt = 0L; private var dragging = false
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
        startForeground(41, notification())
        showAvatar()
    }

    private fun notification(): Notification {
        val id = "chibi_companion"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(id, "Floating companion", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, FloatingCompanionService::class.java).setAction("STOP_COMPANION"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, id).setContentTitle("Miko is here")
            .setContentText("Double-tap Miko to chat").setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(open).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn off", stop).setOngoing(true).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_COMPANION") { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == ScreenCaptureActivity.ACTION_SCREEN_CAPTURE_READY) {
            handleCapturedScreen()
            return START_STICKY
        }
        return START_STICKY
    }

    private fun overlayType() = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private fun showAvatar() {
        val v = ModelSlotView(this)
        // WindowManager.LayoutParams dimensions are pixels, so convert dp explicitly.
        // The old 136x164 raw-pixel window rendered Miko as a tiny sticker on high-density phones.
        val overlayWidth = CompanionUi.dp(this, 136)
        val overlayHeight = CompanionUi.dp(this, 178)
        val lp = WindowManager.LayoutParams(overlayWidth, overlayHeight, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 8; y = CompanionUi.dp(this@FloatingCompanionService, 150) }
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX=e.rawX; downY=e.rawY; originX=lp.x; originY=lp.y
                    downAt=System.currentTimeMillis(); dragging=false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx=e.rawX-downX; val dy=e.rawY-downY
                    if (kotlin.math.abs(dx)>8 || kotlin.math.abs(dy)>8) dragging=true
                    if (dragging) {
                        lp.x=originX+dx.toInt(); lp.y=originY+dy.toInt()
                        runCatching { wm.updateViewLayout(v,lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val now=System.currentTimeMillis()
                    val held=now-downAt
                    if (dragging) {
                        // Dock to the nearest screen edge after the spring-release animation.
                        val metrics = resources.displayMetrics
                        val maxX = (metrics.widthPixels - lp.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - lp.height - CompanionUi.dp(this, 28)).coerceAtLeast(0)
                        lp.x = if (lp.x + lp.width / 2 < metrics.widthPixels / 2) 0 else maxX
                        lp.y = lp.y.coerceIn(CompanionUi.dp(this, 32), maxY)
                        runCatching { wm.updateViewLayout(v, lp) }
                    } else if (held>=550L) {
                        lastTap=0L
                    } else if (now-lastTap < 420L) {
                        lastTap=0L
                        toggleChat()
                    } else {
                        lastTap=now
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> { v.clearDragMotion(); true }
                else -> false
            }
        }
        runCatching { wm.addView(v,lp); avatar=v; avatarParams=lp }.onFailure { stopSelf() }
    }

    private fun toggleChat() { if (chatPanel == null) showChat() else hideChat() }
    private fun showChat() {
        val panel = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(16,14,16,12)
            background=GradientDrawable().apply { setColor(Color.rgb(20,27,53)); cornerRadius=28f; setStroke(2,Color.rgb(104,112,164)) }
            elevation=18f
        }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text="Miko • Assistant"; textSize=18f; setTextColor(Color.rgb(226,224,255)) }, LinearLayout.LayoutParams(0,-2,1f))
        header.addView(Button(this).apply { text="⚙"; setOnClickListener {
            startActivity(Intent(this@FloatingCompanionService, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } })
        header.addView(Button(this).apply { text="×"; setOnClickListener { hideChat() } })
        panel.addView(header)
        panel.addView(Button(this).apply {
            text = "👀 Look at the app behind me"
            setOnClickListener {
                val consent = AlertDialog.Builder(this@FloatingCompanionService)
                    .setTitle("Share one screen capture?")
                    .setMessage("Android will ask for screen-capture permission. If you continue, one screenshot of the app behind the chibi will be sent to your configured AI provider for analysis. Avoid passwords, banking screens, private chats or other sensitive content.")
                    .setPositiveButton("Continue") { _, _ ->
                        hideChat()
                        startActivity(Intent(this@FloatingCompanionService, ScreenCaptureActivity::class.java)
                            .putExtra(ScreenCaptureActivity.EXTRA_NOTIFY_SERVICE, true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    .setNegativeButton("Cancel", null).create()
                consent.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                consent.show()
            }
        })
        val transcript=TextView(this).apply { text=ChatHistory.read(this@FloatingCompanionService); textSize=15f; setTextColor(Color.rgb(245,242,255)); setPadding(4,12,4,12) }
        val scroll=ScrollView(this).apply { addView(transcript) }
        panel.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val input=EditText(this).apply { hint="Talk to Miko…"; maxLines=3; textSize=15f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY) }
        row.addView(input,LinearLayout.LayoutParams(0,-2,1f))
        row.addView(Button(this).apply { text="Send"; setOnClickListener {
            val msg=input.text.toString().trim(); if(msg.isNotEmpty()) { input.text.clear(); ChatHistory.add(this@FloatingCompanionService,"You",msg); transcript.append("\n\nYou: $msg\nHer: …"); scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                worker.execute {
                    val answer = CompanionMemory.handleExplicitRequest(this@FloatingCompanionService, msg)
                        ?: PhoneActions.tryHandle(this@FloatingCompanionService,msg)
                        ?: runCatching { AiClient.reply(this@FloatingCompanionService,msg) }
                            .getOrElse { error ->
                                val detail = error.message?.take(180)?.let { " ($it)" }.orEmpty()
                                "I could not reach your configured AI provider$detail. I am switching to basic offline mode; my reply may be limited.\n\n${OfflineCompanion.reply(msg)}"
                            }
                    ChatHistory.add(this@FloatingCompanionService,"Miko",answer)
                    mainThread {
                        transcript.text=ChatHistory.read(this@FloatingCompanionService)
                        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                        val mood = MikoEmotion.fromReply(answer)
                        if (::tts.isInitialized) {
                            CompanionVoice.configure(this@FloatingCompanionService, tts)
                            tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"chibi-reply")
                        }
                    }
                }
            }
        } })
        panel.addView(row)
        val lp=WindowManager.LayoutParams((resources.displayMetrics.widthPixels-28).coerceAtLeast(280), (resources.displayMetrics.heightPixels*0.56f).toInt(), overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT).apply { gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y=20; softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
        lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        runCatching { wm.addView(panel,lp); chatPanel=panel; chatParams=lp; input.requestFocus(); input.post { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT) } }
    }

    private fun handleCapturedScreen() {
        val screenshot = ScreenCaptureStore.latestJpeg ?: return
        ScreenCaptureStore.latestJpeg = null
        worker.execute {
            val answer = runCatching {
                AiClient.replyWithImage(this@FloatingCompanionService,
                    "Describe the app and content visible on my screen. Help me understand or interact with it. Be careful with sensitive information.", screenshot)
            }.getOrElse {
                "I captured the screen, but I couldn't understand the image. Connect a vision-capable provider such as Gemini and try again."
            }
            screenshot.fill(0)
            ChatHistory.add(this@FloatingCompanionService, "Her", answer)
            mainThread {
                if (::tts.isInitialized) {
                    CompanionVoice.configure(this@FloatingCompanionService, tts)
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "screen-vision")
                }
            }
        }
    }

    private fun hideChat() { chatPanel?.let { runCatching { wm.removeView(it) } }; chatPanel=null; chatParams=null }
    private fun mainThread(block:()->Unit) { android.os.Handler(mainLooper).post(block) }
    override fun onInit(status:Int) {
        if(status==TextToSpeech.SUCCESS) {
            CompanionVoice.configure(this, tts)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { }
                override fun onDone(utteranceId: String?) {
                    
                }
                override fun onError(utteranceId: String?) { mainThread { avatar?.setSpeaking(false) } }
            })
        }
    }
    override fun onDestroy() {
        hideChat(); avatar?.let { runCatching { wm.removeView(it) } }; avatar=null
        worker.shutdownNow(); if(::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
