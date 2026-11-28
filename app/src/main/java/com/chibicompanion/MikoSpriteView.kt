package com.chibicompanion

import android.content.Context
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.io.File

/** Miko's transparent animated WebP expression player, with the original three-frame mouth fallback. */
class MikoSpriteView(context: Context) : FrameLayout(context) {
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "Miko"
    }
    private val hint = TextView(context).apply {
        text = "Import Miko's animation pack to bring her to life ♡"
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(CompanionUi.muted)
    }
    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<Bitmap> = emptyList()
    private var frameIndex = 0
    private var talking = false
    private var currentMood = "idle"
    private var animated: AnimatedImageDrawable? = null
    private var breathing: ValueAnimator? = null
    private val sequence = intArrayOf(0, 1, 2, 1)
    private val mouthTick = object : Runnable {
        override fun run() {
            if (!talking || frames.size != 3 || animated != null) return
            image.setImageBitmap(frames[sequence[frameIndex % sequence.size]])
            frameIndex++
            handler.postDelayed(this, 115L)
        }
    }

    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        addView(hint, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        loadSuppliedImages()
        loadAnimation("idle")
    }

    private fun loadSuppliedImages() {
        val directory = File(context.filesDir, "miko_assets").apply { mkdirs() }
        val names = listOf("miko_mouth_closed.webp", "miko_mouth_half.webp", "miko_mouth_open.webp")
        names.forEach { name ->
            val target = File(directory, name)
            if (!target.isFile || target.length() == 0L) {
                runCatching {
                    context.assets.open("miko_assets/$name").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        val loaded = names.map { name ->
            val file = File(directory, name)
            if (!file.isFile || file.length() == 0L) null else decodeForDevice(file)
        }
        if (loaded.all { it != null } && loaded.filterNotNull().map { it.width to it.height }.distinct().size == 1) {
            frames = loaded.filterNotNull()
            if (animated == null) image.setImageBitmap(frames[0])
            hint.visibility = View.GONE
        } else {
            loaded.filterNotNull().forEach { it.recycle() }
        }
    }

    private fun decodeForDevice(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 640 || bounds.outHeight / sample > 900) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }

    private fun loadAnimation(mood: String): Boolean {
        val file = File(context.filesDir, "miko_animations/$mood.webp")
        if (!file.isFile || file.length() == 0L || Build.VERSION.SDK_INT < 28) return false
        return runCatching {
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
            val animatedDrawable = drawable as? AnimatedImageDrawable ?: return@runCatching false
            animated?.stop()
            breathing?.cancel(); breathing = null
            scaleX = 1f; scaleY = 1f; translationY = 0f
            animated = animatedDrawable.apply {
                repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            }
            image.setImageDrawable(animatedDrawable)
            hint.visibility = View.GONE
            if (isAttachedToWindow) animatedDrawable.start()
            true
        }.getOrDefault(false)
    }

    /** Maps emotion/action labels to the supplied animation filenames. */
    fun setMood(value: String) {
        val key = value.lowercase().trim()
        val mood = when {
            key in setOf("idle", "neutral", "normal", "default") -> "idle"
            key in setOf("happy", "excited", "cheerful") -> "happy"
            key in setOf("laugh", "laughing", "funny") -> "laughing"
            key in setOf("annoyed", "angry", "grumpy") -> "annoyed"
            key in setOf("surprised", "astonished", "shock", "wow") -> "astonished"
            key in setOf("blush", "shy", "embarrassed") -> "blush"
            key in setOf("sad", "sorry", "upset") -> "sad"
            key in setOf("sleepy", "tired", "sleep") -> "sleepy"
            key in setOf("thinking", "think", "curious") -> "thinking"
            key in setOf("wave", "hello", "greeting") -> "wave"
            key in setOf("dance", "little dance") -> "little dance"
            else -> "idle"
        }
        if (mood == currentMood && animated != null) return
        currentMood = mood
        if (!loadAnimation(mood)) {
            animated?.stop()
            animated = null
            if (frames.isNotEmpty()) image.setImageBitmap(frames[0])
            startBreathing()
        }
    }

    private fun startBreathing() {
        if (animated != null || frames.isEmpty() || breathing?.isRunning == true) return
        breathing = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val amount = animator.animatedValue as Float
                scaleY = 1f - (amount * .012f)
                translationY = -CompanionUi.dp(context, 2) * amount
            }
            start()
        }
    }

    fun startTalking() {
        if (animated != null) return // The selected full-body animation keeps playing during TTS.
        if (frames.size != 3 || talking) return
        talking = true
        frameIndex = 0
        handler.removeCallbacks(mouthTick)
        mouthTick.run()
    }

    fun stopTalking() {
        talking = false
        handler.removeCallbacks(mouthTick)
        if (animated == null && frames.size == 3) image.setImageBitmap(frames[0])
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animated?.start()
        startBreathing()
    }

    override fun onDetachedFromWindow() {
        stopTalking()
        breathing?.cancel(); breathing = null
        scaleY = 1f; translationY = 0f
        animated?.stop()
        super.onDetachedFromWindow()
    }
}
