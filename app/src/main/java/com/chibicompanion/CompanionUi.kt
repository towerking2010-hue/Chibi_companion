package com.chibicompanion

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Shared high-contrast midnight/neon visual system. Native and lightweight for A31. */
object CompanionUi {
    val ink = Color.rgb(245, 247, 255)
    val muted = Color.rgb(169, 184, 222)
    val pink = Color.rgb(255, 76, 180)
    val lavender = Color.rgb(139, 105, 255)
    val cyan = Color.rgb(40, 205, 255)
    val bg = Color.rgb(8, 12, 31)
    val white = Color.rgb(22, 29, 61)
    val surface = Color.rgb(18, 25, 55)
    val surfaceRaised = Color.rgb(25, 34, 72)
    val stroke = Color.rgb(53, 72, 130)
    val success = Color.rgb(80, 235, 185)

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
            setStroke(dp(c, 1), Color.rgb(75, 104, 190))
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
        text(c, value.uppercase(), 10f, cyan, true).apply { letterSpacing = .12f }

    fun heading(c: Context, value: String, size: Float = 25f): TextView =
        text(c, value, size, ink, true)

    fun button(c: Context, value: String, primary: Boolean = false, onClick: () -> Unit): TextView =
        text(c, value, 14f, if (primary) Color.WHITE else ink, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(c, 56)
            background = if (primary) gradient(c, Color.rgb(32, 125, 255), Color.rgb(139, 65, 255), 16)
                else panel(c, surfaceRaised, 16)
            setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16))
            isClickable = true
            isFocusable = true
            elevation = dp(c, if (primary) 4 else 0).toFloat()
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> view.animate().scaleX(.985f).scaleY(.985f).setDuration(90).start()
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
                false
            }
            setOnClickListener { onClick() }
        }

    fun card(c: Context, padding: Int = 16, color: Int = surface): LinearLayout =
        column(c, padding).apply {
            background = panel(c, color, 20)
            elevation = dp(c, 3).toFloat()
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
