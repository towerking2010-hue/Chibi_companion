package com.chibicompanion

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
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
    private var replyBubble: LinearLayout? = null
    private var replyParams: WindowManager.LayoutParams? = null
    private var pendingAttachment: Uri? = null
    private var pendingAttachmentName: String = ""
    private var pendingAttachmentMime: String = ""
    private var avatarParams: WindowManager.LayoutParams? = null
    private var lastTap = 0L
    private var downX = 0f; private var downY = 0f; private var originX = 0; private var originY = 0; private var downAt = 0L; private var dragging = false
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var tts: TextToSpeech

    private val POS_PREFS = "miko_floating_position"
    private val POS_X = "pos_x"
    private val POS_Y = "pos_y"

    private fun loadSavedPosition(): Pair<Int, Int> {
        val prefs = getSharedPreferences(POS_PREFS, MODE_PRIVATE)
        val x = prefs.getInt(POS_X, -1)
        val y = prefs.getInt(POS_Y, -1)
        return Pair(x, y)
    }

    private fun savePosition(x: Int, y: Int) {
        getSharedPreferences(POS_PREFS, MODE_PRIVATE).edit()
            .putInt(POS_X, x)
            .putInt(POS_Y, y)
            .apply()
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
        startForeground(41, notification())
        getSharedPreferences("miko_companion_state", MODE_PRIVATE).edit().putBoolean("floating", true).apply()
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
        if (intent?.action == "RELOAD_MIKO_ASSETS") {
            avatar?.let { runCatching { wm.removeView(it) } }
            avatar = null
            showAvatar()
            return START_STICKY
        }
        if (intent?.action == "OPEN_CHAT") {
            if (chatPanel == null) showChat()
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
        // WindowManager.LayoutParams dimensions are pixels, so convert dp explicitly.
        // The old 136x164 raw-pixel window rendered Miko as a tiny sticker on high-density phones.
        val overlayWidth = CompanionUi.dp(this, 190)
        val overlayHeight = CompanionUi.dp(this, 270)

        // Load saved position
        val (savedX, savedY) = loadSavedPosition()
        val metrics = resources.displayMetrics
        val defaultX = 8
        val defaultY = CompanionUi.dp(this@FloatingCompanionService, 150)
        val maxX = (metrics.widthPixels - overlayWidth).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - overlayHeight - CompanionUi.dp(this, 28)).coerceAtLeast(0)

        val startX = if (savedX >= 0) savedX.coerceIn(0, maxX) else defaultX
        val startY = if (savedY >= 0) savedY.coerceIn(CompanionUi.dp(this, 24), maxY) else defaultY

        val lp = WindowManager.LayoutParams(overlayWidth, overlayHeight, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = startX; y = startY }

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
                        // Keep Miko wherever the user drops her; only clamp to visible screen bounds.
                        val maxX = (metrics.widthPixels - lp.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - lp.height - CompanionUi.dp(this, 28)).coerceAtLeast(0)
                        lp.x = lp.x.coerceIn(0, maxX)
                        lp.y = lp.y.coerceIn(CompanionUi.dp(this, 24), maxY)
                        runCatching { wm.updateViewLayout(v, lp) }
                        // Save position
                        savePosition(lp.x, lp.y)
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
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        runCatching { wm.addView(v,lp); avatar=v; avatarParams=lp }.onFailure { stopSelf() }
    }

    private fun toggleChat() { if (chatPanel == null) showChat() else hideChat() }

    /** Compact Google-Assistant-style composer: it never opens a transcript-sized overlay. */
    private fun showChat() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(CompanionUi.dp(this@FloatingCompanionService, 10), CompanionUi.dp(this@FloatingCompanionService, 8),
                CompanionUi.dp(this@FloatingCompanionService, 10), CompanionUi.dp(this@FloatingCompanionService, 8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(28, 31, 58)); cornerRadius = CompanionUi.dp(this@FloatingCompanionService, 24).toFloat()
                setStroke(CompanionUi.dp(this@FloatingCompanionService, 1), Color.rgb(133, 128, 205))
            }
            elevation = CompanionUi.dp(this@FloatingCompanionService, 12).toFloat()
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = "Miko  ·  Quick chat"; textSize = 13f; setTextColor(Color.rgb(220, 218, 255))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply {
            text = "Open chat ↗"; textSize = 12f; setTextColor(Color.rgb(170, 190, 255))
            setPadding(8, 4, 8, 4)
            setOnClickListener { hideChat(); startActivity(Intent(this@FloatingCompanionService, ChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        })
        top.addView(TextView(this).apply {
            text = "×"; textSize = 22f; setTextColor(Color.WHITE); setPadding(12, 0, 4, 0)
            setOnClickListener { hideChat() }
        })
        panel.addView(top)

        val attachment = TextView(this).apply {
            tag = "miko_attachment"; textSize = 11f; setTextColor(Color.rgb(190, 215, 255))
            visibility = if (pendingAttachment != null) View.VISIBLE else View.GONE
            text = if (pendingAttachment != null) "📎 $pendingAttachmentName" else ""
            setPadding(8, 4, 8, 4)
            setOnClickListener { pendingAttachment = null; pendingAttachmentName = ""; pendingAttachmentMime = ""; visibility = View.GONE }
        }
        panel.addView(attachment)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val attach = TextView(this).apply {
            text = "＋"; textSize = 25f; setTextColor(Color.rgb(205, 205, 255)); gravity = Gravity.CENTER
            setPadding(6, 0, 10, 0)
            setOnClickListener { startActivity(Intent(this@FloatingCompanionService, QuickAttachmentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        row.addView(attach, LinearLayout.LayoutParams(CompanionUi.dp(this, 38), CompanionUi.dp(this, 48)))
        val input = EditText(this).apply {
            hint = "Ask Miko anything…"; textSize = 14f; maxLines = 3
            setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            background = GradientDrawable().apply { setColor(Color.rgb(48, 51, 82)); cornerRadius = CompanionUi.dp(this@FloatingCompanionService, 18).toFloat() }
            setPadding(CompanionUi.dp(this@FloatingCompanionService, 12), CompanionUi.dp(this@FloatingCompanionService, 9),
                CompanionUi.dp(this@FloatingCompanionService, 12), CompanionUi.dp(this@FloatingCompanionService, 9))
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        val send = TextView(this).apply {
            text = "➤"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            setPadding(12, 0, 6, 0)
            setOnClickListener {
                val typed = input.text.toString().trim()
                val uri = pendingAttachment
                if (typed.isEmpty() && uri == null) return@setOnClickListener
                input.text.clear()
                val name = pendingAttachmentName
                val mime = pendingAttachmentMime
                pendingAttachment = null; pendingAttachmentName = ""; pendingAttachmentMime = ""
                attachment.visibility = View.GONE
                hideChat()
                submitQuickMessage(typed, uri, name, mime)
            }
        }
        row.addView(send, LinearLayout.LayoutParams(CompanionUi.dp(this, 42), CompanionUi.dp(this, 48)))
        panel.addView(row)

        val width = (resources.displayMetrics.widthPixels - CompanionUi.dp(this, 20)).coerceAtLeast(CompanionUi.dp(this, 260))
        val lp = WindowManager.LayoutParams(width, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = CompanionUi.dp(this@FloatingCompanionService, 18)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        runCatching {
            wm.addView(panel, lp); chatPanel = panel; chatParams = lp
            input.requestFocus()
            input.post { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
        }
    }

    private fun submitQuickMessage(message: String, uri: Uri?, name: String, mime: String) {
        val userLine = buildString {
            if (message.isNotBlank()) append(message)
            if (uri != null) { if (isNotEmpty()) append("\\n"); append("[Attachment: ").append(name).append("]") }
        }
        ChatHistory.add(this, "You", userLine)
        showReplyBubble("Miko is thinking…")
        worker.execute {
            val answer = try {
                if (uri == null) {
                    CompanionMemory.handleExplicitRequest(this@FloatingCompanionService, message)
                        ?: PhoneActions.tryHandle(this@FloatingCompanionService, message)
                        ?: AiClient.reply(this@FloatingCompanionService, message)
                } else when {
                    mime.startsWith("image/") -> {
                        val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                            ?: throw IllegalStateException("Couldn't read that image")
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
                        bitmap.recycle()
                        AiClient.replyWithImage(this@FloatingCompanionService,
                            message.ifBlank { "Please look at this image and help me with it." }, out.toByteArray())
                    }
                    mime.startsWith("text/") || name.endsWith(".txt", true) || name.endsWith(".md", true) || name.endsWith(".csv", true) -> {
                        val text = contentResolver.openInputStream(uri).bufferedReader().use { it.readText().take(24000) }
                        AiClient.reply(this@FloatingCompanionService,
                            (message + "\\n\\nAttached file ($name):\\n" + text).trim())
                    }
                    else -> "I attached $name, but this build can only read images and text files. PDF and video understanding still needs to be added."
                }
            } catch (e: Exception) {
                OfflineCompanion.reply(message.ifBlank { "I couldn't read that attachment." })
            }
            ChatHistory.add(this@FloatingCompanionService, "Miko", answer)
            mainThread {
                showReplyBubble(answer)
                if (::tts.isInitialized) {
                    CompanionVoice.configure(this@FloatingCompanionService, tts)
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "miko-quick-reply")
                }
            }
        }
    }

    /** A separate coloured reply card, positioned clear of Miko where screen space allows. */
    private fun showReplyBubble(message: String) {
        replyBubble?.let { runCatching { wm.removeView(it) } }
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(CompanionUi.dp(this@FloatingCompanionService, 14), CompanionUi.dp(this@FloatingCompanionService, 10),
                CompanionUi.dp(this@FloatingCompanionService, 10), CompanionUi.dp(this@FloatingCompanionService, 10))
            background = GradientDrawable().apply { setColor(Color.rgb(231, 229, 255)); cornerRadius = CompanionUi.dp(this@FloatingCompanionService, 18).toFloat() }
            elevation = CompanionUi.dp(this@FloatingCompanionService, 10).toFloat()
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "MIKO  ·  REPLY"; textSize = 10f; setTextColor(Color.rgb(86, 78, 150)) }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply { text = "×"; textSize = 20f; setTextColor(Color.rgb(86, 78, 150)); setOnClickListener { hideReplyBubble() } })
        bubble.addView(header)
        bubble.addView(TextView(this).apply {
            text = message; textSize = 14f; setTextColor(Color.rgb(39, 38, 70)); maxLines = 7
            setOnClickListener { startActivity(Intent(this@FloatingCompanionService, ChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        })
        val screenW = resources.displayMetrics.widthPixels
        val avatarY = avatarParams?.y ?: CompanionUi.dp(this, 150)
        val bubbleW = (screenW - CompanionUi.dp(this, 24)).coerceAtLeast(CompanionUi.dp(this, 240))
        val lp = WindowManager.LayoutParams(bubbleW, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = if (avatarY > CompanionUi.dp(this@FloatingCompanionService, 190))
                (avatarY - CompanionUi.dp(this@FloatingCompanionService, 120)).coerceAtLeast(CompanionUi.dp(this@FloatingCompanionService, 28))
            else avatarY + CompanionUi.dp(this@FloatingCompanionService, 280)
        }
        runCatching { wm.addView(bubble, lp); replyBubble = bubble; replyParams = lp }
    }

    private fun hideReplyBubble() {
        replyBubble?.let { runCatching { wm.removeView(it) } }
        replyBubble = null; replyParams = null
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
                override fun onStart(utteranceId: String?) { mainThread { avatar?.startTalking() } }
                override fun onDone(utteranceId: String?) { mainThread { avatar?.stopTalking() } }
                override fun onError(utteranceId: String?) { mainThread { avatar?.stopTalking() } }
            })
        }
    }
    override fun onDestroy() {
        getSharedPreferences("miko_companion_state", MODE_PRIVATE).edit().putBoolean("floating", false).apply()
        hideChat(); hideReplyBubble(); avatar?.stopTalking(); avatar?.let { runCatching { wm.removeView(it) } }; avatar=null
        worker.shutdownNow(); if(::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null\n\n    companion object {\n        const val ACTION_ATTACHMENT_READY = "com.chibicompanion.QUICK_ATTACHMENT_READY"\n        const val EXTRA_ATTACHMENT_NAME = "attachment_name"\n        const val EXTRA_ATTACHMENT_MIME = "attachment_mime"\n    }
}
