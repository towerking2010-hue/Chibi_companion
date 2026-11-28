package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File

/**
 * Miko uses exactly the three supplied mouth images.
 * Closed is the resting/listening state; the three frames cycle only during TTS.
 */
class MikoSpriteView(context: Context) : FrameLayout(context) {
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
        contentDescription = "Miko"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<Bitmap> = emptyList()
    private var frameIndex = 0
    private var talking = false
    private val sequence = intArrayOf(0, 1, 2, 1)

    private val mouthTick = object : Runnable {
        override fun run() {
            if (!talking || frames.size != 3) return
            image.setImageBitmap(frames[sequence[frameIndex % sequence.size]])
            frameIndex++
            handler.postDelayed(this, 115L)
        }
    }

    init {
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        loadSuppliedImages()
    }

    private fun loadSuppliedImages() {
        val directory = File(context.filesDir, "miko_assets")
        val names = listOf("miko_mouth_closed.webp", "miko_mouth_half.webp", "miko_mouth_open.webp")
        val loaded = names.map { name ->
            val file = File(directory, name)
            if (!file.isFile || file.length() == 0L) null else decodeForDevice(file)
        }
        if (loaded.all { it != null } && loaded.filterNotNull().map { it.width to it.height }.distinct().size == 1) {
            frames = loaded.filterNotNull()
            image.setImageBitmap(frames[0])
        } else {
            loaded.filterNotNull().forEach { it.recycle() }
            // Do not substitute a different avatar. The home screen importer explains how to add the supplied trio.
            image.setImageDrawable(null)
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
            inPreferredConfig = Bitmap.Config.RGB_565
        })
    }

    /** Kept for existing call sites; no extra expressions are selected. */
    fun setMood(value: String) {
        if (value.equals("neutral", true) || value.equals("normal", true) || value.equals("idle", true)) {
            stopTalking()
        }
    }

    fun startTalking() {
        if (frames.size != 3 || talking) return
        talking = true
        frameIndex = 0
        handler.removeCallbacks(mouthTick)
        mouthTick.run()
    }

    fun stopTalking() {
        talking = false
        handler.removeCallbacks(mouthTick)
        if (frames.size == 3) image.setImageBitmap(frames[0])
    }

    override fun onDetachedFromWindow() {
        stopTalking()
        super.onDetachedFromWindow()
    }
}
