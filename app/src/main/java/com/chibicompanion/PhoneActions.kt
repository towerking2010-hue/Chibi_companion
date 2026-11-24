package com.chibicompanion

import android.content.Context
import android.content.Intent
import android.net.Uri

object PhoneActions {
    fun tryHandle(ctx:Context, raw:String):String? {
        val q=raw.trim()
        val lower=q.lowercase()
        if (lower == "open folder" || lower == "choose folder" || lower.startsWith("browse folder")) {
            val i=Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return "Choose the folder you want me to open. Android will ask you to grant access first."
        }
        if (lower.startsWith("search ") || lower.startsWith("google ")) {
            val term=q.substringAfter(' ')
            val i=Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q="+Uri.encode(term))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i); return "There! I opened a search for $term. Don't get distracted, okay?"
        }
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val name=q.substringAfter(' ')
            val pm=ctx.packageManager
            val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val matches=pm.queryIntentActivities(launcher,0)
            val app=matches.firstOrNull { it.loadLabel(pm).toString().contains(name,true) }
            if(app!=null) {
                val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(app.activityInfo.packageName, app.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent); return "Opening ${app.loadLabel(pm)}. I knew that already, baka."
            }
            if(name.equals("settings",true)) {
                ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return "Settings, coming right up."
            }
            return "I couldn't find an app named $name. Check its name and try again."
        }
        return null
    }
}
