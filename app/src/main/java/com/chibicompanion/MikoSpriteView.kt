package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Image-backed 2D Miko renderer. The atlas contains 12 authored expressions
 * in a 4x3 grid. Lightweight transforms provide breathing/bobbing while the
 * selected expression is held; this is a sprite animation system, not Live2D.
 */
class MikoSpriteView(context: Context) : ChibiView(context) {
    private var spriteMood = "excited"
    private var spriteSpeaking = false
    private val bornAt = System.currentTimeMillis()
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private var atlas: Bitmap? = null
    private var attemptedLoad = false

    override fun setMood(value: String) {
        spriteMood = when (value.lowercase()) {
            "embarrassed" -> "shy"
            "love", "loving", "love you" -> "affectionate"
            "talk" -> "talking"
            "pout", "pouting" -> "pouty"
            else -> value.lowercase()
        }
        super.setMood(value)
        invalidate()
    }

    override fun setSpeaking(value: Boolean) {
        spriteSpeaking = value
        super.setSpeaking(value)
        invalidate()
    }

    private fun loadAtlas(): Bitmap? {
        if (!attemptedLoad) {
            attemptedLoad = true
            val id = resources.getIdentifier("miko_atlas", "drawable", context.packageName)
            if (id != 0) atlas = runCatching { BitmapFactory.decodeResource(resources, id) }.getOrNull()
        }
        return atlas
    }

    private fun expressionIndex(): Int = when {
        spriteSpeaking -> 11
        spriteMood == "neutral" -> 0
        spriteMood == "happy" -> 1
        spriteMood == "excited" -> 2
        spriteMood == "angry" -> 3
        spriteMood == "sad" || spriteMood == "crying" -> 4
        spriteMood == "shy" -> 5
        spriteMood == "sleepy" -> 6
        spriteMood == "surprised" || spriteMood == "confused" -> 7
        spriteMood == "teasing" -> 8
        spriteMood == "affectionate" -> 9
        spriteMood == "determined" -> 10
        spriteMood == "talking" -> 11
        spriteMood == "pouty" -> 3
        else -> 0
    }

    override fun onDraw(canvas: Canvas) {
        val image = loadAtlas()
        if (image == null || image.isRecycled) {
            super.onDraw(canvas)
            return
        }
        super<View>.onDraw(canvas)
        val index = expressionIndex()
        val col = index % 4
        val row = index / 4
        val src = Rect(
            col * image.width / 4,
            row * image.height / 3,
            (col + 1) * image.width / 4,
            (row + 1) * image.height / 3
        )
        val cellW = src.width().toFloat()
        val cellH = src.height().toFloat()
        val fit = min((width * .94f) / cellW, (height * .94f) / cellH)
        val drawW = cellW * fit
        val drawH = cellH * fit
        val t = (System.currentTimeMillis() - bornAt) / 1000f
        val bob = sin(t * 2.1f) * (height * .009f)
        val breathe = 1f + sin(t * 2f) * .012f
        val tilt = sin(t * .75f) * 1.1f

        canvas.save()
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.rotate(tilt)
        canvas.scale(breathe, breathe)
        val dst = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
        canvas.drawBitmap(image, src, dst, spritePaint)
        canvas.restore()
        postInvalidateDelayed(50)
    }
}
