package com.chibicompanion

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.Toast

/**
 * Android ACTION_ASSIST entry point. This also makes Miko eligible for the
 * assistant role on devices that enumerate ACTION_ASSIST handlers.
 */
class MikoAssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) {
            val service = Intent(this, FloatingCompanionService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
        } else {
            Toast.makeText(this, "Allow Miko to display over other apps to summon her.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}
