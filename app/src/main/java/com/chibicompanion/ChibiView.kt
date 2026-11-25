package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import kotlin.math.min
import kotlin.math.sin

/**
 * Image-backed Miko renderer.
 *
 * The character is rendered from the bundled 4x3 transparent expression atlas.
 * This intentionally replaces the old geometric placeholder drawing. All
 * screens share this view so the same character art and expression mapping
 * are used in the home card, chat and floating companion.
 */
open class ChibiView(context: Context) : View(context) {
    @Volatile private var mood = "excited"
    @Volatile private var speaking = false
    @Volatile private var moodChangedAt = System.currentTimeMillis()
    private val startedAt = System.currentTimeMillis()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private var atlas: Bitmap? = null

    init {
        isClickable = true
        isFocusable = true
        loadAtlas()
    }

    private fun loadAtlas() {
        if (atlas != null) return
        val id = resources.getIdentifier("miko_atlas", "drawable", context.packageName)
        if (id == 0) return
        atlas = runCatching {
            BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            })
        }.getOrNull()
    }

    open fun setMood(value: String) {
        val next = when (value.lowercase()) {
            "embarrassed" -> "shy"
            "love", "loving" -> "affectionate"
            "talk" -> "talking"
            "pout", "pouting" -> "pouty"
            else -> value.lowercase()
        }
        if (next != mood) {
            mood = next
            moodChangedAt = System.currentTimeMillis()
            invalidate()
        }
    }

    open fun setSpeaking(value: Boolean) {
        speaking = value
        invalidate()
    }

    private fun expressionIndex(value: String): Int = when (value) {
        "neutral", "calm" -> 0
        "happy", "joy" -> 1
        "excited", "enthusiastic" -> 2
        "surprised", "confused", "curious" -> 3
        "shy", "embarrassed", "blushing" -> 4
        "teasing", "playful" -> 5
        "angry", "pouty", "annoyed" -> 6
        "sad", "crying", "disappointed" -> 7
        "affectionate", "love", "loving" -> 8
        "determined", "confident" -> 9
        "sleepy", "tired" -> 10
        "talking", "speaking" -> 11
        else -> 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        loadAtlas()
        val bitmap = atlas
        if (bitmap == null || bitmap.width < 4 || bitmap.height < 3 || width <= 0 || height <= 0) return

        val index = if (speaking) 11 else expressionIndex(mood)
        val column = index % 4
        val row = index / 4
        val source = Rect(
            (column * bitmap.width / 4f).toInt(),
            (row * bitmap.height / 3f).toInt(),
            ((column + 1) * bitmap.width / 4f).toInt(),
            ((row + 1) * bitmap.height / 3f).toInt()
        )

        val now = System.currentTimeMillis()
        val elapsed = (now - startedAt) / 1000f
        val transition = ((now - moodChangedAt) / 260f).coerceIn(0f, 1f)
        val pop = 1f + sin(transition * Math.PI).toFloat() * .025f
        val breathe = 1f + sin(elapsed * 1.8f) * .012f
        val bob = sin(elapsed * 2.1f) * height * .009f
        val sway = sin(elapsed * .82f) * 1.1f

        val tileWidth = source.width().toFloat()
        val tileHeight = source.height().toFloat()
        val fit = min(width / tileWidth, height / tileHeight)
        val drawWidth = tileWidth * fit
        val drawHeight = tileHeight * fit
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        canvas.save()
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.rotate(sway)
        canvas.scale(breathe * pop, breathe * pop)
        canvas.translate(-width / 2f, -height / 2f)
        canvas.drawBitmap(bitmap, source,
            RectF(left, top, left + drawWidth, top + drawHeight), paint)
        canvas.restore()

        // Keep the companion gently alive without a heavyweight animation engine.
        postInvalidateDelayed(50L)
    }
}
