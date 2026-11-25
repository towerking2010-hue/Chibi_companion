package com.chibicompanion

import android.content.Context
import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.sin

/**
 * Image-atlas Miko renderer with lightweight pickup, drag and release motion.
 * Character artwork and expression poses are shared across all app surfaces.
 */
class MikoSpriteView(context: Context) : ChibiView(context) {
    private var interaction = ""
    private var interactionUntil = 0L
    private var dragTilt = 0f
    private var dragStretch = 1f
    private var moodBeforeInteraction = "excited"

    fun setInteraction(state: String) {
        if (interaction.isEmpty() && state != "released") moodBeforeInteraction = mood
        interaction = state
        interactionUntil = when (state) {
            "picked_up" -> System.currentTimeMillis() + 850L
            "tap" -> System.currentTimeMillis() + 620L
            "double_tap" -> System.currentTimeMillis() + 1100L
            "long_tap" -> System.currentTimeMillis() + 950L
            "released" -> System.currentTimeMillis() + 480L
            else -> 0L
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
        if (interaction != "dragging") moodBeforeInteraction = mood
        dragTilt = (dx * .12f).coerceIn(-14f, 14f)
        dragStretch = (1f + abs(dy) / 1700f).coerceIn(1f, 1.05f)
        interaction = "dragging"
        interactionUntil = 0L
        setMood("happy")
        invalidate()
    }

    fun clearDragMotion() {
        dragTilt = 0f
        dragStretch = 1f
        setInteraction("released")
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        if (interaction.isNotEmpty() && interactionUntil > 0L && now > interactionUntil) {
            interaction = ""
            interactionUntil = 0L
            setMood(moodBeforeInteraction)
        }
        val t = (now / 1000f)
        val dragging = interaction == "dragging"
        val bob = sin(t * 2.1f) * height * (if (dragging) .003f else .012f)
        val sway = sin(t * .85f) * (if (dragging) .4f else 2.4f)
        val breathe = 1f + sin(t * 1.8f) * (if (dragging) .003f else .012f)
        val pickup = if (interaction == "picked_up") 1.04f + sin(t * 18f) * .018f else 1f
        val settle = if (interaction == "released") 1f + sin(t * 12f) * .012f else 1f

        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.rotate(sway + dragTilt)
        canvas.scale(breathe * pickup * settle, breathe * dragStretch * pickup * settle)
        canvas.translate(-width / 2f, -height / 2f)
        super.onDraw(canvas)
        canvas.restore()
    }
}
