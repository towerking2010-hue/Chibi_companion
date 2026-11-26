package com.chibicompanion

import android.content.Intent
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.os.Bundle
import android.provider.Settings

class MikoVoiceInteractionService : VoiceInteractionService()

class MikoVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = MikoVoiceSession(this)
}

private class MikoVoiceSession(service: android.content.Context) : VoiceInteractionSession(service) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

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
}
