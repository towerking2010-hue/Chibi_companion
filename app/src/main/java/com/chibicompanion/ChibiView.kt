package com.chibicompanion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Crisp, resolution-independent Miko character renderer.
 *
 * This is deliberately drawn with Android Canvas primitives rather than a
 * tiny enlarged bitmap. It provides long hair, a complete chibi body, clear
 * facial expressions, blinking, breathing and separate hair sway. It is a
 * stylized vector character, not a rigged Live2D model.
 */
open class ChibiView(context: Context) : View(context) {
    @Volatile protected var currentMood = "excited"
    @Volatile private var speaking = false
    private var moodChangedAt = System.nanoTime()
    private val startedAt = System.nanoTime()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val outline = Color.rgb(104, 111, 151)
    private val hair = Color.rgb(225, 231, 247)
    private val hairShade = Color.rgb(177, 193, 224)
    private val skin = Color.rgb(255, 224, 211)
    private val eyeBlue = Color.rgb(65, 139, 226)
    private val ink = Color.rgb(62, 68, 100)

    init {
        isClickable = true
        isFocusable = true
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    open fun setMood(value: String) {
        val next = when (value.lowercase()) {
            "embarrassed" -> "shy"
            "love", "loving" -> "affectionate"
            "talk" -> "talking"
            "pout", "pouting" -> "pouty"
            else -> value.lowercase()
        }
        if (next != currentMood) {
            currentMood = next
            moodChangedAt = System.nanoTime()
            invalidate()
        }
    }

    open fun setSpeaking(value: Boolean) {
        speaking = value
        invalidate()
    }

    private fun fill(color: Int, style: Paint.Style = Paint.Style.FILL) {
        p.shader = null
        p.color = color
        p.style = style
        p.strokeWidth = 2.2f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
    }

    private fun path(c: Canvas, color: Int, stroke: Boolean = false, block: (Path) -> Unit) {
        val q = Path()
        block(q)
        fill(color, if (stroke) Paint.Style.STROKE else Paint.Style.FILL)
        c.drawPath(q, p)
    }

    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, color: Int) {
        fill(color); c.drawOval(x-rx, y-ry, x+rx, y+ry, p)
    }

    private fun line(c: Canvas, color: Int, width: Float, vararg xy: Float) {
        if (xy.size < 4) return
        fill(color, Paint.Style.STROKE); p.strokeWidth = width
        val q = Path(); q.moveTo(xy[0], xy[1])
        var i = 2
        while (i + 1 < xy.size) { q.lineTo(xy[i], xy[i+1]); i += 2 }
        c.drawPath(q, p)
    }

    private fun drawHairBack(c: Canvas, t: Float, sway: Float) {
        // Long twin side locks fall well below the waist.
        path(c, hairShade) {
            moveTo(67f, 74f); cubicTo(35f, 95f, 39f, 155f, 43f, 211f)
            cubicTo(44f, 239f, 31f, 258f, 40f, 270f)
            cubicTo(59f, 275f, 72f, 249f, 73f, 218f)
            cubicTo(78f, 177f, 79f, 116f, 92f, 89f); close()
        }
        path(c, hair) {
            moveTo(66f, 72f); cubicTo(38f, 87f, 46f, 145f, 48f, 206f)
            cubicTo(50f, 238f, 38f, 258f, 45f, 265f)
            cubicTo(59f, 264f, 67f, 240f, 68f, 210f)
            cubicTo(72f, 164f, 73f, 111f, 91f, 86f); close()
        }
        path(c, hairShade) {
            moveTo(166f, 74f); cubicTo(207f, 93f, 205f, 151f, 197f, 210f)
            cubicTo(192f, 240f, 209f, 258f, 197f, 270f)
            cubicTo(177f, 276f, 164f, 248f, 163f, 218f)
            cubicTo(158f, 174f, 160f, 112f, 145f, 88f); close()
        }
        path(c, hair) {
            moveTo(170f, 75f); cubicTo(201f, 94f, 195f, 153f, 190f, 208f)
            cubicTo(187f, 239f, 201f, 257f, 192f, 265f)
            cubicTo(178f, 264f, 170f, 239f, 168f, 209f)
            cubicTo(164f, 164f, 164f, 110f, 146f, 87f); close()
        }
        // Hair shine strands follow the swaying locks.
        path(c, Color.rgb(248, 250, 255), true) {
            moveTo(58f, 104f); cubicTo(49f, 147f, 60f, 203f, 53f, 246f)
        }
        path(c, Color.rgb(248, 250, 255), true) {
            moveTo(179f, 104f); cubicTo(190f, 150f, 178f, 205f, 188f, 247f)
        }
    }

    private fun drawBody(c: Canvas, t: Float) {
        val bodyY = 178f + sin(t*2f)*1.4f
        // legs and shoes
        path(c, Color.rgb(226, 230, 244)) {
            moveTo(99f, 224f); cubicTo(95f, 241f, 92f, 250f, 92f, 256f)
            cubicTo(92f, 264f, 116f, 265f, 119f, 257f)
            lineTo(117f, 225f); close()
        }
        path(c, Color.rgb(226, 230, 244)) {
            moveTo(128f, 225f); lineTo(126f, 257f)
            cubicTo(129f, 265f, 153f, 264f, 153f, 256f)
            cubicTo(152f, 246f, 148f, 236f, 145f, 224f); close()
        }
        oval(c, 105f, 258f, 17f, 7f, Color.rgb(75, 83, 119))
        oval(c, 140f, 258f, 17f, 7f, Color.rgb(75, 83, 119))
        // arms behind outfit
        path(c, skin) {
            moveTo(88f, bodyY+9f); cubicTo(78f, 187f, 80f, 209f, 91f, 213f)
            lineTo(101f, 203f); lineTo(98f, 187f); close()
        }
        path(c, skin) {
            moveTo(151f, 187f); lineTo(148f, 203f); lineTo(159f, 213f)
            cubicTo(171f, 208f, 168f, 187f, 159f, bodyY+9f); close()
        }
        // soft lavender sailor-style casual top
        path(c, Color.rgb(198, 211, 244)) {
            moveTo(99f, bodyY); cubicTo(108f, 171f, 136f, 171f, 147f, bodyY+2f)
            lineTo(157f, 222f); cubicTo(142f, 232f, 102f, 232f, 88f, 222f); close()
        }
        path(c, Color.rgb(239, 242, 255)) {
            moveTo(104f, bodyY+1f); lineTo(120f, 191f); lineTo(139f, bodyY+1f)
            lineTo(132f, 212f); lineTo(113f, 212f); close()
        }
        oval(c, 121f, 205f, 3f, 3f, eyeBlue)
        oval(c, 121f, 218f, 2.4f, 2.4f, Color.rgb(151, 171, 218))
    }

    private fun drawFace(c: Canvas, t: Float, blink: Boolean) {
        // face and ears
        oval(c, 60f, 123f, 10f, 15f, skin)
        oval(c, 180f, 123f, 10f, 15f, skin)
        oval(c, 120f, 119f, 61f, 67f, skin)
        // blush
        oval(c, 77f, 148f, 12f, 5f, Color.rgb(247, 169, 181))
        oval(c, 163f, 148f, 12f, 5f, Color.rgb(247, 169, 181))

        val mood = currentMood
        val closed = blink || mood == "sleepy"
        val angry = mood == "angry" || mood == "pouty"
        val surprised = mood == "surprised" || mood == "confused"
        val happy = mood == "happy" || mood == "joy" || mood == "affectionate" ||
            mood == "excited" || mood == "teasing" || mood == "talking" || speaking
        if (closed || happy) {
            path(c, ink, true) {
                moveTo(82f, 121f); cubicTo(91f, if (angry) 116f else 130f, 103f, if (angry) 116f else 130f, 111f, 121f)
            }
            path(c, ink, true) {
                moveTo(130f, 121f); cubicTo(140f, if (angry) 116f else 130f, 151f, if (angry) 116f else 130f, 160f, 121f)
            }
        } else {
            val eyeY = if (surprised) 124f else 123f
            oval(c, 96f, eyeY, if (surprised) 11f else 10f, if (surprised) 15f else 13f, Color.WHITE)
            oval(c, 145f, eyeY, if (surprised) 11f else 10f, if (surprised) 15f else 13f, Color.WHITE)
            oval(c, 97f, eyeY+2f, 7f, 10f, eyeBlue)
            oval(c, 146f, eyeY+2f, 7f, 10f, eyeBlue)
            oval(c, 98f, eyeY+1f, 3f, 5f, Color.rgb(29, 76, 149))
            oval(c, 147f, eyeY+1f, 3f, 5f, Color.rgb(29, 76, 149))
            oval(c, 94f, eyeY-4f, 3.2f, 3.2f, Color.WHITE)
            oval(c, 143f, eyeY-4f, 3.2f, 3.2f, Color.WHITE)
            if (mood == "excited" || mood == "enthusiastic") {
                // tiny star glints drawn inside the eyes
                path(c, Color.WHITE) {
                    moveTo(97f, 119f); lineTo(99f, 123f); lineTo(103f, 125f)
                    lineTo(99f, 127f); lineTo(97f, 131f); lineTo(95f, 127f)
                    lineTo(91f, 125f); lineTo(95f, 123f); close()
                }
                path(c, Color.WHITE) {
                    moveTo(146f, 119f); lineTo(148f, 123f); lineTo(152f, 125f)
                    lineTo(148f, 127f); lineTo(146f, 131f); lineTo(144f, 127f)
                    lineTo(140f, 125f); lineTo(144f, 123f); close()
                }
            }
        }
        // brows
        if (angry) {
            line(c, outline, 3f, 81f,108f, 108f,114f)
            line(c, outline, 3f, 132f,114f, 159f,108f)
        }
        // mouth varies with mood / speech
        when {
            surprised -> oval(c, 120f, 158f, 6f, 8f, Color.rgb(155, 75, 101))
            speaking || mood == "talking" -> {
                oval(c, 120f, 158f, 7f, 8f, Color.rgb(155, 75, 101))
                oval(c, 120f, 162f, 4f, 2f, Color.rgb(242, 145, 164))
            }
            happy -> path(c, Color.rgb(155, 75, 101)) {
                moveTo(108f, 155f); cubicTo(115f, 166f, 127f, 166f, 134f, 155f)
                cubicTo(132f, 172f, 110f, 172f, 108f, 155f); close()
            }
            angry || mood == "determined" -> path(c, Color.rgb(155, 75, 101), true) {
                moveTo(111f, 160f); lineTo(130f, 160f)
            }
            else -> path(c, Color.rgb(155, 75, 101), true) {
                moveTo(112f, 158f); cubicTo(120f, 164f, 128f, 158f, 130f, 157f)
            }
        }
    }

    private fun drawHairFront(c: Canvas, t: Float, sway: Float) {
        // rounded crown, long bangs, side fringe and ahoge
        path(c, hair) {
            moveTo(59f, 108f); cubicTo(47f, 67f, 67f, 35f, 105f, 34f)
            cubicTo(140f, 23f, 179f, 49f, 181f, 91f)
            cubicTo(177f, 107f, 170f, 112f, 164f, 115f)
            cubicTo(158f, 88f, 151f, 75f, 143f, 67f)
            cubicTo(137f, 91f, 127f, 105f, 117f, 110f)
            cubicTo(119f, 86f, 110f, 74f, 103f, 66f)
            cubicTo(97f, 90f, 83f, 105f, 72f, 111f); close()
        }
        path(c, hairShade, true) {
            moveTo(63f, 88f); cubicTo(74f, 42f, 145f, 28f, 170f, 78f)
        }
        path(c, Color.rgb(247, 249, 255), true) {
            moveTo(82f, 51f); cubicTo(97f, 38f, 111f, 37f, 123f, 42f)
        }
        path(c, hairShade) {
            moveTo(91f, 35f); cubicTo(76f, 14f, 94f, 3f, 110f, 9f)
            cubicTo(99f, 15f, 99f, 24f, 111f, 34f); close()
        }
        // side locks overlap cheeks to frame the face
        path(c, hair) {
            moveTo(61f, 78f); cubicTo(48f, 102f, 53f, 128f, 62f, 148f)
            cubicTo(66f, 128f, 69f, 109f, 77f, 93f); close()
        }
        path(c, hair) {
            moveTo(174f, 76f); cubicTo(190f, 102f, 185f, 128f, 175f, 150f)
            cubicTo(172f, 126f, 168f, 107f, 160f, 92f); close()
        }
        // a small understated flower clip
        oval(c, 160f, 58f, 6f, 6f, Color.rgb(177, 163, 222))
        oval(c, 168f, 58f, 6f, 6f, Color.rgb(177, 163, 222))
        oval(c, 164f, 52f, 6f, 6f, Color.rgb(195, 183, 233))
        oval(c, 164f, 64f, 6f, 6f, Color.rgb(195, 183, 233))
        oval(c, 164f, 58f, 3f, 3f, Color.rgb(244, 221, 160))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val now = System.nanoTime()
        val t = (now - startedAt) / 1_000_000_000f
        val moodAge = (now - moodChangedAt) / 1_000_000f
        val blinkPhase = (t % 4.1f)
        val blinking = blinkPhase > 3.82f && blinkPhase < 4.02f
        val breathe = 1f + sin(t * 1.65f) * 0.008f
        val bob = sin(t * 2.0f) * 2.2f
        val hairSway = sin(t * 1.25f) * 2.4f
        val moodPop = 1f + (1f - min(1f, moodAge / 240f)) * 0.018f

        val scale = min(width / 240f, height / 300f)
        val drawW = 240f * scale
        val drawH = 300f * scale
        val left = (width - drawW) / 2f
        val top = (height - drawH) / 2f

        canvas.save()
        canvas.translate(left, top + bob * scale)
        canvas.scale(scale, scale)
        canvas.translate(120f, 150f)
        canvas.scale(breathe * moodPop, breathe * moodPop)
        canvas.translate(-120f, -150f)

        // Hair behind the body and face, with gently moving lower tips.
        canvas.save()
        canvas.rotate(hairSway * 0.22f, 120f, 100f)
        drawHairBack(canvas, t, hairSway)
        canvas.restore()
        drawBody(canvas, t)
        drawFace(canvas, t, blinking)
        canvas.save()
        canvas.rotate(hairSway * 0.35f, 120f, 80f)
        drawHairFront(canvas, t, hairSway)
        canvas.restore()

        canvas.restore()
        // 60fps choreographed with Android's display vsync; no 50ms polling.
        postInvalidateOnAnimation()
    }
}
