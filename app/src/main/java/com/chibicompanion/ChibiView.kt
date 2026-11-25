package com.chibicompanion

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Super-deformed chibi companion: oversized head, tiny body and expressive
 * animated features. Native Canvas keeps rendering light on mid-range phones.
 */
class ChibiView(context: Context) : View(context) {
    @Volatile private var mood = "excited"
    @Volatile private var speaking = false
    @Volatile private var moodChangedAt = System.currentTimeMillis()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val started = System.currentTimeMillis()

    fun setMood(value: String) {
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
    fun setSpeaking(value: Boolean) { speaking = value; invalidate() }
    private fun f(c: Int) = fill.apply { shader = null; style = Paint.Style.FILL; color = c }
    private fun s(c: Int, w: Float) = line.apply { shader = null; color = c; strokeWidth = w }
    private fun shape(vararg xy: Float): Path = Path().apply {
        if (xy.size >= 2) {
            moveTo(xy[0], xy[1]); var i = 2
            while (i + 1 < xy.size) { lineTo(xy[i], xy[i + 1]); i += 2 }
            close()
        }
    }

    private fun star(cx: Float, cy: Float, outer: Float, inner: Float): Path =
        Path().apply {
            for (i in 0 until 10) {
                val angle = -Math.PI / 2.0 + i * Math.PI / 5.0
                val radius = if (i % 2 == 0) outer else inner
                val x = cx + (kotlin.math.cos(angle) * radius).toFloat()
                val y = cy + (kotlin.math.sin(angle) * radius).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val t = (System.currentTimeMillis() - started) / 1000f
        val scale = (width / 220f).coerceAtMost(height / 320f).coerceAtLeast(.08f)
        val bounce = sin(t * 2.2f) * 2.5f
        val tilt = sin(t * .9f) * 1.8f
        val blink = ((t % 4.1f) < .14f && mood !in listOf("excited", "angry", "surprised", "determined", "affectionate")) || mood == "sleepy"
        val talking = speaking || mood == "talking"
        val transition = ((System.currentTimeMillis() - moodChangedAt) / 240f).coerceIn(0f, 1f)
        val emotionPop = 1f + sin(transition * Math.PI).toFloat() * .035f
        val lookX = sin(t * .42f) * 2.2f
        val lookY = cos(t * .31f) * 1.2f
        val skin = 0xFFFFDED4.toInt()
        val ink = 0xFF39466D.toInt()
        val blue = 0xFF438DEB.toInt()
        val pale = 0xFFDCEAFF.toInt()
        val hair = 0xFFF3F6FF.toInt()
        val navy = 0xFF343B61.toInt()

        c.save()
        c.translate(width / 2f, height / 2f + bounce)
        c.scale(scale * emotionPop, scale * emotionPop)
        c.scale(1f + sin(t * 2.0f) * .008f, 1f + cos(t * 2.0f) * .012f)
        c.rotate(tilt)

        // Tiny feet and body: intentionally much smaller than the head.
        c.drawOval(-40f, 142f, 40f, 154f, f(0x333C4C7A))
        c.drawRoundRect(-25f, 104f, -8f, 137f, 8f, 8f, f(skin))
        c.drawRoundRect(8f, 104f, 25f, 137f, 8f, 8f, f(skin))
        c.drawRoundRect(-33f, 128f, -5f, 145f, 9f, 9f, f(navy))
        c.drawRoundRect(5f, 128f, 33f, 145f, 9f, 9f, f(navy))
        c.drawRoundRect(-34f, 136f, -5f, 147f, 7f, 7f, f(Color.WHITE))
        c.drawRoundRect(5f, 136f, 34f, 147f, 7f, 7f, f(Color.WHITE))

        // Small hoodie body and short sleeves.
        val body = Path().apply {
            moveTo(-29f, 48f); quadTo(0f, 39f, 29f, 48f)
            quadTo(39f, 79f, 30f, 111f); quadTo(0f, 121f, -30f, 111f)
            quadTo(-39f, 79f, -29f, 48f); close()
        }
        fill.shader = LinearGradient(-30f, 45f, 30f, 116f,
            intArrayOf(0xFFFFFEFF.toInt(), 0xFFBBD9FF.toInt(), 0xFFEAF3FF.toInt()),
            null, Shader.TileMode.CLAMP)
        c.drawPath(body, fill); fill.shader = null
        c.drawPath(body, s(0xFF91B2E3.toInt(), 2f))
        c.drawRoundRect(-48f, 55f, -27f, 91f, 12f, 12f, f(0xFFEAF2FF.toInt()))
        c.drawRoundRect(27f, 55f, 48f, 91f, 12f, 12f, f(0xFFEAF2FF.toInt()))
        c.drawCircle(-39f, 91f, 9f, f(skin))
        c.drawCircle(39f, 91f, 9f, f(skin))
        c.drawPath(shape(-8f, 53f, 0f, 64f, 8f, 53f, 0f, 76f), f(Color.WHITE))
        c.drawCircle(0f, 68f, 3f, f(blue))
        c.drawLine(0f, 78f, 0f, 109f, s(0xFF9EB9E3.toInt(), 1.5f))
        c.drawCircle(0f, 91f, 3f, f(0xFF78A7EA.toInt()))

        // Huge round head and soft ears.
        c.drawCircle(-77f, -35f, 12f, f(skin))
        c.drawCircle(77f, -35f, 12f, f(skin))
        fill.shader = LinearGradient(-75f, -130f, 70f, 50f,
            intArrayOf(0xFFFFF5F0.toInt(), 0xFFFFDCCF.toInt(), 0xFFFFE9E1.toInt()),
            null, Shader.TileMode.CLAMP)
        c.drawOval(-91f, -139f, 91f, 48f, fill); fill.shader = null

        // Flowing white/silver hair behind the face.
        val back = Path().apply {
            moveTo(-65f,-98f); cubicTo(-111f,-154f,-94f,-8f,-79f,58f)
            quadTo(-62f,74f,-54f,35f); cubicTo(0f,62f,54f,35f,54f,35f)
            quadTo(66f,78f,81f,53f); cubicTo(105f,-28f,107f,-123f,42f,-147f)
            quadTo(-26f,-174f,-65f,-98f); close()
        }
        fill.shader = LinearGradient(-90f,-130f,90f,65f,
            intArrayOf(0xFFB9C8E6.toInt(),0xFFFDFEFF.toInt(),0xFFD3DFF4.toInt()),
            null,Shader.TileMode.CLAMP)
        c.drawPath(back,fill); fill.shader=null

        // Oversized fringe and side locks frame the face.
        c.drawOval(-91f,-153f,91f,25f,f(hair))
        val fringe = Path().apply {
            moveTo(-83f,-91f); cubicTo(-84f,-173f,48f,-183f,84f,-91f)
            cubicTo(51f,-101f,37f,-61f,17f,-55f)
            cubicTo(10f,-87f,4f,-105f,0f,-111f)
            cubicTo(-13f,-76f,-32f,-55f,-48f,-62f)
            cubicTo(-49f,-81f,-62f,-92f,-83f,-91f); close()
        }
        fill.shader = LinearGradient(-55f,-160f,55f,-52f,
            intArrayOf(Color.WHITE,0xFFE6EEFC.toInt(),0xFFBDCAE5.toInt()),null,Shader.TileMode.CLAMP)
        c.drawPath(fringe,fill); fill.shader=null
        c.drawPath(fringe,s(0xFFB8C7E2.toInt(),1.4f))
        c.drawPath(Path().apply { moveTo(-35f,-140f); cubicTo(-24f,-112f,-34f,-91f,-45f,-78f) },s(Color.WHITE,3f))
        c.drawPath(Path().apply { moveTo(17f,-151f); cubicTo(31f,-121f,30f,-95f,20f,-76f) },s(Color.WHITE,3f))
        c.drawPath(Path().apply { moveTo(-78f,-51f); cubicTo(-91f,-3f,-79f,26f,-70f,43f) },s(0xFFB6C7E7.toInt(),4f))
        c.drawPath(Path().apply { moveTo(78f,-51f); cubicTo(91f,-3f,79f,26f,70f,43f) },s(0xFFB6C7E7.toInt(),4f))

        // Big sparkling blue eyes, with a proper blink.
        if (mood == "excited") {
            // Miko's signature anime sparkle-eyes expression.
            c.drawOval(-61f,-59f,-16f,-9f,f(Color.WHITE))
            c.drawOval(16f,-59f,61f,-9f,f(Color.WHITE))
            c.drawPath(star(-38f,-34f,25f,11f), f(0xFF4B8FEF.toInt()))
            c.drawPath(star(38f,-34f,25f,11f), f(0xFF4B8FEF.toInt()))
            c.drawPath(star(-38f,-35f,12f,5f), f(0xFFFFF8D8.toInt()))
            c.drawPath(star(38f,-35f,12f,5f), f(0xFFFFF8D8.toInt()))
            c.drawCircle(-49f,-48f,3f,f(Color.WHITE))
            c.drawCircle(27f,-48f,3f,f(Color.WHITE))
        } else if (mood == "angry") {
            c.drawOval(-60f,-47f,-17f,-20f,f(Color.WHITE))
            c.drawOval(17f,-47f,60f,-20f,f(Color.WHITE))
            c.drawOval(-53f,-44f,-25f,-21f,f(0xFF356DCB.toInt()))
            c.drawOval(25f,-44f,53f,-21f,f(0xFF356DCB.toInt()))
            c.drawLine(-63f,-73f,-29f,-59f,s(0xFF9D435D.toInt(),5f))
            c.drawLine(63f,-73f,29f,-59f,s(0xFF9D435D.toInt(),5f))
        } else if (mood == "teasing") {
            c.drawArc(-60f,-55f,-17f,-17f,10f,160f,false,s(ink,4f))
            c.drawOval(18f,-57f,59f,-12f,f(Color.WHITE))
            c.drawOval(27f,-53f,54f,-14f,f(0xFF438FEA.toInt()))
            c.drawCircle(32f,-47f,6f,f(Color.WHITE))
        } else if (mood == "affectionate") {
            c.drawOval(-61f,-59f,-16f,-9f,f(Color.WHITE))
            c.drawOval(16f,-59f,61f,-9f,f(Color.WHITE))
            val heartEye = Path().apply {
                moveTo(-38f,-22f); cubicTo(-61f,-38f,-58f,-55f,-42f,-49f)
                cubicTo(-31f,-58f,-18f,-39f,-38f,-22f); close()
            }
            c.drawPath(heartEye,f(0xFF438DEB.toInt()))
            c.save(); c.scale(-1f,1f); c.drawPath(heartEye,f(0xFF438DEB.toInt())); c.restore()
            c.drawCircle(-45f,-47f,4f,f(Color.WHITE)); c.drawCircle(31f,-47f,4f,f(Color.WHITE))
        } else if (mood == "determined") {
            c.drawOval(-60f,-50f,-17f,-16f,f(Color.WHITE)); c.drawOval(17f,-50f,60f,-16f,f(Color.WHITE))
            c.drawOval(-52f,-46f,-25f,-17f,f(0xFF356DCB.toInt())); c.drawOval(25f,-46f,52f,-17f,f(0xFF356DCB.toInt()))
            c.drawLine(-65f,-65f,-26f,-76f,s(navy,4f)); c.drawLine(65f,-65f,26f,-76f,s(navy,4f))
        } else if (mood == "pouty") {
            c.drawOval(-61f,-54f,-16f,-13f,f(Color.WHITE)); c.drawOval(16f,-54f,61f,-13f,f(Color.WHITE))
            c.drawOval(-53f,-47f,-25f,-14f,f(0xFF438DEB.toInt())); c.drawOval(25f,-47f,53f,-14f,f(0xFF438DEB.toInt()))
            c.drawLine(-62f,-68f,-28f,-62f,s(navy,4f)); c.drawLine(62f,-68f,28f,-62f,s(navy,4f))
        } else if (mood == "sleepy" || mood == "sad" || mood == "crying" || mood == "shy" || mood == "embarrassed" || blink) {
            val eyeDrop = if (mood == "sleepy") 0f else 5f
            c.drawArc(-61f,-54f+eyeDrop,-16f,-15f+eyeDrop,8f,164f,false,s(ink,4.5f))
            c.drawArc(16f,-54f+eyeDrop,61f,-15f+eyeDrop,8f,164f,false,s(ink,4.5f))
            if (mood == "sad" || mood == "crying") {
                c.drawOval(-55f,-30f,-48f,-17f,f(0xFF78C9F4.toInt()))
                c.drawOval(48f,-30f,55f,-17f,f(0xFF78C9F4.toInt()))
            }
        } else {
            c.drawOval(-61f,-59f,-16f,-9f,f(Color.WHITE))
            c.drawOval(16f,-59f,61f,-9f,f(Color.WHITE))
            c.save()
            c.translate(lookX, lookY)
            fill.shader = RadialGradient(-37f,-34f,28f,
                intArrayOf(0xFFB7F0FF.toInt(),0xFF438FEA.toInt(),0xFF203A87.toInt()),null,Shader.TileMode.CLAMP)
            c.drawOval(-57f,-56f,-20f,-12f,fill)
            fill.shader = RadialGradient(37f,-34f,28f,
                intArrayOf(0xFFB7F0FF.toInt(),0xFF438FEA.toInt(),0xFF203A87.toInt()),null,Shader.TileMode.CLAMP)
            c.drawOval(20f,-56f,57f,-12f,fill); fill.shader=null
            c.drawOval(-46f,-42f,-32f,-16f,f(0xFF18254D.toInt()))
            c.drawOval(32f,-42f,46f,-16f,f(0xFF18254D.toInt()))
            c.drawCircle(-46f,-47f,7f,f(Color.WHITE)); c.drawCircle(32f,-47f,7f,f(Color.WHITE))
            c.drawCircle(-29f,-17f,3f,f(0xFFB9F4FF.toInt())); c.drawCircle(49f,-17f,3f,f(0xFFB9F4FF.toInt()))
            c.restore()
            c.drawLine(-59f,-65f,-42f,-69f,s(navy,3f)); c.drawLine(59f,-65f,42f,-69f,s(navy,3f))
        }

        // Blush and brows change with emotion.
        c.drawOval(-77f,-2f,-43f,14f,f(0x66F28FA6))
        c.drawOval(43f,-2f,77f,14f,f(0x66F28FA6))
        val brow = if (mood == "angry") 0xFF9D435D.toInt() else 0xFF9B8AA4.toInt()
        if (mood != "angry" && mood != "determined") {
            if (mood == "confused") {
                c.drawLine(-55f,-82f,-30f,-74f,s(brow,2.8f))
                c.drawLine(55f,-76f,30f,-79f,s(brow,2.8f))
            } else {
                c.drawLine(-55f,-76f,-30f,-79f,s(brow,2.8f))
                c.drawLine(55f,-76f,30f,-79f,s(brow,2.8f))
            }
        }
        if (mood == "crying" || mood == "sad") {
            c.drawOval(-57f,-8f,-49f,22f,f(0xFF78C9F4.toInt()))
            c.drawOval(49f,-8f,57f,22f,f(0xFF78C9F4.toInt()))
        }

        when {
            talking -> {
                val open = 5f + (sin(t*13f)+1f)*4f
                c.drawOval(-open,18f,open,38f,f(0xFF8D405B.toInt()))
                c.drawOval(-open+2f,30f,open-2f,36f,f(0xFFFFA4B9.toInt()))
            }
            mood == "angry" -> c.drawLine(-12f,26f,12f,26f,s(0xFF98566B.toInt(),3.5f))
            mood == "pouty" -> c.drawOval(-13f,22f,13f,39f,f(0xFF98566B.toInt()))
            mood == "surprised" || mood == "confused" -> c.drawOval(-8f,19f,8f,41f,f(0xFF91445E.toInt()))
            mood == "determined" -> c.drawArc(-13f,21f,13f,37f,190f,160f,false,s(0xFF98566B.toInt(),3f))
            mood == "sad" || mood == "crying" -> c.drawArc(-12f,26f,12f,43f,195f,150f,false,s(0xFF98566B.toInt(),2.8f))
            mood == "shy" || mood == "embarrassed" -> c.drawArc(-12f,20f,12f,34f,15f,150f,false,s(0xFF98566B.toInt(),2.5f))
            mood == "happy" -> {
                c.drawOval(-12f,16f,12f,40f,f(0xFF91445E.toInt()))
                c.drawOval(-7f,31f,7f,38f,f(0xFFFFA4B9.toInt()))
            }
            mood == "sleepy" -> c.drawArc(-10f,19f,10f,32f,5f,170f,false,s(0xFF98566B.toInt(),2.5f))
            else -> c.drawArc(-10f,18f,10f,33f,15f,150f,false,s(0xFF98566B.toInt(),2.5f))
        }

        // Signature blue flower hair clip.
        c.drawCircle(67f,-107f,13f,f(blue))
        for (i in 0..4) {
            val a=i*Math.PI*2/5
            c.drawCircle(67f+(10*cos(a)).toFloat(),-107f+(10*sin(a)).toFloat(),5f,f(0xFFEAF6FF.toInt()))
        }
        c.drawCircle(67f,-107f,3.5f,f(0xFFFFD66B.toInt()))
        c.drawPath(shape(76f,-112f,92f,-99f,83f,-80f,73f,-94f),f(navy))
        if (mood == "excited" || mood == "affectionate") {
            val twinkle = 0.75f + (sin(t * 5f) + 1f) * .25f
            c.save(); c.scale(twinkle, twinkle, -99f, -84f)
            c.drawPath(star(-99f,-84f,9f,4f), f(0xFFFFD76B.toInt())); c.restore()
            c.save(); c.scale(1.1f - twinkle * .15f, 1.1f - twinkle * .15f, 103f, -61f)
            c.drawPath(star(103f,-61f,7f,3f), f(0xFFFFD76B.toInt())); c.restore()
        }
        if (mood == "confused") {
            c.drawArc(91f,-105f,111f,-75f,190f,150f,false,s(0xFF78A7EA.toInt(),2.5f))
            c.drawCircle(105f,-112f,3f,f(0xFF78A7EA.toInt()))
        }
        if (mood == "happy" || mood == "shy" || mood == "excited" || mood == "affectionate" || talking) {
            val heart=Path().apply {
                moveTo(95f,-132f); cubicTo(78f,-147f,73f,-124f,95f,-109f)
                cubicTo(117f,-124f,112f,-147f,95f,-132f); close()
            }
            c.drawPath(heart,f(if(mood=="shy") 0xFFFFA4C0.toInt() else 0xFFFF8FB8.toInt()))
        }

        c.restore()
        postInvalidateDelayed(50)
    }
}
