package com.chibicompanion

import android.content.Context

object AssistantPreferences {
    private const val PREF = "assistant_preferences"
    const val QUIET = "quiet"
    const val OPTIONAL = "optional"
    const val ACTIVE = "active"

    fun proactiveMode(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("proactive_mode", OPTIONAL) ?: OPTIONAL

    fun setProactiveMode(ctx: Context, mode: String) {
        val safe = if (mode in listOf(QUIET, OPTIONAL, ACTIVE)) mode else OPTIONAL
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("proactive_mode", safe).apply()
    }

    fun screenAwarenessEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("screen_awareness", false)

    fun setScreenAwarenessEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("screen_awareness", enabled).apply()
    }
}
