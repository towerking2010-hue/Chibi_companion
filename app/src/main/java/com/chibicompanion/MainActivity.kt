package com.chibicompanion

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

class MainActivity : Activity() {
    private var waitingForOverlayPermission = false
    private lateinit var setupButton: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 903)
        }
        window.statusBarColor = CompanionUi.bg
        window.navigationBarColor = CompanionUi.bg
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(CompanionUi.bg)
        }
        val root = CompanionUi.column(this, 20)
        scroll.addView(root)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val title = CompanionUi.column(this)
        title.addView(CompanionUi.text(this, "MIKO  ✦", 11f, CompanionUi.pink, true))
        title.addView(CompanionUi.text(this, "I'm here.", 27f, CompanionUi.ink, true))
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "⚙") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        root.addView(header)

        val hero = CompanionUi.column(this, 12).apply {
            gravity = Gravity.CENTER
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(42,31,75), Color.rgb(20,42,75), 28)
        }
        hero.addView(ModelSlotView(this), LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 300)))
        hero.addView(CompanionUi.text(this, "Your little companion. Always nearby.", 13f, CompanionUi.muted).apply {
            gravity = Gravity.CENTER
        })
        root.addView(hero)

        setupButton = CompanionUi.button(this, "✦  Enable Miko", true) { beginMikoSetup() }
        root.addView(setupButton)
        root.addView(CompanionUi.text(this, "Set Miko as your phone assistant to summon her with Android's assistant shortcut.", 12f, CompanionUi.muted))

        root.addView(CompanionUi.button(this, "Talk to Miko", true) {
            startActivity(Intent(this, ChatActivity::class.java))
        })

        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun shortcut(label: String, action: () -> Unit) {
            val view = CompanionUi.button(this, label, false, action)
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            lp.marginEnd = CompanionUi.dp(this, 6)
            shortcuts.addView(view, lp)
        }
        shortcut("Tasks") { startActivity(Intent(this, TasksActivity::class.java)) }
        shortcut("Notes") { startActivity(Intent(this, NotesActivity::class.java)) }
        root.addView(shortcuts)
        root.addView(CompanionUi.text(this, "More features live inside your chat and settings. No clutter.", 11f, CompanionUi.muted).apply {
            gravity = Gravity.CENTER
        })
        setContentView(scroll)
        refreshSetupButton()
    }

    private fun isMikoDefaultAssistant(): Boolean {
        return Build.VERSION.SDK_INT >= 29 &&
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
    }

    private fun refreshSetupButton() {
        if (::setupButton.isInitialized) {
            setupButton.text = if (isMikoDefaultAssistant()) "✓  Miko is your assistant" else "✦  Enable Miko"
        }
    }

    private fun beginMikoSetup() {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = getSystemService(RoleManager::class.java)
            if (!roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                Toast.makeText(this, "This Android version doesn't offer the assistant role.", Toast.LENGTH_LONG).show()
                return
            }
            if (roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                continueMikoSetup()
            } else {
                startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), 904)
            }
        } else {
            runCatching { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                .onFailure { Toast.makeText(this, "Open your phone's Digital assistant app setting.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun continueMikoSetup() {
        refreshSetupButton()
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startMikoFloating()
    }

    private fun startMikoFloating() {
        val intent = Intent(this, FloatingCompanionService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Miko is ready ♡", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated by Android; retained for compatibility with the assistant role prompt")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 904) {
            if (isMikoDefaultAssistant()) continueMikoSetup()
            else Toast.makeText(this, "Miko wasn't selected. You can try again anytime.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSetupButton()
        if (waitingForOverlayPermission) {
            waitingForOverlayPermission = false
            if (Settings.canDrawOverlays(this) && isMikoDefaultAssistant()) startMikoFloating()
        }
    }
}
