package com.chibicompanion

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Stable model-free viewport reserved for the owner's future animated Miko asset. */
class ModelSlotView(context: Context) : FrameLayout(context) {
    init {
        background = GradientDrawable().apply {
            setColor(Color.rgb(20, 27, 53))
            cornerRadius = CompanionUi.dp(context, 26).toFloat()
            setStroke(CompanionUi.dp(context, 1), Color.rgb(104, 112, 164))
        }
        contentDescription = "Reserved space for Miko's animated character model"
        val stack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(CompanionUi.dp(context, 20), CompanionUi.dp(context, 20), CompanionUi.dp(context, 20), CompanionUi.dp(context, 20))
        }
        stack.addView(TextView(context).apply { text = "✦"; textSize = 42f; setTextColor(Color.rgb(170,151,245)); gravity = Gravity.CENTER })
        stack.addView(TextView(context).apply {
            text = "MIKO'S MODEL SPACE"; textSize = 12f; letterSpacing = .16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.rgb(226,224,255)); gravity = Gravity.CENTER
        })
        stack.addView(TextView(context).apply {
            text = "Your animated Miko will live here"; textSize = 12f
            setTextColor(Color.rgb(155,166,203)); gravity = Gravity.CENTER
            setPadding(0, CompanionUi.dp(context,8), 0, 0)
        })
        addView(stack, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}