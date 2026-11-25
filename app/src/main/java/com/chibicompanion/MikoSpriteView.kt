package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.sin

/**
 * Lightweight image-backed Miko renderer.
 *
 * The bundled core atlas is a 2x2 sheet (four complete chibi poses), not a
 * 4x3 sheet. Keep source-cell geometry in sync with the actual asset so we
 * never crop a face or draw outside this view's bounds.
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

    // Four complete poses in the committed 2x2 atlas. Several moods share
    // the closest matching pose until a larger expression sheet is authored.
    private fun expressionIndex(): Int = when {
        spriteSpeaking -> 2
        spriteMood in setOf("neutral", "calm") -> 0
        spriteMood in setOf("happy", "shy", "affectionate", "teasing", "love") -> 1
        spriteMood in setOf("excited", "talking", "surprised", "determined") -> 2
        spriteMood in setOf("angry", "sad", "crying", "sleepy", "confused", "pouty") -> 3
        else -> 0
    }

    override fun onDraw(canvas: Canvas) {
        val image = loadAtlas()
        if (image == null || image.isRecycled || width <= 0 || height <= 0) {
            super.onDraw(canvas)
            return
        }

        val index = expressionIndex()
        val columns = 2
        val rows = 2
        val col = index % columns
        val row = index / columns
        val src = Rect(
            col * image.width / columns,
            row * image.height / rows,
            (col + 1) * image.width / columns,
            (row + 1) * image.height / rows
        )

        val fit = min((width * .94f) / src.width(), (height * .94f) / src.height())
        val drawW = src.width() * fit
        val drawH = src.height() * fit
        val t = (System.currentTimeMillis() - bornAt) / 1000f
        val bob = sin(t * 2.1f) * (height * .009f)
        val breathe = 1f + sin(t * 2f) * .012f
        val tilt = sin(t * .75f) * 1.1f

        canvas.save()
        // Explicit clipping protects adjacent controls even if a parent or
        // WindowManager gives this view unusual dimensions.
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.rotate(tilt)
        canvas.scale(breathe, breathe)
        val dst = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
        canvas.drawBitmap(image, src, dst, spritePaint)
        canvas.restore()

        if (isShown) postInvalidateDelayed(50)
    }
}
