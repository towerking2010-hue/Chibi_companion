package com.chibicompanion

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object CompanionUi {
    val ink = Color.rgb(245, 242, 255)
    val muted = Color.rgb(171, 178, 207)
    val pink = Color.rgb(255, 105, 174)
    val lavender = Color.rgb(153, 119, 255)
    val bg = Color.rgb(15, 19, 39)
    val white = Color.rgb(30, 36, 65)

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    fun panel(c: Context, color: Int = white, radius: Int = 24): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(c, radius).toFloat() }
    fun gradient(c: Context, a: Int, b: Int, radius: Int = 28): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a,b)).apply { cornerRadius = dp(c,radius).toFloat() }
    fun text(c: Context, value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = value; textSize = size; setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.create("sans-serif", Typeface.NORMAL)
        }
    fun button(c: Context, value: String, primary: Boolean = false, onClick: () -> Unit): TextView =
        text(c, value, 15f, if (primary) white else ink, true).apply {
            gravity = Gravity.CENTER
            background = if (primary) gradient(c, pink, lavender, 18) else panel(c, Color.rgb(37, 43, 76), 18)
            setPadding(dp(c,16), dp(c,15), dp(c,16), dp(c,15))
            isClickable = true; isFocusable = true; setOnClickListener { onClick() }
        }
    fun spacer(c: Context, h: Int): View = View(c).apply { layoutParams = LinearLayout.LayoutParams(1, dp(c,h)) }
    fun column(c: Context, padding: Int = 0) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c,padding), dp(c,padding), dp(c,padding), dp(c,padding))
    }
}
