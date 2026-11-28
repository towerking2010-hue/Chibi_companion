package com.chibicompanion

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shared visual system for Miko's native Android screens.
 * Kept dependency-free so the app stays light on mid-range devices.
 */
object CompanionUi {
    val ink = Color.rgb(61, 57, 80)
    val muted = Color.rgb(135, 132, 151)
    val pink = Color.rgb(237, 151, 187)
    val lavender = Color.rgb(181, 164, 226)
    val cyan = Color.rgb(132, 190, 218)
    val bg = Color.rgb(250, 248, 253)
    val white = Color.rgb(255, 255, 255)
    val surface = Color.rgb(248, 245, 252)
    val surfaceRaised = Color.rgb(243, 239, 249)
    val stroke = Color.rgb(237, 231, 244)
    val success = Color.rgb(75, 154, 123)

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun panel(c: Context, color: Int = white, radius: Int = 22): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(c, radius).toFloat()
            setStroke(dp(c, 1), stroke)
        }

    fun gradient(c: Context, a: Int, b: Int, radius: Int = 24): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply {
            cornerRadius = dp(c, radius).toFloat()
        }

    fun text(c: Context, value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.NORMAL)
                else Typeface.create("sans-serif", Typeface.NORMAL)
        }

    fun eyebrow(c: Context, value: String): TextView =
        text(c, value.uppercase(), 10f, lavender, true).apply { letterSpacing = .12f }

    fun heading(c: Context, value: String, size: Float = 25f): TextView =
        text(c, value, size, ink, true)

    fun button(c: Context, value: String, primary: Boolean = false, onClick: () -> Unit): TextView =
        text(c, value, 14f, if (primary) Color.WHITE else ink, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(c, 48)
            background = if (primary) gradient(c, pink, lavender, 16)
                else panel(c, surfaceRaised, 16)
            setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12))
            isClickable = true
            isFocusable = true
            elevation = dp(c, if (primary) 2 else 0).toFloat()
            setOnClickListener { onClick() }
        }

    fun card(c: Context, padding: Int = 16, color: Int = surface): LinearLayout =
        column(c, padding).apply {
            background = panel(c, color, 20)
        }

    fun spacer(c: Context, h: Int): View =
        View(c).apply { layoutParams = LinearLayout.LayoutParams(1, dp(c, h)) }

    fun column(c: Context, padding: Int = 0) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c, padding), dp(c, padding), dp(c, padding), dp(c, padding))
    }

    fun row(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun applySystemBars(activity: android.app.Activity) {
        activity.window.statusBarColor = bg
        activity.window.navigationBarColor = bg
        activity.window.decorView.systemUiVisibility = 0
    }
}
