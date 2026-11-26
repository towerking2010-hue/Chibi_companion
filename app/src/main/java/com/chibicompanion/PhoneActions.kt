package com.chibicompanion

import android.content.Context
import android.content.Intent
import android.net.Uri

object PhoneActions {
    fun tryHandle(ctx:Context, raw:String):String? {
        return try {
        val q=raw.trim()
        val lower=q.lowercase()
        if (lower in listOf("show notes", "my notes", "open notes", "show my notes", "notes")) {
            ctx.startActivity(Intent(ctx, NotesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return "Here are your private notes. Keep all your brilliant ideas in one place! ♡"
        }
        if (lower in listOf("show tasks", "my tasks", "open tasks", "show my tasks", "task list")) {
            ctx.startActivity(Intent(ctx, TasksActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return "Here is your task list. Let's get things done! ♡"
        }
        if (lower.startsWith("add task ") || lower.startsWith("remember task ") || lower.startsWith("todo ")) {
            val title = when {
                lower.startsWith("add task ") -> q.substring("add task ".length).trim()
                lower.startsWith("remember task ") -> q.substring("remember task ".length).trim()
                else -> q.substring("todo ".length).trim()
            }
            if (title.isBlank()) return "What should I add to your task list?"
            TaskStore.add(ctx, title)
            return "Added '$title' to your task list. One step at a time! ♡"
        }
        if (lower == "what time is it" || lower == "current time" || lower == "tell me the time") {
            val now = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            return "It's $now."
        }
        if (lower == "what's the date" || lower == "what is the date" || lower == "today's date") {
            val today = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            return "Today is $today."
        }
        if (lower.startsWith("directions to ") || lower.startsWith("navigate to ") || lower.startsWith("take me to ")) {
            val destination = q.substringAfter(" to ", "").trim()
            if (destination.isBlank()) return "Where would you like to go?"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(destination)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                ctx.startActivity(mapIntent)
                "I opened navigation for $destination. Check the route before you start."
            } catch (_: Exception) {
                val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(destination)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(web)
                "I opened Maps search for $destination."
            }
        }
        if (lower.startsWith("create event ") || lower.startsWith("add calendar event ")) {
            val title = if (lower.startsWith("create event ")) q.substring("create event ".length).trim()
                else q.substring("add calendar event ".length).trim()
            if (title.isBlank()) return "Tell me the event title."
            val event = Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)
                .putExtra(android.provider.CalendarContract.Events.TITLE, title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(event)
            return "I opened a new calendar event titled '$title'. Add the date and time, then save it."
        }
        if (lower.startsWith("email ")) {
            val recipient = q.substringAfter("email ").trim()
            if (recipient.isBlank()) return "Who should I address the email to?"
            val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(recipient)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(mail)
            return "I opened an email draft. Review the recipient and message before sending."
        }
        val settingsAction = when {
            lower in listOf("open wifi settings", "wifi settings", "open wi-fi settings") -> android.provider.Settings.ACTION_WIFI_SETTINGS
            lower in listOf("open bluetooth settings", "bluetooth settings") -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            lower in listOf("open display settings", "display settings", "brightness settings") -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
            lower in listOf("open sound settings", "sound settings", "volume settings") -> android.provider.Settings.ACTION_SOUND_SETTINGS
            lower in listOf("open battery settings", "battery settings") -> android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            lower in listOf("open location settings", "location settings", "gps settings") -> android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            lower in listOf("open accessibility settings", "accessibility settings") -> android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> null
        }
        if (settingsAction != null) {
            ctx.startActivity(Intent(settingsAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return "I opened the relevant Android settings. You stay in control of the change."
        }
        if (lower in listOf("open camera", "launch camera", "take a photo", "take photo")) {
            return try {
                ctx.startActivity(Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Camera is open. You can frame and take the photo."
            } catch (_: Exception) { "I couldn't find a camera app on this phone." }
        }
        if (lower.startsWith("remind me to ")) {
            val match = Regex("^remind me to (.+?) in (\\d+)\\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)$", RegexOption.IGNORE_CASE).find(q)
            if (match != null) {
                val task = match.groupValues[1].trim()
                val amount = match.groupValues[2].toLongOrNull() ?: return "I couldn't read that reminder delay."
                val unit = match.groupValues[3].lowercase()
                val multiplier = when {
                    unit.startsWith("hour") || unit.startsWith("hr") -> 3_600_000L
                    unit.startsWith("sec") -> 1_000L
                    else -> 60_000L
                }
                val delay = runCatching { Math.multiplyExact(amount, multiplier) }.getOrNull()
                    ?: return "That reminder is too far in the future."
                if (task.isBlank() || delay !in 1_000L..(30L * 24 * 60 * 60 * 1000)) return "Choose a reminder between 1 second and 30 days from now."
                ReminderScheduler.schedule(ctx, task, delay)
                val duration = when {
                    amount == 1L && unit.startsWith("hour") -> "1 hour"
                    unit.startsWith("hour") || unit.startsWith("hr") -> "$amount hours"
                    amount == 1L && unit.startsWith("min") -> "1 minute"
                    unit.startsWith("min") -> "$amount minutes"
                    amount == 1L -> "1 second"
                    else -> "$amount seconds"
                }
                return "Okay! I'll remind you about '$task' in $duration. ♡"
            }
            val task = q.substringAfter("remind me to ").trim()
            if (task.isBlank()) return "What should I remind you about?"
            val event = Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)
                .putExtra(android.provider.CalendarContract.Events.TITLE, "Reminder: $task")
                .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Created with Miko")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(event)
            return "I opened a calendar reminder draft for '$task'. Choose its date and time, then save it."
        }
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
            val name=q.substringAfter(' ').trim()
            if (name.isBlank()) return "Which app should I open?"
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
        null
        } catch (e: Exception) {
            "I could not complete that action. Android may not have a compatible app or permission ready."
        }
    }
}
