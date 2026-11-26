package com.chibicompanion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.app.role.RoleManager
import android.os.Build

class MainActivity : Activity() {
    private var waitingForOverlayPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 903)
        }
        window.statusBarColor = CompanionUi.bg
        window.navigationBarColor = CompanionUi.bg
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(CompanionUi.bg) }
        val root = CompanionUi.column(this, 18)
        scroll.addView(root)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = CompanionUi.column(this)
        brand.addView(CompanionUi.text(this, "MIKO  ✦  PERSONAL ASSISTANT", 10f, CompanionUi.pink, true))
        brand.addView(CompanionUi.text(this, "Ready when you are.", 23f, CompanionUi.ink, true))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.text(this, "♡", 30f, CompanionUi.lavender, true))
        root.addView(header)

        val setupCard = CompanionUi.column(this, 12).apply {
            background = CompanionUi.panel(this@MainActivity, Color.rgb(30,36,65), 22)
        }
        setupCard.addView(CompanionUi.text(this, "GET STARTED", 10f, CompanionUi.pink, true))
        setupCard.addView(CompanionUi.text(this, "Make Miko your phone assistant", 17f, CompanionUi.ink, true))
        setupCard.addView(CompanionUi.text(this, "One guided setup. Android will ask you to approve Miko as your assistant, then allow her floating companion bubble.", 12f, CompanionUi.muted))
        setupCard.addView(CompanionUi.button(this, "✦  Enable Miko", true) { beginMikoSetup() })
        root.addView(setupCard)

        val modelCard = CompanionUi.column(this, 10).apply {
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(42,31,75), Color.rgb(20,42,75), 28)
        }
        modelCard.addView(ModelSlotView(this), LinearLayout.LayoutParams(-1, CompanionUi.dp(this,330)))
        root.addView(modelCard, LinearLayout.LayoutParams(-1,-2))
        root.addView(CompanionUi.text(this, "YOUR EVERYDAY ASSISTANT", 10f, CompanionUi.pink, true))
        root.addView(CompanionUi.text(this, "Talk, ask, search, plan and control supported phone actions.", 13f, CompanionUi.muted))
        root.addView(CompanionUi.button(this, "✦  Talk to Miko", true) {
            startActivity(Intent(this, ChatActivity::class.java))
        })
        root.addView(CompanionUi.button(this, "✓  Tasks and plans") { startActivity(Intent(this, TasksActivity::class.java)) })
        root.addView(CompanionUi.button(this, "✎  Notes") { startActivity(Intent(this, NotesActivity::class.java)) })
        root.addView(CompanionUi.spacer(this, 4))
        root.addView(CompanionUi.text(this, "QUICK ACTIONS", 10f, CompanionUi.pink, true))
        listOf(
            "⏰  Set an alarm" to "Set alarm for 7:00 AM",
            "⌛  Start a timer" to "Set timer for 10 minutes",
            "🔎  Search the web" to "Search ",
            "📱  Open an app" to "Open "
        ).forEach { (label, command) ->
            root.addView(CompanionUi.button(this, label) {
                startActivity(Intent(this, ChatActivity::class.java).putExtra(ChatActivity.EXTRA_PROMPT, command))
            })
            root.addView(CompanionUi.spacer(this, 7))
        }
        root.addView(CompanionUi.button(this, "☁  Start floating Miko") {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingCompanionService::class.java))
                Toast.makeText(this, "Miko's floating panel is ready", Toast.LENGTH_SHORT).show()
            } else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(CompanionUi.button(this, "＋  Add Miko to Quick Settings") {
            Toast.makeText(this, "Swipe down twice, tap Edit (pencil), then drag Miko into your Quick Settings tiles.", Toast.LENGTH_LONG).show()
        })
        root.addView(CompanionUi.button(this, "✕  Stop floating Miko") {
            stopService(Intent(this, FloatingCompanionService::class.java).setAction("STOP_COMPANION"))
            Toast.makeText(this, "Floating Miko stopped", Toast.LENGTH_SHORT).show()
        })
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "⚙  Settings and connections") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        root.addView(CompanionUi.spacer(this, 10))
        val note = CompanionUi.column(this,14).apply {
            background = CompanionUi.panel(this@MainActivity, Color.rgb(30,36,65),22)
        }
        note.addView(CompanionUi.text(this, "A note about AI and privacy",14f,CompanionUi.ink,true))
        note.addView(CompanionUi.text(this,
            "Phone actions use Android's own confirmation screens. Cloud AI is optional and uses only a provider key you add. Without a key, offline replies are basic and limited; no paid service is enabled automatically.",
            12f,CompanionUi.muted))
        root.addView(note)
        root.addView(CompanionUi.spacer(this,18))
        root.addView(CompanionUi.text(this,"The character viewport is reserved for your animated Miko asset.",10f,CompanionUi.muted).apply { gravity=Gravity.CENTER })
        setContentView(scroll)
    }

    private fun beginMikoSetup() {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = getSystemService(RoleManager::class.java)
            if (!roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                Toast.makeText(this, "This Android version does not offer the assistant role.", Toast.LENGTH_LONG).show()
                return
            }
            if (roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                continueMikoSetup()
            } else {
                startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), 904)
            }
        } else {
            runCatching { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                .onFailure { Toast.makeText(this, "Open Settings → Default apps → Digital assistant app.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun continueMikoSetup() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            Toast.makeText(this, "Allow display over other apps once, then Miko will appear.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startMikoFloating()
    }

    private fun startMikoFloating() {
        val intent = Intent(this, FloatingCompanionService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Miko is enabled and ready ♡", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 904) {
            if (Build.VERSION.SDK_INT >= 29 &&
                getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                continueMikoSetup()
            } else {
                Toast.makeText(this, "Miko wasn't selected as the default assistant. You can try again anytime.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForOverlayPermission) {
            waitingForOverlayPermission = false
            if (Settings.canDrawOverlays(this) &&
                (Build.VERSION.SDK_INT < 29 || getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT))) {
                startMikoFloating()
            }
        }
    }
}
