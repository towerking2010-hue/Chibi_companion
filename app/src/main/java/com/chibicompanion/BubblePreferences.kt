package com.chibicompanion

import android.content.Context

object BubblePreferences {
    private const val PREFS = "miko_bubble_preferences"
    private const val KEY_TIMEOUT = "timeout_ms"
    const val NEVER = 0L

    fun timeoutMs(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_TIMEOUT, 9000L)

    fun setTimeoutMs(context: Context, value: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_TIMEOUT, value.coerceAtLeast(0L)).apply()
    }

    fun label(value: Long): String = when (value) {
        0L -> "Until dismissed"
        5000L -> "5 seconds"
        15000L -> "15 seconds"
        else -> "9 seconds"
    }
}
