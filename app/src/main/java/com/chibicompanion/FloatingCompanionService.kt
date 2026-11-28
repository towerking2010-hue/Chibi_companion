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
    private var avatar: MikoSpriteView? = null
    private var chatPanel: LinearLayout? = null
    private var chatParams: WindowManager.LayoutParams? = null
    private var bubbleView: TextView? = null
    private var bubbleDismiss: Runnable? = null
    private var avatarParams: WindowManager.LayoutParams? = null
    private var lastTap = 0L
    private var downX = 0f; private var downY = 0f; private var originX = 0; private var originY = 0; private var dragging = false
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
        return Notification.Builder(this, id).setContentTitle("Chibi Companion is active")
            .setContentText("Double-tap your companion to chat").setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(open).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn off", stop).setOngoing(true).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_COMPANION") { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == "RELOAD_MIKO_ASSETS") {
            avatar?.let { runCatching { wm.removeView(it) } }
            avatar = null
            showAvatar()
            return START_STICKY
        }
        if (intent?.action == ScreenCaptureActivity.ACTION_SCREEN_CAPTURE_READY) {
            handleCapturedScreen()
            return START_STICKY
        }
        return START_STICKY
    }

    private fun overlayType() = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private fun showAvatar() {
        val v = MikoSpriteView(this)
        val lp = WindowManager.LayoutParams(136, 164, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 12; y = 220 }
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; originX=lp.x; originY=lp.y; dragging=false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=e.rawX-downX; val dy=e.rawY-downY
                    if (kotlin.math.abs(dx)>8 || kotlin.math.abs(dy)>8) dragging=true
                    if (dragging) { lp.x=originX+dx.toInt(); lp.y=originY+dy.toInt(); runCatching { wm.updateViewLayout(v,lp) } }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val now=System.currentTimeMillis()
                    if (!dragging && now-lastTap < 420) { lastTap=0L; toggleChat() } else if (!dragging) lastTap=now
                    true
                }
                else -> false
            }
        }
        runCatching { wm.addView(v,lp); avatar=v; avatarParams=lp }.onFailure { stopSelf() }
    }

    private fun toggleChat() { if (chatPanel == null) showChat() else hideChat() }
    /** Compact quick-chat bar. The full transcript remains in ChatActivity. */
    private fun showChat() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            background = GradientDrawable().apply {
                setColor(Color.rgb(250, 248, 255))
                cornerRadius = 24f
                setStroke(2, Color.rgb(174, 196, 239))
            }
            elevation = 18f
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Miko ♡"
            textSize = 15f
            setTextColor(Color.rgb(53, 70, 111))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Button(this).apply {
            text = "Attach"
            setOnClickListener {
                hideChat()
                startActivity(Intent(this@FloatingCompanionService, ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_OPEN_ATTACHMENT_PICKER, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        header.addView(Button(this).apply {
            text = "Voice"
            setOnClickListener {
                hideChat()
                startActivity(Intent(this@FloatingCompanionService, ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_START_VOICE_INPUT, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        header.addView(Button(this).apply {
            text = "Full chat"
            setOnClickListener {
                hideChat()
                startActivity(Intent(this@FloatingCompanionService, ChatActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        header.addView(Button(this).apply { text = "×"; setOnClickListener { hideChat() } })
        panel.addView(header)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = "Message Miko…"
            maxLines = 3
            textSize = 15f
            setPadding(12, 8, 12, 8)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 18f
            }
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "➤"
            contentDescription = "Send message to Miko"
            setOnClickListener {
                val msg = input.text.toString().trim()
                if (msg.isNotEmpty()) {
                    input.text.clear()
                    ChatHistory.add(this@FloatingCompanionService, "You", msg)
                    hideChat()
                    showBubble("Thinking…")
                    worker.execute {
                        val answer = CompanionMemory.handleExplicitRequest(this@FloatingCompanionService, msg)
                            ?: PhoneActions.tryHandle(this@FloatingCompanionService, msg)
                            ?: runCatching { AiClient.reply(this@FloatingCompanionService, msg) }
                                .getOrElse { OfflineCompanion.reply(msg, this@FloatingCompanionService) }
                        ChatHistory.add(this@FloatingCompanionService, "Her", answer)
                        mainThread {
                            showBubble(answer)
                            avatar?.setMood(MikoEmotion.fromReply(answer))
                            if (::tts.isInitialized) {
                                CompanionVoice.configure(this@FloatingCompanionService, tts)
                                tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "miko-quick-reply")
                            }
                            bubbleDismiss?.let { android.os.Handler(mainLooper).removeCallbacks(it) }
                            val dismiss = Runnable { hideBubble() }
                            bubbleDismiss = dismiss
                            val timeout = BubblePreferences.timeoutMs(this@FloatingCompanionService)
                            if (timeout > 0L) android.os.Handler(mainLooper).postDelayed(dismiss, timeout)
                            android.os.Handler(mainLooper).postDelayed({ avatar?.setMood("neutral") }, 3500)
                        }
                    }
                }
            }
        })
        panel.addView(row)

        val lp = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels - 28).coerceAtLeast(260),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 24
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        runCatching {
            wm.addView(panel, lp)
            chatPanel = panel
            chatParams = lp
            input.requestFocus()
            input.post {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    /** A short-lived reply bubble sits above the avatar; tapping it opens the full chat. */
    private fun showBubble(message: String) {
        val existing = bubbleView
        if (existing != null) {
            existing.text = message
            return
        }
        val bubble = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.rgb(53, 60, 91))
            setPadding(16, 12, 16, 12)
            maxLines = 5
            background = GradientDrawable().apply {
                setColor(Color.rgb(255, 250, 255))
                cornerRadius = 22f
                setStroke(2, Color.rgb(213, 196, 239))
            }
            elevation = 14f
        }
        var swipeStartX = 0f
        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { swipeStartX = event.rawX; true }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX - swipeStartX) > 70f) {
                        hideBubble()
                    } else {
                        hideBubble()
                        startActivity(Intent(this@FloatingCompanionService, ChatActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    true
                }
                else -> true
            }
        }
        val avatarY = avatarParams?.y ?: 220
        val bubbleWidth = (resources.displayMetrics.widthPixels * 0.68f).toInt().coerceAtLeast(210)
        val screenWidth = resources.displayMetrics.widthPixels
        val avatarX = avatarParams?.x ?: 12
        val lp = WindowManager.LayoutParams(
            bubbleWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = avatarX.coerceIn(8, (screenWidth - bubbleWidth - 8).coerceAtLeast(8))
            y = if (avatarY > 170) (avatarY - 112).coerceAtLeast(24) else avatarY + 174
        }
        runCatching { wm.addView(bubble, lp); bubbleView = bubble }
    }

    private fun hideBubble() {
        bubbleDismiss?.let { android.os.Handler(mainLooper).removeCallbacks(it) }
        bubbleDismiss = null
        bubbleView?.let { runCatching { wm.removeView(it) } }
        bubbleView = null
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
                avatar?.setMood("happy")
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
                override fun onStart(utteranceId: String?) { mainThread { avatar?.startTalking() } }
                override fun onDone(utteranceId: String?) {
                    mainThread {
                        avatar?.stopTalking()
                        avatar?.setMood("happy")
                        android.os.Handler(mainLooper).postDelayed({ avatar?.setMood("neutral") }, 900)
                    }
                }
                override fun onError(utteranceId: String?) { mainThread { avatar?.stopTalking(); avatar?.setMood("neutral") } }
            })
        }
    }
    override fun onDestroy() {
        hideChat(); hideBubble(); avatar?.let { runCatching { wm.removeView(it) } }; avatar=null
        worker.shutdownNow(); if(::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
