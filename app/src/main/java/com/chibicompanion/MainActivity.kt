package com.chibicompanion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CompanionUi.bg
        window.navigationBarColor = CompanionUi.bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = CompanionUi.column(this, 22).apply { setBackgroundColor(CompanionUi.bg) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        val brand = CompanionUi.column(this).apply {
            addView(CompanionUi.text(this@MainActivity, "MIKO", 11f, CompanionUi.pink, true))
            addView(CompanionUi.text(this@MainActivity, "Your little AI companion ♡", 23f, CompanionUi.ink, true))
        }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.text(this, "✦", 27f, CompanionUi.lavender, true))
        root.addView(header)
        root.addView(CompanionUi.spacer(this, 18))

        val hero = CompanionUi.column(this, 12).apply {
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(255,235,243), Color.rgb(237,234,255), 30)
            gravity = Gravity.CENTER
        }
        hero.addView(CompanionUi.text(this, "MEET MIKO", 10f, CompanionUi.muted, true))
        val avatar = ChibiView(this)
        hero.addView(avatar, LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 280)))
        hero.addView(CompanionUi.text(this, "Ehehe! You're here! ✦", 16f, CompanionUi.ink, true))
        hero.addView(CompanionUi.text(this, "Your curious little companion.", 12f, CompanionUi.muted))
        root.addView(hero, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(CompanionUi.spacer(this, 18))

        root.addView(CompanionUi.text(this, "Make yourself at home", 18f, CompanionUi.ink, true))
        root.addView(CompanionUi.spacer(this, 10))
        root.addView(CompanionUi.button(this, "♡  Bring Miko to your screen", true) {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingCompanionService::class.java))
                Toast.makeText(this, "Miko is now floating ♡", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        root.addView(CompanionUi.spacer(this, 9))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(CompanionUi.button(this, "Chat & AI settings") {
            startActivity(Intent(this@MainActivity, ChatActivity::class.java))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val gap = View(this); row.addView(gap, LinearLayout.LayoutParams(CompanionUi.dp(this,8),1))
        row.addView(CompanionUi.button(this, "Send Miko away") {
            stopService(Intent(this@MainActivity, FloatingCompanionService::class.java))
            Toast.makeText(this, "Miko went to rest", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.text(this, "Drag Miko anywhere • double-tap to chat", 11f, CompanionUi.muted).apply { gravity = Gravity.CENTER })
        setContentView(root)
    }
}
