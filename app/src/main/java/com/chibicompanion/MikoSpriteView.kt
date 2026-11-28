package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File
import android.os.Handler
import android.os.Looper

/**
 * Displays the user's three aligned Miko mouth sprites. If they have not been
 * imported yet, the lightweight Canvas avatar remains as a safe fallback.
 */
class MikoSpriteView(context: Context) : FrameLayout(context) {
    private val fallback = ChibiView(context)
    private val sprite = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = false
        visibility = View.GONE
        contentDescription = "Miko"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<Bitmap> = emptyList()
    private var frameIndex = 0
    private var talking = false
    private val talkFrames = intArrayOf(0, 1, 2, 1, 0, 1)
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
    }

    private fun loadUserSprites() {
        val directory = File(context.filesDir, "miko_assets")
        val names = listOf("miko_mouth_closed.webp", "miko_mouth_half.webp", "miko_mouth_open.webp")
        val decoded = names.map { name -> File(directory, name).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.absolutePath) } }
        if (decoded.all { it != null }) {
            frames = decoded.filterNotNull()
            fallback.visibility = View.GONE
            sprite.visibility = View.VISIBLE
            sprite.setImageBitmap(frames[0])
        }
    }

    fun setMood(value: String) {
        fallback.setMood(value)
        if (value == "talking") startTalking() else if (value == "neutral" || value == "happy" || value == "shy" || value == "angry") stopTalking()
    }

    fun startTalking() {
        if (talking) return
        if (frames.size != 3) {
            fallback.setMood("talking")
            return
        }
        talking = true
        frameIndex = 0
        handler.removeCallbacks(talkTick)
        talkTick.run()
    }

    fun stopTalking() {
        talking = false
        handler.removeCallbacks(talkTick)
        if (frames.size == 3) sprite.setImageBitmap(frames[0])
        fallback.setMood("neutral")
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(talkTick)
        talking = false
        super.onDetachedFromWindow()
    }
}
