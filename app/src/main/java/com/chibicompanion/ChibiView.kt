package com.chibicompanion

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Lightweight native Canvas chibi; no WebView, Live2D runtime, or image assets required. */
class ChibiView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hair = Color.rgb(238, 243, 255)
    private val outline = Color.rgb(90, 105, 145)
    private val skin = Color.rgb(255, 224, 211)
    private val blue = Color.rgb(85, 145, 235)
    private val start = System.currentTimeMillis()
    @Volatile private var mood: String = "neutral"
    fun setMood(value: String) { mood = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = (System.currentTimeMillis() - start) / 1000f
        val s = (width.coerceAtMost(height) / 160f).coerceAtLeast(.1f)
        canvas.save(); canvas.translate(width / 2f, height / 2f + sin(t * 2.1f) * 2.5f * s); canvas.scale(s, s)
        // soft shadow
        p.color = 0x223D5B91; canvas.drawOval(-42f, 66f, 42f, 77f, p)
        // tiny body and hoodie
        p.color = Color.rgb(177, 205, 250); canvas.drawRoundRect(-34f, 30f, 34f, 73f, 18f, 18f, p)
        p.color = Color.WHITE; canvas.drawCircle(-13f, 48f, 4f, p); canvas.drawCircle(13f, 48f, 4f, p)
        // face
        p.color = skin; canvas.drawOval(-43f, -39f, 43f, 43f, p)
        // hair cap and side locks
        p.color = hair; canvas.drawOval(-47f, -54f, 47f, 9f, p)
        val bangs = Path().apply { moveTo(-42f,-17f); quadTo(-32f,-49f,-14f,-28f); quadTo(-4f,-53f,8f,-28f); quadTo(26f,-49f,43f,-15f); quadTo(20f,-27f,5f,-13f); quadTo(-15f,-29f,-42f,-17f); close() }
        canvas.drawPath(bangs,p)
        p.color = hair; canvas.drawRoundRect(-48f,-12f,-37f,42f,8f,8f,p); canvas.drawRoundRect(37f,-12f,48f,42f,8f,8f,p)
        // eyes blink briefly every few seconds
        val blink = (t % 4.2f) in 0.08f..0.22f
        p.color = outline; p.strokeWidth = 2.5f
        if (mood == "angry") {
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f
            canvas.drawLine(-28f,-7f,-12f,-2f,p); canvas.drawLine(12f,-2f,28f,-7f,p); p.style = Paint.Style.FILL
        }
        if (blink || mood == "sleepy") { canvas.drawLine(-25f,4f,-12f,4f,p); canvas.drawLine(12f,4f,25f,4f,p) }
        else {
            p.color = Color.WHITE; canvas.drawOval(-28f,-1f,-10f,20f,p); canvas.drawOval(10f,-1f,28f,20f,p)
            p.color = blue; canvas.drawOval(-25f,1f,-12f,18f,p); canvas.drawOval(12f,1f,25f,18f,p)
            p.color = Color.rgb(35,55,90); canvas.drawCircle(-19f,9f,5f,p); canvas.drawCircle(19f,9f,5f,p)
            p.color = Color.WHITE; canvas.drawCircle(-21f,5f,2f,p); canvas.drawCircle(17f,5f,2f,p)
        }
        // blush and tiny tsundere mouth
        p.color = if (mood == "shy") 0x99F28D9B.toInt() else 0x66F28D9B.toInt(); canvas.drawOval(-37f,19f,-23f,26f,p); canvas.drawOval(23f,19f,37f,26f,p)
        p.color = outline; p.style = Paint.Style.STROKE; p.strokeWidth = 2f
        when (mood) {
            "happy" -> canvas.drawArc(-7f,17f,7f,31f,10f,160f,false,p)
            "angry" -> canvas.drawLine(-5f,27f,5f,27f,p)
            "talking" -> canvas.drawOval(-4f,22f,4f,31f,p)
            else -> canvas.drawArc(-5f,21f,5f,29f,10f,160f,false,p)
        }; p.style = Paint.Style.FILL
        // ahoge
        p.color = hair; p.style = Paint.Style.STROKE; p.strokeWidth = 4f
        val strand = Path().apply { moveTo(3f,-51f); cubicTo(-13f,-76f,20f,-71f,11f,-57f) }; canvas.drawPath(strand,p); p.style = Paint.Style.FILL
        canvas.restore()
        postInvalidateDelayed(45)
    }
}
