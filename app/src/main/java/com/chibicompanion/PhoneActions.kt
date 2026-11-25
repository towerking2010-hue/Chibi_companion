package com.chibicompanion

import android.content.Context
import android.content.Intent
import android.net.Uri

object PhoneActions {
    fun tryHandle(ctx:Context, raw:String):String? {
        val q=raw.trim()
        val lower=q.lowercase()
        val navigation = when {
            lower in listOf("go back", "press back", "back") -> "back"
            lower in listOf("go home", "home screen", "press home") -> "home"
            lower in listOf("open notifications", "show notifications") -> "notifications"
            lower in listOf("open quick settings", "show quick settings") -> "quick_settings"
            lower in listOf("scroll down", "swipe down") -> "scroll_down"
            lower in listOf("scroll up", "swipe up") -> "scroll_up"
            else -> null
        }
        if (navigation != null) {
            return "System-wide navigation control is not included in this safer build. You can use Android's Back, Home, notifications, and Quick Settings controls directly."
        }
        if (lower == "open folder" || lower == "choose folder" || lower.startsWith("browse folder")) {
            val i=Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return "Choose the folder you want me to open. Android will ask you to grant access first."
        }
        if (lower.startsWith("set alarm") || lower.startsWith("wake me")) {
            val time = Regex("(?:at|for)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(q)
                ?: return "Tell me the alarm time, for example: set alarm for 7:30 AM."
            var hour = time.groupValues[1].toIntOrNull() ?: return "I couldn't read that alarm time."
            val minute = time.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
            val meridiem = time.groupValues[3].lowercase()
            if (meridiem == "pm" && hour < 12) hour += 12
            if (meridiem == "am" && hour == 12) hour = 0
            if (hour !in 0..23 || minute !in 0..59) return "That doesn't look like a valid time."
            val i = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                .putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return "I opened your clock with that alarm ready. Please confirm and save it there. ♡"
        }
        if (lower.startsWith("set timer") || lower.startsWith("start timer")) {
            val seconds = Regex("(\\d+)\\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)", RegexOption.IGNORE_CASE).find(q)
                ?: return "Tell me a duration, like: set timer for 10 minutes."
            val amount = seconds.groupValues[1].toIntOrNull() ?: return "I couldn't read that duration."
            val unit = seconds.groupValues[2].lowercase()
            val total = amount * when {
                unit.startsWith("hour") || unit.startsWith("hr") -> 3600
                unit.startsWith("sec") -> 1
                else -> 60
            }
            val i = Intent(android.provider.AlarmClock.ACTION_SET_TIMER)
                .putExtra(android.provider.AlarmClock.EXTRA_LENGTH, total.coerceIn(1, 86400))
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return "Your timer is ready in the Clock app. Check it and start it there. ♡"
        }
        if (lower.startsWith("call ")) {
            val number = q.substringAfter("call ").trim()
            if (number.isBlank()) return "Tell me who or which number you want to call."
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return "I opened the dialer. Check the number and tap call when you're ready."
        }
        if (lower.startsWith("text ") || lower.startsWith("sms ")) {
            val rest = q.substringAfter(' ').trim()
            val number = rest.substringBefore(' ').trim()
            val body = rest.substringAfter(' ', "")
            if (number.isBlank()) return "Tell me the phone number to text."
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)))
                .putExtra("sms_body", body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            return "I opened a text draft. Review it and press send yourself."
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
