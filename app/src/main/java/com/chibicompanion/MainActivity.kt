package com.chibicompanion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
        }
        layout.addView(TextView(this).apply {
            text = "Chibi Companion"
            textSize = 28f
        })
        layout.addView(TextView(this).apply {
            text = "Your tiny silver-haired, blue-eyed tsundere companion.\n\nEnable overlay permission, then start her."
            textSize = 16f
            setPadding(0, 18, 0, 24)
        })
        val permission = Button(this).apply {
            text = "Grant display-over-other-apps permission"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                }
            }
        }
        layout.addView(permission)
        layout.addView(Button(this).apply {
            text = "Start floating companion"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, FloatingCompanionService::class.java))
                    Toast.makeText(this@MainActivity, "Companion started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Grant overlay permission first", Toast.LENGTH_LONG).show()
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Stop floating companion"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingCompanionService::class.java)) }
        })
        layout.addView(Button(this).apply {
            text = "Open chat and provider settings"
            setOnClickListener { startActivity(Intent(this, ChatActivity::class.java)) }
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}
