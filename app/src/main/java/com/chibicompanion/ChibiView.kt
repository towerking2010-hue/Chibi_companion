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

    private fun path(c: Canvas, color: Int, stroke: Boolean = false, block: Path.() -> Unit) {
        val q = Path()
        q.block()
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
        // Layered cool shadows and fine strand ribbons give the long locks depth.
        path(c, Color.rgb(202, 215, 240), true) {
            moveTo(65f, 112f); cubicTo(58f, 154f, 69f, 207f, 59f, 250f)
        }
        path(c, Color.rgb(202, 215, 240), true) {
            moveTo(171f, 111f); cubicTo(181f, 153f, 168f, 207f, 180f, 250f)
        }
        path(c, Color.rgb(248, 250, 255), true) {
            moveTo(58f, 104f); cubicTo(49f, 147f, 60f, 203f, 53f, 246f)
        }
        path(c, Color.rgb(248, 250, 255), true) {
            moveTo(179f, 104f); cubicTo(190f, 150f, 178f, 205f, 188f, 247f)
        }
    }

    private fun drawBody(c: Canvas, t: Float) {
        val bodyY = 178f + sin(t * 2f) * 1.2f
        // Petite legs, soft socks and rounded flower boots.
        path(c, Color.rgb(255, 224, 211)) {
            moveTo(101f, 218f); cubicTo(98f, 231f, 96f, 245f, 97f, 254f)
            cubicTo(99f, 260f, 113f, 260f, 117f, 254f)
            lineTo(116f, 219f); close()
        }
        path(c, Color.rgb(255, 224, 211)) {
            moveTo(124f, 219f); lineTo(123f, 254f)
            cubicTo(127f, 260f, 141f, 260f, 143f, 254f)
            cubicTo(144f, 244f, 140f, 230f, 137f, 218f); close()
        }
        path(c, Color.rgb(242, 244, 255)) {
            moveTo(97f, 235f); cubicTo(103f, 240f, 111f, 240f, 117f, 235f)
            lineTo(118f, 256f); cubicTo(113f, 261f, 99f, 261f, 96f, 255f); close()
        }
        path(c, Color.rgb(242, 244, 255)) {
            moveTo(123f, 235f); cubicTo(130f, 240f, 138f, 240f, 144f, 235f)
            lineTo(145f, 255f); cubicTo(140f, 261f, 126f, 261f, 122f, 256f); close()
        }
        path(c, Color.rgb(191, 198, 231)) {
            moveTo(94f, 251f); cubicTo(99f, 245f, 115f, 246f, 120f, 253f)
            lineTo(120f, 267f); cubicTo(116f, 275f, 94f, 275f, 91f, 265f); close()
        }
        path(c, Color.rgb(191, 198, 231)) {
            moveTo(120f, 253f); cubicTo(126f, 246f, 142f, 245f, 147f, 251f)
            lineTo(150f, 265f); cubicTo(147f, 275f, 125f, 275f, 120f, 267f); close()
        }
        path(c, Color.rgb(249, 249, 255), true) {
            moveTo(97f, 255f); cubicTo(103f, 251f, 111f, 251f, 116f, 256f)
            moveTo(124f, 256f); cubicTo(130f, 251f, 138f, 251f, 144f, 255f)
        }
        // Tiny flower details on each boot.
        for (cx in listOf(105f, 136f)) {
            oval(c, cx - 4f, 261f, 3.5f, 4.5f, Color.rgb(179, 165, 220))
            oval(c, cx + 4f, 261f, 3.5f, 4.5f, Color.rgb(179, 165, 220))
            oval(c, cx, 256f, 3.5f, 4.5f, Color.rgb(202, 190, 237))
            oval(c, cx, 266f, 3.5f, 4.5f, Color.rgb(202, 190, 237))
            oval(c, cx, 261f, 2f, 2f, Color.rgb(250, 225, 173))
        }
        // Arms and rounded puff sleeves.
        path(c, skin) {
            moveTo(91f, 183f); cubicTo(79f, 191f, 79f, 207f, 88f, 215f)
            cubicTo(94f, 219f, 101f, 211f, 99f, 204f); close()
        }
        path(c, skin) {
            moveTo(149f, 183f); cubicTo(161f, 191f, 161f, 207f, 152f, 215f)
            cubicTo(146f, 219f, 139f, 211f, 141f, 204f); close()
        }
        path(c, Color.rgb(246, 245, 255)) {
            moveTo(91f, 174f); cubicTo(77f, 176f, 76f, 197f, 87f, 207f)
            cubicTo(94f, 212f, 103f, 205f, 103f, 194f)
            lineTo(101f, 181f); close()
        }
        path(c, Color.rgb(246, 245, 255)) {
            moveTo(149f, 174f); cubicTo(163f, 176f, 164f, 197f, 153f, 207f)
            cubicTo(146f, 212f, 137f, 205f, 137f, 194f)
            lineTo(139f, 181f); close()
        }
        // Midnight-navy pinafore and pleated skirt.
        path(c, Color.rgb(49, 58, 101)) {
            moveTo(99f, 178f); cubicTo(108f, 170f, 132f, 170f, 141f, 178f)
            lineTo(154f, 225f); cubicTo(143f, 235f, 97f, 235f, 86f, 225f); close()
        }
        path(c, Color.rgb(238, 239, 255)) {
            moveTo(94f, 219f); cubicTo(108f, 226f, 132f, 226f, 146f, 219f)
            lineTo(153f, 233f); cubicTo(139f, 244f, 101f, 244f, 87f, 233f); close()
        }
        path(c, Color.rgb(160, 169, 215), true) {
            moveTo(94f, 222f); lineTo(89f, 233f); moveTo(106f, 226f); lineTo(104f, 241f)
            moveTo(120f, 227f); lineTo(120f, 243f); moveTo(134f, 226f); lineTo(136f, 241f)
            moveTo(146f, 222f); lineTo(151f, 233f)
        }
        // White blouse, sailor collar and lavender bow.
        path(c, Color.rgb(249, 248, 255)) {
            moveTo(104f, 174f); lineTo(120f, 191f); lineTo(136f, 174f)
            lineTo(141f, 211f); lineTo(120f, 222f); lineTo(99f, 211f); close()
        }
        path(c, Color.rgb(202, 193, 238)) {
            moveTo(104f, 175f); lineTo(120f, 190f); lineTo(136f, 175f)
            lineTo(129f, 197f); lineTo(120f, 204f); lineTo(111f, 197f); close()
        }
        path(c, Color.rgb(171, 153, 222)) {
            moveTo(112f, 192f); lineTo(120f, 198f); lineTo(128f, 192f)
            lineTo(126f, 207f); lineTo(120f, 212f); lineTo(114f, 207f); close()
        }
        oval(c, 120f, 198f, 2.8f, 2.8f, Color.rgb(255, 229, 183))
        // Jacket edging, buttons, cuffs and fine tailoring.
        path(c, Color.rgb(247, 248, 255), true) {
            moveTo(96f, 181f); cubicTo(91f, 198f, 91f, 216f, 96f, 229f)
            moveTo(144f, 181f); cubicTo(149f, 198f, 149f, 216f, 144f, 229f)
        }
        line(c, Color.rgb(185, 174, 225), 1.5f, 88f, 203f, 96f, 208f)
        line(c, Color.rgb(185, 174, 225), 1.5f, 152f, 203f, 144f, 208f)
        oval(c, 101f, 211f, 2f, 2f, Color.rgb(226, 211, 170))
        oval(c, 139f, 211f, 2f, 2f, Color.rgb(226, 211, 170))
        // Delicate lace-like sleeve trim.
        path(c, Color.rgb(205, 194, 238), true) {
            moveTo(83f, 201f); cubicTo(88f, 206f, 94f, 207f, 99f, 202f)
            moveTo(141f, 202f); cubicTo(146f, 207f, 152f, 206f, 157f, 201f)
        }
    }

    private fun drawFace(c: Canvas, t: Float, blink: Boolean) {
        // face and ears
        oval(c, 60f, 123f, 10f, 15f, skin)
        oval(c, 180f, 123f, 10f, 15f, skin)
        oval(c, 120f, 119f, 61f, 67f, skin)
        // blush
        val blush = if (currentMood == "shy" || currentMood == "embarrassed") Color.rgb(255, 132, 169) else Color.rgb(247, 169, 181)
        oval(c, 77f, 148f, 12f, 5f, blush)
        oval(c, 163f, 148f, 12f, 5f, blush)

        val mood = currentMood
        val closed = blink || mood == "sleepy"
        val angry = mood == "angry" || mood == "pouty"
        val surprised = mood == "surprised" || mood == "confused"
        val sad = mood == "sad" || mood == "crying"
        val shy = mood == "shy" || mood == "embarrassed"
        val starEyes = mood == "excited" || mood == "enthusiastic" || mood == "happy" || mood == "joy"
        val heartEyes = mood == "affectionate" || mood == "love"
        val happy = starEyes || heartEyes || mood == "teasing" || mood == "talking" || speaking
        if (closed) {
            path(c, ink, true) {
                moveTo(80f, 122f); cubicTo(90f, if (angry) 116f else 132f, 103f, if (angry) 116f else 132f, 112f, 121f)
            }
            path(c, ink, true) {
                moveTo(128f, 121f); cubicTo(138f, if (angry) 116f else 132f, 151f, if (angry) 116f else 132f, 161f, 122f)
            }
            line(c, ink, 1.5f, 84f, 121f, 80f, 117f)
            line(c, ink, 1.5f, 157f, 121f, 161f, 117f)
        } else {
            val eyeY = if (surprised) 124f else 123f
            val eyeRx = if (surprised) 11.5f else 10.5f
            val eyeRy = if (surprised) 15.5f else 14f
            // Soft lower iris rim, deep sapphire iris and layered catchlights.
            oval(c, 96f, eyeY, eyeRx, eyeRy, Color.WHITE)
            oval(c, 145f, eyeY, eyeRx, eyeRy, Color.WHITE)
            oval(c, 97f, eyeY+3f, 7.8f, 10.8f, Color.rgb(35, 105, 194))
            oval(c, 146f, eyeY+3f, 7.8f, 10.8f, Color.rgb(35, 105, 194))
            oval(c, 97f, eyeY+5f, 5.1f, 7.2f, Color.rgb(19, 59, 126))
            oval(c, 146f, eyeY+5f, 5.1f, 7.2f, Color.rgb(19, 59, 126))
            oval(c, 94f, eyeY-4.5f, 3.7f, 4.2f, Color.WHITE)
            oval(c, 143f, eyeY-4.5f, 3.7f, 4.2f, Color.WHITE)
            oval(c, 100f, eyeY+8f, 1.8f, 2.2f, Color.rgb(164, 215, 255))
            oval(c, 149f, eyeY+8f, 1.8f, 2.2f, Color.rgb(164, 215, 255))
            // Upper eyeliner and outer lashes.
            path(c, ink, true) {
                moveTo(85f, 117f); cubicTo(91f, 106f, 103f, 107f, 108f, 116f)
            }
            path(c, ink, true) {
                moveTo(133f, 116f); cubicTo(139f, 107f, 151f, 106f, 157f, 117f)
            }
            line(c, ink, 1.7f, 88f, 112f, 83f, 108f)
            line(c, ink, 1.7f, 154f, 112f, 159f, 108f)
            if (starEyes) {
                path(c, Color.WHITE) {
                    moveTo(97f, 117f); lineTo(100f, 122f); lineTo(105f, 124f)
                    lineTo(100f, 127f); lineTo(97f, 132f); lineTo(94f, 127f)
                    lineTo(89f, 124f); lineTo(94f, 122f); close()
                }
                path(c, Color.WHITE) {
                    moveTo(146f, 117f); lineTo(149f, 122f); lineTo(154f, 124f)
                    lineTo(149f, 127f); lineTo(146f, 132f); lineTo(143f, 127f)
                    lineTo(138f, 124f); lineTo(143f, 122f); close()
                }
            } else if (heartEyes) {
                path(c, Color.rgb(255, 169, 205)) {
                    moveTo(97f, 130f); cubicTo(82f, 121f, 91f, 112f, 97f, 120f)
                    cubicTo(104f, 112f, 112f, 121f, 97f, 130f); close()
                }
                path(c, Color.rgb(255, 169, 205)) {
                    moveTo(146f, 130f); cubicTo(131f, 121f, 140f, 112f, 146f, 120f)
                    cubicTo(153f, 112f, 161f, 121f, 146f, 130f); close()
                }
            }
            if (sad) {
                path(c, Color.rgb(104, 190, 255)) {
                    moveTo(88f, 133f); cubicTo(82f, 144f, 86f, 150f, 91f, 145f); close()
                }
                path(c, Color.rgb(104, 190, 255)) {
                    moveTo(155f, 133f); cubicTo(149f, 144f, 153f, 150f, 158f, 145f); close()
                }
            }
        }
        // brows
        if (angry) {
            line(c, outline, 3.2f, 81f,108f, 108f,114f)
            line(c, outline, 3.2f, 132f,114f, 159f,108f)
        } else if (sad) {
            line(c, outline, 2.6f, 82f,112f, 106f,108f)
            line(c, outline, 2.6f, 134f,108f, 158f,112f)
        } else if (surprised) {
            line(c, outline, 2.4f, 84f,105f, 105f,103f)
            line(c, outline, 2.4f, 135f,103f, 156f,105f)
        }
        // mouth varies with mood / speech
        when {
            surprised -> oval(c, 120f, 158f, 6f, 8f, Color.rgb(155, 75, 101))
            speaking || mood == "talking" -> {
                val mouthOpen = 4.5f + abs(sin(t * 13f)) * 4.5f
                oval(c, 120f, 159f, 6.5f, mouthOpen, Color.rgb(139, 55, 91))
                oval(c, 120f, 162f, 3.8f, 1.8f, Color.rgb(242, 145, 164))
            }
            happy -> path(c, Color.rgb(155, 75, 101)) {
                moveTo(108f, 155f); cubicTo(115f, 166f, 127f, 166f, 134f, 155f)
                cubicTo(132f, 172f, 110f, 172f, 108f, 155f); close()
            }
            angry || mood == "determined" || mood == "pouty" -> path(c, Color.rgb(155, 75, 101), true) {
                moveTo(111f, 160f); cubicTo(117f, 157f, 124f, 157f, 130f, 160f)
            }
            sad -> path(c, Color.rgb(155, 75, 101), true) {
                moveTo(110f, 164f); cubicTo(116f, 155f, 126f, 155f, 132f, 164f)
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
        // Fine individual bang strands and soft crown highlights.
        path(c, Color.rgb(255, 255, 255), true) {
            moveTo(91f, 48f); cubicTo(83f, 66f, 82f, 80f, 77f, 91f)
        }
        path(c, Color.rgb(194, 207, 235), true) {
            moveTo(111f, 42f); cubicTo(124f, 56f, 126f, 77f, 119f, 96f)
        }
        path(c, Color.rgb(255, 255, 255), true) {
            moveTo(151f, 54f); cubicTo(164f, 67f, 168f, 82f, 169f, 96f)
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
        // Signature five-petal lavender flower clip, matching the approved reference.
        oval(c, 157f, 57f, 5.2f, 7f, Color.rgb(157, 172, 224))
        oval(c, 167f, 57f, 5.2f, 7f, Color.rgb(157, 172, 224))
        oval(c, 162f, 49f, 5.2f, 7f, Color.rgb(177, 190, 235))
        oval(c, 162f, 65f, 5.2f, 7f, Color.rgb(177, 190, 235))
        oval(c, 170f, 64f, 5.2f, 6f, Color.rgb(157, 172, 224))
        oval(c, 162f, 57f, 3.2f, 3.2f, Color.rgb(244, 183, 153))
        // Fine flowing outer strands give the long hair a layered silhouette.
        path(c, Color.rgb(195, 207, 234), true) {
            moveTo(53f, 116f); cubicTo(43f, 163f, 51f, 209f, 43f, 246f)
        }
        path(c, Color.rgb(250, 251, 255), true) {
            moveTo(187f, 115f); cubicTo(198f, 164f, 189f, 211f, 199f, 245f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val now = System.nanoTime()
        val t = (now - startedAt) / 1_000_000_000f
        val moodAge = (now - moodChangedAt) / 1_000_000f
        // Keep the greeting visibly awake: short natural blinks, with a longer
        // open-eye interval so a freshly opened home screen never looks sleepy.
        val blinkPhase = (t % 5.2f)
        val blinking = blinkPhase > 4.98f && blinkPhase < 5.08f
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
