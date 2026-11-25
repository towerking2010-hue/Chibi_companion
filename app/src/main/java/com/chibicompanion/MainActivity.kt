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

        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(CompanionUi.bg) }
        val root = CompanionUi.column(this, 18)
        scroll.addView(root)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        val brand = CompanionUi.column(this)
        brand.addView(CompanionUi.text(this, "MIKO  ✦  COMPANION", 10f, CompanionUi.pink, true))
        brand.addView(CompanionUi.text(this, "Your little world, together.", 22f, CompanionUi.ink, true))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.text(this, "♡", 30f, CompanionUi.lavender, true))
        root.addView(header)
        root.addView(CompanionUi.spacer(this, 10))

        val hero = CompanionUi.column(this, 12).apply {
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(255,235,245), Color.rgb(230,235,255), 30)
            gravity = Gravity.CENTER
        }
        hero.addView(CompanionUi.text(this, "YOUR CHIBI COMPANION", 10f, CompanionUi.muted, true))
        val avatar = MikoSpriteView(this).apply {
            setMood("excited")
            setOnClickListener { setInteraction("tap") }
            setOnLongClickListener { setInteraction("long_tap"); true }
        }
        hero.addView(avatar, LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 300)))
        hero.addView(CompanionUi.text(this, "Ehehe! You're here! ✦", 19f, CompanionUi.ink, true))
        hero.addView(CompanionUi.text(this, "A tiny friend with a big personality.", 12f, CompanionUi.muted))
        root.addView(hero, LinearLayout.LayoutParams(-1, -2))
        root.addView(CompanionUi.spacer(this, 18))

        root.addView(CompanionUi.text(this, "Spend a little time together", 18f, CompanionUi.ink, true))
        root.addView(CompanionUi.spacer(this, 9))
        root.addView(CompanionUi.button(this, "♡  Chat with Miko", true) {
            startActivity(Intent(this@MainActivity, ChatActivity::class.java))
        })
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "⚙  Miko settings") {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        })
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "✦  Bring Miko to your screen") {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingCompanionService::class.java))
                Toast.makeText(this, "Miko is now floating ♡", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "Let Miko rest") {
            stopService(Intent(this@MainActivity, FloatingCompanionService::class.java))
            Toast.makeText(this, "Miko went to rest", Toast.LENGTH_SHORT).show()
        })
        root.addView(CompanionUi.spacer(this, 14))

        val note = CompanionUi.column(this, 14).apply {
            background = CompanionUi.panel(this@MainActivity, Color.rgb(244,241,251), 22)
        }
        note.addView(CompanionUi.text(this, "A little note ♡", 14f, CompanionUi.ink, true))
        note.addView(CompanionUi.text(this, "Miko can chat using a provider you configure, or use her basic offline fallback. Cloud AI requires your own provider key; no paid service is enabled automatically.", 12f, CompanionUi.muted))
        root.addView(note)
        root.addView(CompanionUi.spacer(this, 18))
        root.addView(CompanionUi.text(this, "Drag the floating Miko anywhere • double-tap her to chat", 10f, CompanionUi.muted).apply { gravity = Gravity.CENTER })
        setContentView(scroll)
    }
}
