package com.chibicompanion

import android.content.Intent
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.os.Bundle
import android.os.Build
import android.provider.Settings

class MikoVoiceInteractionService : VoiceInteractionService()

class MikoVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = MikoVoiceSession(this)
}

private class MikoVoiceSession(service: android.content.Context) : VoiceInteractionSession(service) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (!Settings.canDrawOverlays(context)) {
            val setup = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(setup)
            finish()
            return
        }
        val intent = Intent(context, FloatingCompanionService::class.java)
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        finish()
    }
}
