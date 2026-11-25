package com.chibicompanion

import android.content.Context
import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.sin

/** Gesture-aware motion layer around the crisp vector Miko renderer. */
class MikoSpriteView(context: Context) : ChibiView(context) {
    private var interaction = ""
    private var interactionUntil = 0L
    private var dragTilt = 0f
    private var dragStretch = 1f
    private var releaseTilt = 0f
    private var releaseStretch = 1f
    private var releasedAt = 0L
    private var moodBeforeInteraction = "excited"

    fun setInteraction(state: String) {
        if (interaction.isEmpty() && state != "released") moodBeforeInteraction = currentMood
        interaction = state
        val now = System.currentTimeMillis()
        interactionUntil = when (state) {
            "picked_up" -> now + 850L
            "tap" -> now + 620L
            "double_tap" -> now + 1100L
            "long_tap" -> now + 950L
            "released" -> now + 480L
            else -> 0L
        }
        if (state == "released") {
            releaseTilt = dragTilt
            releaseStretch = dragStretch
            releasedAt = now
        }
        when (state) {
            "picked_up" -> setMood("surprised")
            "dragging" -> setMood("happy")
            "tap" -> setMood("happy")
            "double_tap" -> setMood("affectionate")
            "long_tap" -> setMood("shy")
            "released" -> setMood("excited")
        }
        invalidate()
    }

    fun setDragMotion(dx: Float, dy: Float) {
        dragTilt = (dx * .10f).coerceIn(-11f, 11f)
        dragStretch = (1f + abs(dy) / 2300f).coerceIn(1f, 1.035f)
        interaction = "dragging"
        interactionUntil = 0L
        setMood("happy")
        invalidate()
    }

    fun clearDragMotion() {
        setInteraction("released")
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        if (interaction.isNotEmpty() && interactionUntil > 0L && now > interactionUntil) {
            interaction = ""
            interactionUntil = 0L
            setMood(moodBeforeInteraction)
        }

        val t = now / 1000f
        val dragging = interaction == "dragging"
        val bob = sin(t * 2.0f) * height * if (dragging) .0015f else .006f
        val sway = sin(t * .8f) * if (dragging) .25f else 1.1f
        val breathe = 1f + sin(t * 1.65f) * if (dragging) .002f else .005f
        val pickup = if (interaction == "picked_up") 1.025f + sin(t * 14f) * .008f else 1f
        val tapBounce = when (interaction) {
            "tap" -> 1f + abs(sin(t * 9f)) * .018f
            "double_tap" -> 1f + abs(sin(t * 13f)) * .025f
            "long_tap" -> 1f + sin(t * 4f) * .008f
            else -> 1f
        }

        val releaseAge = if (releasedAt == 0L) 1f
            else ((now - releasedAt) / 420f).coerceIn(0f, 1f)
        val spring = (1f - releaseAge)
        val returningTilt = if (interaction == "released") releaseTilt * spring * spring else dragTilt
        val returningStretch = if (interaction == "released") 1f + (releaseStretch - 1f) * spring else dragStretch
        if (releaseAge >= 1f) {
            dragTilt = 0f
            dragStretch = 1f
            releaseTilt = 0f
            releaseStretch = 1f
            releasedAt = 0L
        }

        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.rotate(sway + returningTilt)
        canvas.scale(breathe * pickup * tapBounce, breathe * returningStretch * pickup * tapBounce)
        canvas.translate(-width / 2f, -height / 2f)
        super.onDraw(canvas)
        canvas.restore()
    }
}
