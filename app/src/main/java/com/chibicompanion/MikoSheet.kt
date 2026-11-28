package com.chibicompanion

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Compact, non-blocking-feeling bottom sheet used for contextual help and confirmations. */
object MikoSheet {
    fun show(
        context: Context,
        eyebrow: String,
        title: String,
        message: String,
        steps: List<String> = emptyList(),
        actionLabel: String = "Got it",
        onAction: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 24), dp(context, 12), dp(context, 24), dp(context, 26))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(dp(context, 26).toFloat(), dp(context, 26).toFloat(), dp(context, 26).toFloat(), dp(context, 26).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        val handle = View(context).apply {
            background = GradientDrawable().apply { setColor(Color.rgb(220, 214, 229)); cornerRadius = dp(context, 8).toFloat() }
        }
        content.addView(handle, LinearLayout.LayoutParams(dp(context, 38), dp(context, 4)).apply {
            gravity = Gravity.CENTER
            bottomMargin = dp(context, 22)
        })
        content.addView(label(context, eyebrow.uppercase(), 10f, CompanionUi.lavender, true).apply {
            letterSpacing = .13f
            bottomPadding = dp(context, 8)
        })
        content.addView(label(context, title, 23f, CompanionUi.ink, true).apply {
            bottomPadding = dp(context, 10)
        })
        content.addView(label(context, message, 15f, CompanionUi.muted, false).apply {
            bottomPadding = dp(context, if (steps.isEmpty()) 22 else 14)
            setLineSpacing(dp(context, 3).toFloat(), 1f)
        })
        steps.forEachIndexed { index, step ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                setPadding(0, dp(context, 7), 0, dp(context, 7))
            }
            val number = label(context, (index + 1).toString().padStart(2, '0'), 12f, CompanionUi.lavender, true).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.rgb(244, 239, 251))
                    cornerRadius = dp(context, 10).toFloat()
                }
            }
            row.addView(number, LinearLayout.LayoutParams(dp(context, 34), dp(context, 34)).apply { rightMargin = dp(context, 12) })
            row.addView(label(context, step, 14f, CompanionUi.ink, false), LinearLayout.LayoutParams(0, -2, 1f))
            content.addView(row)
        }
        val button = TextView(context).apply {
            text = actionLabel
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(CompanionUi.pink, CompanionUi.lavender)).apply { cornerRadius = dp(context, 16).toFloat() }
            minHeight = dp(context, 50)
            setOnClickListener { dialog.dismiss(); onAction?.invoke() }
        }
        content.addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 12) })
        dialog.setContentView(ScrollView(context).apply { isFillViewport = true; addView(content) })
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.BOTTOM)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { dimAmount = .34f; windowAnimations = android.R.style.Animation_InputMethod }
            }
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = .34f; softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
        }
    }

    private fun label(context: Context, value: String, size: Float, color: Int, bold: Boolean) =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density).toInt()

    private var TextView.bottomPadding: Int
        get() = paddingBottom
        set(value) = setPadding(paddingLeft, paddingTop, paddingRight, value)
}
