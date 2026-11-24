package com.chibicompanion

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/**
 * Lightweight, fully native 2D companion character.
 * The character is drawn in layers so idle movement, blinking and expressions
 * stay smooth without a WebView, game engine or heavyweight model runtime.
 */
class ChibiView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val start = System.currentTimeMillis()
    @Volatile private var mood = "neutral"

    fun setMood(value: String) { mood = value.lowercase(); invalidate() }

    private fun fill(color: Int): Paint {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        return p
    }
    private fun stroke(color: Int, width: Float): Paint {
        line.shader = null
        line.color = color
        line.strokeWidth = width
        return line
    }
    private fun path(vararg xy: Float): Path {
        val result = Path()
        if (xy.size >= 2) {
            result.moveTo(xy[0], xy[1])
            var i = 2
            while (i + 1 < xy.size) { result.lineTo(xy[i], xy[i + 1]); i += 2 }
            result.close()
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = (System.currentTimeMillis() - start) / 1000f
        val s = (width / 220f).coerceAtMost(height / 320f).coerceAtLeast(.08f)
        val bob = sin(t * 1.8f) * 2.3f
        val sway = sin(t * .72f) * 1.35f
        val breath = 1f + sin(t * 1.55f) * .009f
        val phase = t % 4.6f
        val blink = phase < .13f || mood == "sleepy"

        canvas.save()
        canvas.translate(width / 2f, height / 2f + bob)
        canvas.scale(s * breath, s * (2f - breath))
        canvas.rotate(sway * .42f)

        // Soft grounding shadow.
        p.shader = RadialGradient(0f, 154f, 52f,
            intArrayOf(0x443B4E85, 0x003B4E85), null, Shader.TileMode.CLAMP)
        canvas.drawOval(-54f, 145f, 54f, 161f, p)
        p.shader = null

        val silver = Color.rgb(235, 241, 255)
        val silverShade = Color.rgb(177, 192, 226)
        val navy = Color.rgb(43, 51, 83)
        val blue = Color.rgb(74, 143, 232)
        val paleBlue = Color.rgb(184, 216, 255)
        val skin = Color.rgb(255, 222, 211)
        val ink = Color.rgb(63, 75, 112)

        // Long back hair.
        val backHair = Path().apply {
            moveTo(-48f, -119f); cubicTo(-94f, -118f, -91f, -48f, -83f, 17f)
            cubicTo(-80f, 62f, -72f, 83f, -54f, 91f)
            cubicTo(-62f, 38f, -53f, -4f, -43f, -28f)
            cubicTo(43f, -28f, 53f, 37f, 48f, 91f)
            cubicTo(77f, 76f, 88f, 24f, 81f, -44f)
            cubicTo(78f, -104f, 47f, -133f, 0f, -139f)
            close()
        }
        p.shader = LinearGradient(-75f, -90f, 75f, 80f,
            intArrayOf(0xFFB9C9EB.toInt(), 0xFFF8FAFF.toInt(), 0xFFBAC9EA.toInt()),
            floatArrayOf(0f, .48f, 1f), Shader.TileMode.CLAMP)
        canvas.drawPath(backHair, p); p.shader = null

        // Legs, socks and chunky sneakers.
        canvas.drawRoundRect(-31f, 91f, -8f, 145f, 10f, 10f, fill(skin))
        canvas.drawRoundRect(8f, 91f, 31f, 145f, 10f, 10f, fill(skin))
        canvas.drawRoundRect(-32f, 116f, -7f, 151f, 9f, 9f, fill(Color.WHITE))
        canvas.drawRoundRect(7f, 116f, 32f, 151f, 9f, 9f, fill(Color.WHITE))
        canvas.drawLine(-31f, 123f, -8f, 123f, stroke(blue, 3f))
        canvas.drawLine(8f, 123f, 31f, 123f, stroke(blue, 3f))
        canvas.drawRoundRect(-42f, 143f, -3f, 163f, 9f, 9f, fill(navy))
        canvas.drawRoundRect(3f, 143f, 42f, 163f, 9f, 9f, fill(navy))
        canvas.drawRoundRect(-43f, 150f, -2f, 165f, 8f, 8f, fill(Color.WHITE))
        canvas.drawRoundRect(2f, 150f, 43f, 165f, 8f, 8f, fill(Color.WHITE))
        canvas.drawLine(-34f, 151f, -12f, 151f, stroke(blue, 3f))
        canvas.drawLine(12f, 151f, 34f, 151f, stroke(blue, 3f))
        canvas.drawCircle(-23f, 145f, 5f, fill(paleBlue))
        canvas.drawCircle(23f, 145f, 5f, fill(paleBlue))
        canvas.drawLine(-28f, 154f, -15f, 160f, stroke(blue, 2f))
        canvas.drawLine(28f, 154f, 15f, 160f, stroke(blue, 2f))

        // Pleated dark skirt.
        val skirt = Path().apply {
            moveTo(-39f, 63f); quadTo(0f, 70f, 39f, 63f)
            lineTo(48f, 105f); quadTo(0f, 119f, -48f, 105f); close()
        }
        canvas.drawPath(skirt, fill(navy))
        canvas.drawPath(skirt, stroke(0xFF7C9CD4.toInt(), 2f))
        for (x in -34..34 step 17) {
            canvas.drawLine(x.toFloat(), 73f, (x * 1.24f), 105f, stroke(0xFF8198C2.toInt(), 1.5f))
        }
        canvas.drawLine(-43f, 101f, 43f, 101f, stroke(paleBlue, 2.5f))

        // Hoodie torso and sleeves.
        val torso = Path().apply {
            moveTo(-36f, 8f); quadTo(0f, -1f, 36f, 8f)
            lineTo(41f, 72f); quadTo(0f, 82f, -41f, 72f); close()
        }
        p.shader = LinearGradient(-35f, 5f, 40f, 78f,
            intArrayOf(0xFFF9FCFF.toInt(), 0xFFBBD8FA.toInt(), 0xFFEAF3FF.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawPath(torso, p); p.shader = null
        canvas.drawPath(torso, stroke(0xFF8FAAD6.toInt(), 2f))
        val leftSleeve = Path().apply {
            moveTo(-34f, 12f); cubicTo(-61f, 8f, -66f, 43f, -52f, 68f)
            quadTo(-44f, 78f, -30f, 67f); lineTo(-23f, 28f); close()
        }
        val rightSleeve = Path().apply {
            moveTo(34f, 12f); cubicTo(61f, 8f, 66f, 43f, 52f, 68f)
            quadTo(44f, 78f, 30f, 67f); lineTo(23f, 28f); close()
        }
        canvas.drawPath(leftSleeve, fill(Color.rgb(242, 248, 255)))
        canvas.drawPath(rightSleeve, fill(Color.rgb(242, 248, 255)))
        canvas.drawPath(leftSleeve, stroke(0xFF9DBAE6.toInt(), 2f))
        canvas.drawPath(rightSleeve, stroke(0xFF9DBAE6.toInt(), 2f))
        canvas.drawLine(-52f, 62f, -31f, 66f, stroke(blue, 4f))
        canvas.drawLine(52f, 62f, 31f, 66f, stroke(blue, 4f))
        canvas.drawLine(-18f, 17f, -18f, 65f, stroke(0xFFB4C8E9.toInt(), 1.5f))
        canvas.drawLine(18f, 17f, 18f, 65f, stroke(0xFFB4C8E9.toInt(), 1.5f))

        // Neck, collar and ribbon.
        canvas.drawRoundRect(-10f, -3f, 10f, 21f, 7f, 7f, fill(skin))
        canvas.drawPath(path(-17f, 8f, 0f, 22f, 17f, 8f, 0f, 34f), fill(Color.WHITE))
        canvas.drawCircle(0f, 22f, 4f, fill(blue))
        canvas.drawPath(path(-3f, 25f, -24f, 15f, -18f, 37f), fill(navy))
        canvas.drawPath(path(3f, 25f, 24f, 15f, 18f, 37f), fill(navy))
        canvas.drawCircle(0f, 25f, 4f, fill(paleBlue))

        // Arms and little hands.
        canvas.drawOval(-59f, 62f, -43f, 80f, fill(skin))
        canvas.drawOval(43f, 62f, 59f, 80f, fill(skin))
        canvas.drawPath(path(-53f, 65f, -46f, 70f, -55f, 73f), stroke(0xFFD89F9A.toInt(), 1.3f))
        canvas.drawPath(path(53f, 65f, 46f, 70f, 55f, 73f), stroke(0xFFD89F9A.toInt(), 1.3f))

        // Face.
        p.shader = LinearGradient(-45f, -125f, 44f, 12f,
            intArrayOf(0xFFFFF0E9.toInt(), 0xFFFFDCCF.toInt(), 0xFFFFE8DF.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawOval(-56f, -132f, 56f, 14f, p); p.shader = null
        canvas.drawOval(-58f, -80f, -47f, -48f, fill(skin))
        canvas.drawOval(47f, -80f, 58f, -48f, fill(skin))

        // Hair cap and flowing side locks.
        p.shader = LinearGradient(-50f, -145f, 52f, -45f,
            intArrayOf(0xFFFFFFFF.toInt(), 0xFFE1E9FA.toInt(), 0xFFB8C8E8.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawOval(-61f, -151f, 61f, -42f, p); p.shader = null
        val leftLock = Path().apply {
            moveTo(-53f, -111f); cubicTo(-77f, -80f, -73f, -5f, -55f, 55f)
            cubicTo(-47f, 39f, -49f, -5f, -38f, -49f); close()
        }
        val rightLock = Path().apply {
            moveTo(53f, -111f); cubicTo(77f, -80f, 73f, -5f, 55f, 55f)
            cubicTo(47f, 39f, 49f, -5f, 38f, -49f); close()
        }
        canvas.drawPath(leftLock, fill(0xFFE9F0FF.toInt()))
        canvas.drawPath(rightLock, fill(0xFFDCE7FA.toInt()))

        // Big blue eyes with expressive lids.
        if (blink) {
            canvas.drawArc(-43f, -77f, -12f, -48f, 12f, 155f, false, stroke(ink, 3f))
            canvas.drawArc(12f, -77f, 43f, -48f, 12f, 155f, false, stroke(ink, 3f))
        } else {
            canvas.drawOval(-43f, -79f, -12f, -43f, fill(Color.WHITE))
            canvas.drawOval(12f, -79f, 43f, -43f, fill(Color.WHITE))
            p.shader = RadialGradient(-27f, -61f, 20f,
                intArrayOf(0xFF9CE1FF.toInt(), 0xFF3888E9.toInt(), 0xFF203C91.toInt()),
                null, Shader.TileMode.CLAMP)
            canvas.drawOval(-40f, -77f, -15f, -45f, p)
            p.shader = RadialGradient(27f, -61f, 20f,
                intArrayOf(0xFF9CE1FF.toInt(), 0xFF3888E9.toInt(), 0xFF203C91.toInt()),
                null, Shader.TileMode.CLAMP)
            canvas.drawOval(15f, -77f, 40f, -45f, p); p.shader = null
            canvas.drawOval(-32f, -65f, -22f, -49f, fill(0xFF17264F.toInt()))
            canvas.drawOval(22f, -65f, 32f, -49f, fill(0xFF17264F.toInt()))
            canvas.drawCircle(-31f, -68f, 5f, fill(Color.WHITE))
            canvas.drawCircle(23f, -68f, 5f, fill(Color.WHITE))
            canvas.drawCircle(-22f, -51f, 2.2f, fill(0xFFB9F0FF.toInt()))
            canvas.drawCircle(32f, -51f, 2.2f, fill(0xFFB9F0FF.toInt()))
            canvas.drawLine(-43f, -76f, -34f, -81f, stroke(navy, 2.5f))
            canvas.drawLine(43f, -76f, 34f, -81f, stroke(navy, 2.5f))
        }

        // Brows and cheeks.
        val browColor = if (mood == "angry") 0xFF75566E.toInt() else 0xFF9A8CA8.toInt()
        canvas.drawLine(-39f, -88f, -18f, -91f, stroke(browColor, 2.2f))
        canvas.drawLine(39f, -88f, 18f, -91f, stroke(browColor, 2.2f))
        p.shader = RadialGradient(-38f, -31f, 18f, 0x88F58FA6.toInt(), 0x00F58FA6, Shader.TileMode.CLAMP)
        canvas.drawOval(-52f, -40f, -25f, -25f, p)
        p.shader = RadialGradient(38f, -31f, 18f, 0x88F58FA6.toInt(), 0x00F58FA6, Shader.TileMode.CLAMP)
        canvas.drawOval(25f, -40f, 52f, -25f, p); p.shader = null

        // Mouth changes with the current emotion / speech state.
        when (mood) {
            "angry" -> canvas.drawLine(-7f, -14f, 7f, -14f, stroke(0xFF9A5C72.toInt(), 2.5f))
            "talking" -> {
                val open = 5f + (sin(t * 13f) + 1f) * 3f
                canvas.drawOval(-open, -20f, open, -5f, fill(0xFF8D455E.toInt()))
                canvas.drawOval(-open + 2f, -8f, open - 2f, -5f, fill(0xFFFFA1B6.toInt()))
            }
            "happy" -> {
                canvas.drawOval(-8f, -21f, 8f, -5f, fill(0xFF91465D.toInt()))
                canvas.drawOval(-5f, -8f, 5f, -5f, fill(0xFFFFA1B6.toInt()))
            }
            "sleepy" -> canvas.drawArc(-7f, -19f, 7f, -9f, 10f, 160f, false, stroke(0xFF9A5C72.toInt(), 2f))
            else -> canvas.drawArc(-7f, -20f, 7f, -9f, 15f, 150f, false, stroke(0xFF9A5C72.toInt(), 2f))
        }

        // Layered bangs.
        val bangs = Path().apply {
            moveTo(-57f, -104f); cubicTo(-42f, -150f, 17f, -155f, 57f, -105f)
            cubicTo(36f, -99f, 28f, -80f, 14f, -75f)
            cubicTo(8f, -91f, 4f, -105f, 0f, -112f)
            cubicTo(-8f, -88f, -18f, -77f, -29f, -78f)
            cubicTo(-28f, -93f, -38f, -103f, -57f, -104f); close()
        }
        p.shader = LinearGradient(-30f, -150f, 35f, -75f,
            intArrayOf(Color.WHITE, 0xFFE2EBFC.toInt(), 0xFFB9C9E8.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawPath(bangs, p); p.shader = null
        canvas.drawPath(bangs, stroke(0xFFB4C5E4.toInt(), 1.1f))
        canvas.drawPath(Path().apply { moveTo(-25f,-130f); cubicTo(-19f,-112f,-23f,-98f,-29f,-88f) }, stroke(Color.WHITE, 2f))
        canvas.drawPath(Path().apply { moveTo(7f,-137f); cubicTo(14f,-117f,17f,-103f,14f,-88f) }, stroke(Color.WHITE, 2f))

        // Blue flower clip and ribbon on the right side of her hair.
        canvas.drawCircle(43f, -111f, 11f, fill(blue))
        for (a in 0..4) {
            val angle = a * (Math.PI * 2.0 / 5.0)
            canvas.drawCircle(43f + (8.0 * kotlin.math.cos(angle)).toFloat(),
                -111f + (8.0 * kotlin.math.sin(angle)).toFloat(), 4.2f, fill(0xFFEAF6FF.toInt()))
        }
        canvas.drawCircle(43f, -111f, 3f, fill(0xFFFFD66B.toInt()))
        canvas.drawPath(path(53f, -117f, 70f, -105f, 64f, -87f, 53f, -98f), fill(navy))
        canvas.drawPath(path(53f, -100f, 67f, -88f, 54f, -79f), fill(blue))

        // A tiny floating heart reacts to her mood.
        if (mood == "happy" || mood == "shy" || mood == "talking") {
            val heart = Path().apply {
                moveTo(76f, -116f); cubicTo(61f, -128f, 57f, -109f, 76f, -96f)
                cubicTo(95f, -109f, 91f, -128f, 76f, -116f); close()
            }
            canvas.drawPath(heart, fill(if (mood == "shy") 0xFFFFA4C0.toInt() else 0xFFFF8FB8.toInt()))
        }

        // Hair shine strands.
        canvas.drawPath(Path().apply { moveTo(-51f,-75f); cubicTo(-58f,-25f,-55f,20f,-48f,43f) }, stroke(0x99FFFFFF.toInt(), 2f))
        canvas.drawPath(Path().apply { moveTo(51f,-72f); cubicTo(58f,-20f,57f,20f,51f,43f) }, stroke(0x99FFFFFF.toInt(), 2f))

        canvas.restore()
        postInvalidateDelayed(55)
    }
}
