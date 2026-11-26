package com.chibicompanion

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.sin

/**
 * Temporary, code-drawn Miko avatar.
 * This is deliberately an animated placeholder, not the final Live2D/3D character asset.
 */
class ModelSlotView(context: Context) : FrameLayout(context) {
    private val avatar = AnimatedMikoView(context)

    init {
        setBackgroundColor(Color.TRANSPARENT)
        contentDescription = "Animated temporary Miko character placeholder"
        addView(avatar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val label = TextView(context).apply {
            text = "MIKO  ✦  HERE WITH YOU"
            textSize = 9f
            letterSpacing = .12f
            setTextColor(Color.rgb(222, 218, 255))
            gravity = Gravity.CENTER
            setPadding(CompanionUi.dp(context, 10), CompanionUi.dp(context, 5),
                CompanionUi.dp(context, 10), CompanionUi.dp(context, 5))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(115, 17, 20, 45))
                cornerRadius = CompanionUi.dp(context, 20).toFloat()
                setStroke(CompanionUi.dp(context, 1), Color.argb(100, 180, 168, 255))
            }
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = CompanionUi.dp(context, 9)
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        avatar.startMotion()
    }

    override fun onDetachedFromWindow() {
        avatar.stopMotion()
        super.onDetachedFromWindow()
    }
}

private class AnimatedMikoView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var motion: ValueAnimator? = null
    private val scaleRect = RectF()

    fun startMotion() {
        if (motion?.isRunning == true) return
        motion = ValueAnimator.ofFloat(0f, (2f * PI).toFloat()).apply {
            duration = 3600L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopMotion() {
        motion?.cancel()
        motion = null
    }

    private fun paint(color: Int, style: Paint.Style = Paint.Style.FILL, width: Float = 1f) {
        p.color = color
        p.style = style
        p.strokeWidth = width
        p.shader = null
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
    }

    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, color: Int) {
        paint(color)
        c.drawOval(x-rx, y-ry, x+rx, y+ry, p)
    }

    private fun path(c: Canvas, color: Int, block: Path.() -> Unit) {
        val shape = Path().apply(block)
        paint(color)
        c.drawPath(shape, p)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Soft ambient glow behind the character.
        paint(Color.WHITE)
        p.shader = RadialGradient(w*.5f, h*.53f, w*.43f,
            intArrayOf(Color.argb(75, 126, 105, 255), Color.argb(0, 75, 119, 255)),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(w*.5f, h*.54f, w*.43f, p)
        p.shader = null

        val scale = minOf(w/250f, h/300f)
        canvas.save()
        canvas.translate(w*.5f, h*.53f + sin(phase.toDouble()).toFloat()*4f*scale)
        canvas.scale(scale, scale)

        // Ground shadow gently changes with the floating motion.
        val shadowAlpha = (38 - sin(phase.toDouble()).toFloat()*7).toInt().coerceIn(20, 50)
        oval(canvas, 0f, 127f, 49f, 8f, Color.argb(shadowAlpha, 8, 10, 28))

        val breathe = sin(phase.toDouble()).toFloat()*1.8f
        canvas.save()
        canvas.scale(1f, 1f + breathe*.008f, 0f, 60f)

        // Long silver hair flowing behind the body.
        path(canvas, Color.rgb(179, 190, 226)) {
            moveTo(-49f,-47f); cubicTo(-77f,-5f,-68f,69f,-57f,111f)
            cubicTo(-49f,130f,-30f,112f,-34f,82f); lineTo(-31f,8f); close()
        }
        path(canvas, Color.rgb(211, 220, 245)) {
            moveTo(48f,-46f); cubicTo(78f,-2f,69f,72f,57f,111f)
            cubicTo(50f,129f,30f,112f,34f,81f); lineTo(30f,7f); close()
        }

        // Tiny body, outfit, sleeves and legs.
        oval(canvas, -14f, 93f, 7f, 17f, Color.rgb(246, 218, 218))
        oval(canvas, 14f, 93f, 7f, 17f, Color.rgb(246, 218, 218))
        path(canvas, Color.rgb(69, 68, 130)) {
            moveTo(-25f,42f); cubicTo(-33f,59f,-29f,91f,-23f,105f)
            lineTo(23f,105f); cubicTo(29f,88f,33f,58f,25f,42f)
            cubicTo(12f,34f,-12f,34f,-25f,42f); close()
        }
        path(canvas, Color.rgb(165, 139, 235)) {
            moveTo(-25f,48f); cubicTo(-39f,58f,-38f,78f,-28f,81f)
            lineTo(-16f,65f); close()
        }
        path(canvas, Color.rgb(165, 139, 235)) {
            moveTo(25f,48f); cubicTo(39f,58f,38f,78f,28f,81f)
            lineTo(16f,65f); close()
        }
        oval(canvas, -29f, 79f, 7f, 7f, Color.rgb(250, 220, 222))
        oval(canvas, 29f, 79f, 7f, 7f, Color.rgb(250, 220, 222))
        path(canvas, Color.rgb(235, 224, 255)) {
            moveTo(-24f,57f); lineTo(0f,76f); lineTo(24f,57f)
            lineTo(19f,96f); lineTo(-19f,96f); close()
        }
        oval(canvas, 0f, 56f, 5f, 5f, Color.rgb(255, 221, 239))

        // Face and side locks.
        path(canvas, Color.rgb(189, 199, 232)) {
            moveTo(-52f,-56f); cubicTo(-64f,-20f,-57f,23f,-43f,48f)
            lineTo(-29f,31f); lineTo(-35f,-30f); close()
        }
        path(canvas, Color.rgb(189, 199, 232)) {
            moveTo(52f,-56f); cubicTo(64f,-20f,57f,23f,43f,48f)
            lineTo(29f,31f); lineTo(35f,-30f); close()
        }
        oval(canvas, 0f, -14f, 48f, 57f, Color.rgb(255, 224, 216))
        // Ears.
        oval(canvas, -46f, -12f, 8f, 13f, Color.rgb(255, 211, 207))
        oval(canvas, 46f, -12f, 8f, 13f, Color.rgb(255, 211, 207))

        // Hair cap and layered bangs.
        path(canvas, Color.rgb(219, 228, 249)) {
            moveTo(-49f,-24f); cubicTo(-57f,-80f,-25f,-91f,0f,-88f)
            cubicTo(39f,-88f,58f,-58f,48f,-22f)
            cubicTo(31f,-42f,22f,-49f,9f,-55f)
            cubicTo(-5f,-34f,-29f,-27f,-49f,-24f); close()
        }
        path(canvas, Color.rgb(239, 243, 255)) {
            moveTo(-45f,-37f); cubicTo(-27f,-57f,-20f,-65f,-8f,-75f)
            cubicTo(-11f,-48f,-20f,-32f,-37f,-18f); close()
        }
        path(canvas, Color.rgb(202, 214, 242)) {
            moveTo(-8f,-76f); cubicTo(7f,-68f,16f,-52f,15f,-34f)
            cubicTo(7f,-42f,0f,-53f,-8f,-76f); close()
        }
        path(canvas, Color.rgb(229, 236, 252)) {
            moveTo(12f,-76f); cubicTo(39f,-65f,47f,-45f,43f,-22f)
            cubicTo(29f,-36f,22f,-51f,12f,-76f); close()
        }

        // Eyes blink briefly once each animation loop.
        val blink = phase > 3.00f && phase < 3.22f
        val eyeH = if (blink) 1.2f else 12f
        oval(canvas, -19f, -10f, 7.5f, eyeH, Color.rgb(55, 93, 159))
        oval(canvas, 19f, -10f, 7.5f, eyeH, Color.rgb(55, 93, 159))
        if (!blink) {
            oval(canvas, -19f, -13f, 3.2f, 6.8f, Color.rgb(111, 190, 255))
            oval(canvas, 19f, -13f, 3.2f, 6.8f, Color.rgb(111, 190, 255))
            oval(canvas, -21f, -17f, 2.4f, 2.4f, Color.WHITE)
            oval(canvas, 17f, -17f, 2.4f, 2.4f, Color.WHITE)
        }
        // Eyebrows, blush and small smile.
        paint(Color.rgb(128, 112, 164), Paint.Style.STROKE, 2.3f)
        canvas.drawArc(RectF(-28f,-29f,-11f,-21f), 190f, 150f, false, p)
        canvas.drawArc(RectF(11f,-29f,28f,-21f), 200f, 150f, false, p)
        oval(canvas, -31f, 8f, 7f, 3.5f, Color.argb(95, 244, 120, 147))
        oval(canvas, 31f, 8f, 7f, 3.5f, Color.argb(95, 244, 120, 147))
        paint(Color.rgb(190, 104, 131), Paint.Style.STROKE, 2.2f)
        canvas.drawArc(RectF(-6f, 4f, 6f, 14f), 15f, 150f, false, p)

        // Tiny hair ribbon and floating sparkles.
        path(canvas, Color.rgb(170, 143, 238)) {
            moveTo(34f,-63f); cubicTo(45f,-77f,58f,-68f,48f,-56f)
            lineTo(37f,-50f); close()
        }
        path(canvas, Color.rgb(198, 176, 255)) {
            moveTo(38f,-63f); cubicTo(24f,-76f,19f,-61f,30f,-54f)
            lineTo(39f,-53f); close()
        }
        oval(canvas, 36f, -56f, 4f, 4f, Color.rgb(255, 225, 246))
        paint(Color.rgb(255, 230, 255), Paint.Style.STROKE, 2.4f)
        canvas.drawLine(-74f,-42f,-74f,-54f,p); canvas.drawLine(-80f,-48f,-68f,-48f,p)
        canvas.drawLine(75f,17f,75f,5f,p); canvas.drawLine(69f,11f,81f,11f,p)

        canvas.restore()
        canvas.restore()
    }
}
