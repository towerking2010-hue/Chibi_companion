package com.chibicompanion

import android.content.Context

object ChatHistory {
    private const val PREF="chat_history"
    fun add(ctx:Context, role:String, message:String) {
        val p=ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        val old=p.getString("lines","") ?: ""
        val next=(old + "\n\n" + role + ": " + message).trim()
        p.edit().putString("lines", next.takeLast(14000)).apply()
    }
    fun clear(ctx: Context) { ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("lines").apply() }
    fun read(ctx:Context):String = ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("lines","")
        ?.takeIf { it.isNotBlank() } ?: "Her: Hmph! You finally came to see me?"
}
