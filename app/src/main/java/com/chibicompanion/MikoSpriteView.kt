package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File

/**
 * Miko's lightweight visual host.
 * Priority: imported animated WebP mood clip -> three-mouth talking sprites -> Canvas fallback.
 */
class MikoSpriteView(context: Context) : FrameLayout(context) {
    private val fallback = ChibiView(context)
    private val sprite = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
        visibility = View.GONE
        contentDescription = "Miko"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<Bitmap> = emptyList()
    private var frameIndex = 0
    private var talking = false
    private var activeAnimation: Drawable? = null
    private var currentMood = "idle"
    private val talkFrames = intArrayOf(0, 1, 2, 1)
    private val talkTick = object : Runnable {
        override fun run() {
            if (!talking || frames.size != 3) return
            sprite.setImageBitmap(frames[talkFrames[frameIndex % talkFrames.size]])
            frameIndex++
            handler.postDelayed(this, 115L)
        }
    }

    init {
        addView(fallback, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(sprite, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        loadUserSprites()
        showMood("idle")
    }

    private fun loadUserSprites() {
        val directory = File(context.filesDir, "miko_assets")
        val names = listOf("miko_mouth_closed.webp", "miko_mouth_half.webp", "miko_mouth_open.webp")
        val decoded = names.map { name ->
            File(directory, name).takeIf { it.isFile && it.length() > 0L }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }
        }
        if (decoded.all { it != null }) {
            frames = decoded.filterNotNull()
            if (frames.map { it.width to it.height }.distinct().size == 1) {
                fallback.visibility = View.GONE
                sprite.visibility = View.VISIBLE
                sprite.setImageBitmap(frames[0])
            } else {
                frames.forEach { it.recycle() }
                frames = emptyList()
            }
        }
    }

    fun setMood(value: String) {
        val mood = when (value.lowercase()) {
            "neutral", "normal" -> "idle"
            "angry" -> "tsundere"
            else -> value.lowercase()
        }
        currentMood = if (mood in MikoAnimationStore.moods) mood else "idle"
        showMood(currentMood)
    }

    private fun showMood(mood: String) {
        stopActiveAnimation()
        talking = false
        handler.removeCallbacks(talkTick)

        val clip = File(MikoAnimationStore.directory(context), "$mood.webp")
        if (clip.isFile && clip.length() > 0L && Build.VERSION.SDK_INT >= 28) {
            try {
                val source = ImageDecoder.createSource(clip)
                val decoded = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
                    if (info.size.width > 900 || info.size.height > 1200) {
                        val scale = minOf(900f / info.size.width, 1200f / info.size.height, 1f)
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
                activeAnimation = decoded
                fallback.visibility = View.GONE
                sprite.visibility = View.VISIBLE
                sprite.setImageDrawable(decoded)
                if (decoded is AnimatedImageDrawable) {
                    decoded.repeatCount = if (mood in setOf("idle", "thinking", "listening", "talking", "sleepy")) {
                        AnimatedImageDrawable.REPEAT_INFINITE
                    } else 0
                    decoded.start()
                }
                return
            } catch (_: Exception) {
                activeAnimation = null
            }
        }

        if (mood == "talking" && frames.size == 3) {
            fallback.visibility = View.GONE
            sprite.visibility = View.VISIBLE
            startMouthFlap()
        } else if (frames.size == 3) {
            fallback.visibility = View.GONE
            sprite.visibility = View.VISIBLE
            sprite.setImageBitmap(frames[0])
        } else {
            sprite.visibility = View.GONE
            fallback.visibility = View.VISIBLE
            fallback.setMood(mood)
        }
    }

    private fun startMouthFlap() {
        if (frames.size != 3) return
        talking = true
        frameIndex = 0
        talkTick.run()
    }

    fun startTalking() = setMood("talking")

    fun stopTalking() {
        if (currentMood == "talking") setMood("idle")
        else {
            talking = false
            handler.removeCallbacks(talkTick)
            if (frames.size == 3 && activeAnimation == null) sprite.setImageBitmap(frames[0])
        }
    }

    private fun stopActiveAnimation() {
        (activeAnimation as? AnimatedImageDrawable)?.stop()
        activeAnimation?.callback = null
        activeAnimation = null
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(talkTick)
        talking = false
        stopActiveAnimation()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showMood(currentMood)
    }
}
