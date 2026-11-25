package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * High-resolution 4x3 long-hair Miko sprite renderer.
 * The 12 poses are complete transparent sprites; each cell is 384x384.
 * Lightweight transforms keep the renderer suitable for mid-range Android.
 */
class MikoSpriteView(context: Context) : ChibiView(context) {
    private var spriteMood = "excited"
    private var spriteSpeaking = false
    private var interaction = ""
    private var interactionUntil = 0L
    private var dragTilt = 0f
    private var dragStretch = 1f
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

    /** Trigger a brief, readable touch reaction. */
    fun setInteraction(state: String) {
        interaction = state
        interactionUntil = when (state) {
            "picked_up" -> System.currentTimeMillis() + 850L
            "tap" -> System.currentTimeMillis() + 620L
            "double_tap" -> System.currentTimeMillis() + 1100L
            "long_tap" -> System.currentTimeMillis() + 950L
            "released" -> System.currentTimeMillis() + 450L
            else -> 0L
        }
        invalidate()
    }

    /** Lean into the drag direction and let the pose settle on release. */
    fun setDragMotion(dx: Float, dy: Float) {
        dragTilt = (dx * .11f).coerceIn(-13f, 13f)
        dragStretch = (1f + abs(dy) / 1800f).coerceIn(1f, 1.045f)
        interaction = "dragging"
        interactionUntil = 0L
        invalidate()
    }

    fun clearDragMotion() {
        dragTilt = 0f
        dragStretch = 1f
        setInteraction("released")
    }

    private fun loadAtlas(): Bitmap? {
        if (!attemptedLoad) {
            attemptedLoad = true
            val id = resources.getIdentifier("miko_atlas", "drawable", context.packageName)
            if (id != 0) atlas = runCatching { BitmapFactory.decodeResource(resources, id) }.getOrNull()
        }
        return atlas
    }

    private fun expressionIndex(now: Long): Int {
        if (interaction.isNotEmpty() && interactionUntil > 0L && now > interactionUntil) interaction = ""
        when (interaction) {
            "picked_up" -> return 7
            "dragging" -> return 11
            "tap" -> return 1
            "double_tap" -> return 9
            "long_tap" -> return 3
            "released" -> return 2
        }
        if (spriteSpeaking || spriteMood == "talking") return 11
        return when (spriteMood) {
            "neutral", "calm" -> 0
            "happy" -> 1
            "excited" -> 2
            "shy", "embarrassed" -> 3
            "angry", "pouty", "pouting" -> 4
            "sad", "crying" -> 5
            "sleepy" -> 6
            "surprised" -> 7
            "teasing", "confused" -> 8
            "affectionate", "love", "loving" -> 9
            "determined" -> 10
            else -> 0
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val image = loadAtlas()
        if (image == null || image.isRecycled || width <= 0 || height <= 0) return

        val now = System.currentTimeMillis()
        val index = expressionIndex(now)
        val columns = 4
        val rows = 3
        val cellW = image.width / columns
        val cellH = image.height / rows
        val col = index % columns
        val row = index / columns
        val src = Rect(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH)

        // Keep a little breathing room around the full hair silhouette.
        val fit = min((width * .96f) / src.width(), (height * .96f) / src.height())
        val drawW = src.width() * fit
        val drawH = src.height() * fit
        val t = (now - bornAt) / 1000f
        val isDragging = interaction == "dragging"
        val floatAmount = if (isDragging) height * .004f else height * .018f
        val bob = sin(t * 2.15f) * floatAmount
        val breathe = 1f + sin(t * 1.9f) * (if (isDragging) .004f else .014f)
        val idleSway = sin(t * .82f) * (if (isDragging) .35f else 2.2f)
        val pickupPop = if (interaction == "picked_up") 1.035f + sin(t * 18f) * .018f else 1f
        val releaseSettle = if (interaction == "released") 1f + sin(t * 12f) * .012f else 1f

        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.rotate(idleSway + dragTilt)
        canvas.scale(breathe * pickupPop * releaseSettle, breathe * dragStretch * pickupPop * releaseSettle)
        val dst = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
        canvas.drawBitmap(image, src, dst, spritePaint)
        canvas.restore()

        if (isShown) postInvalidateDelayed(40L)
    }
}
