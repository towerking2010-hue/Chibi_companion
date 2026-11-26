package com.chibicompanion

import android.content.Intent
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.os.Bundle
import android.provider.Settings
import android.util.Log

class MikoVoiceInteractionService : VoiceInteractionService() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MikoVoiceInteraction", "VoiceInteractionService created")
    }
}

class MikoVoiceSessionService : VoiceInteractionSessionService() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MikoVoiceInteraction", "VoiceSessionService created in process: ${android.os.Process.myPid()}")
    }

    override fun onNewSession(args: Bundle?): VoiceInteractionSession = MikoVoiceSession(this)
}

private class MikoVoiceSession(service: android.content.Context) : VoiceInteractionSession(service) {
    override fun onCreate() {
        super.onCreate()
        Log.d("MikoVoiceInteraction", "VoiceSession created")
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d("MikoVoiceInteraction", "onShow called, flags=$showFlags, overlay=${Settings.canDrawOverlays(context)}")

        // Launch through Android's dedicated assistant-activity pathway.
        // Starting an overlay foreground service directly from the session can
        // be blocked by background-start restrictions on some Android builds.
        val destination = if (Settings.canDrawOverlays(context)) {
            MikoAssistActivity::class.java
        } else {
            MainActivity::class.java
        }
        startAssistantActivity(Intent(context, destination))
        finish()
    }

    override fun onHide() {
        super.onHide()
        Log.d("MikoVoiceInteraction", "onHide called")
    }
}