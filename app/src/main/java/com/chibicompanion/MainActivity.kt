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
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private var waitingForOverlayPermission = false
    private var waitingForAssistantSettings = false
    private var pendingOpenChat = false
    private lateinit var setupButton: TextView
    private lateinit var floatingButton: TextView
    private lateinit var statusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 903)
        }
        CompanionUi.applySystemBars(this)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setBackgroundColor(CompanionUi.bg)
        }
        val root = CompanionUi.column(this, 20).apply {
            setPadding(CompanionUi.dp(this@MainActivity, 20), CompanionUi.dp(this@MainActivity, 18),
                CompanionUi.dp(this@MainActivity, 20), CompanionUi.dp(this@MainActivity, 28))
        }
        scroll.addView(root)

        val header = CompanionUi.row(this)
        val brand = CompanionUi.column(this, 0)
        brand.addView(CompanionUi.eyebrow(this, "Your companion"))
        brand.addView(CompanionUi.heading(this, "Good ${greeting()},", 25f))
        brand.addView(CompanionUi.text(this, "I'm right here with you.", 13f, CompanionUi.muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "⚙") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }, LinearLayout.LayoutParams(CompanionUi.dp(this, 52), CompanionUi.dp(this, 48)))
        root.addView(header)

        val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        root.addView(CompanionUi.text(this, date, 12f, CompanionUi.muted))

        // The owner's current model placeholder is intentionally unchanged.
        val hero = CompanionUi.column(this, 14).apply {
            gravity = Gravity.CENTER
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(39, 35, 72), Color.rgb(22, 48, 67), 26)
            setPadding(CompanionUi.dp(this@MainActivity, 12), CompanionUi.dp(this@MainActivity, 10),
                CompanionUi.dp(this@MainActivity, 12), CompanionUi.dp(this@MainActivity, 16))
        }
        hero.addView(ModelSlotView(this), LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 278)))
        hero.addView(CompanionUi.text(this, "A little company, whenever you need it.", 13f, Color.rgb(222, 220, 244)).apply {
            gravity = Gravity.CENTER
        })
        root.addView(hero)

        val quick = CompanionUi.card(this, 16)
        quick.addView(CompanionUi.eyebrow(this, "Quick access"))
        quick.addView(CompanionUi.spacer(this, 10))
        quick.addView(CompanionUi.button(this, "✦   Talk to Miko", true) {
            startActivity(Intent(this, ChatActivity::class.java))
        })
        val quickRow = CompanionUi.row(this)
        fun quickAction(label: String, action: () -> Unit) {
            val view = CompanionUi.button(this, label, false, action)
            val lp = LinearLayout.LayoutParams(0, CompanionUi.dp(this, 48), 1f).apply {
                topMargin = CompanionUi.dp(this@MainActivity, 9)
                marginEnd = CompanionUi.dp(this@MainActivity, 7)
            }
            quickRow.addView(view, lp)
        }
        quickAction("Tasks") { startActivity(Intent(this, TasksActivity::class.java)) }
        quickAction("Notes") { startActivity(Intent(this, NotesActivity::class.java)) }
        quick.addView(quickRow)
        root.addView(quick)

        val companionCard = CompanionUi.card(this, 16)
        companionCard.addView(CompanionUi.eyebrow(this, "Always within reach"))
        companionCard.addView(CompanionUi.spacer(this, 5))
        companionCard.addView(CompanionUi.text(this,
            "Keep Miko floating above your other apps, or open her compact chat without leaving what you're doing.",
            12f, CompanionUi.muted))
        companionCard.addView(CompanionUi.spacer(this, 10))
        statusLabel = CompanionUi.text(this, "Checking companion status…", 11f, CompanionUi.success)
        companionCard.addView(statusLabel)
        floatingButton = CompanionUi.button(this, "Show floating Miko", true) { toggleFloatingCompanion() }
        companionCard.addView(floatingButton, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = CompanionUi.dp(this@MainActivity, 10)
        })
        companionCard.addView(CompanionUi.button(this, "Open floating chat") { openFloatingChat() })
        root.addView(companionCard)

        val setupCard = CompanionUi.card(this, 16)
        setupCard.addView(CompanionUi.eyebrow(this, "Optional setup"))
        setupCard.addView(CompanionUi.spacer(this, 5))
        setupCard.addView(CompanionUi.text(this,
            "Choose Miko as your phone's digital assistant if your Android version and device support it. The floating shortcut works separately.",
            12f, CompanionUi.muted))
        setupButton = CompanionUi.button(this, "Set up phone assistant") { beginMikoSetup() }
        setupCard.addView(setupButton, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = CompanionUi.dp(this@MainActivity, 10)
        })
        root.addView(setupCard)

        root.addView(CompanionUi.text(this, "Private by default  •  No paid AI calls unless you add a provider key",
            10f, CompanionUi.muted).apply { gravity = Gravity.CENTER; setPadding(0, CompanionUi.dp(this@MainActivity, 4), 0, 0) })
        setContentView(scroll)
        refreshSetupButton()
    }

    private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }

    private fun isMikoDefaultAssistant(): Boolean =
        Build.VERSION.SDK_INT >= 29 && getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)

    private fun refreshSetupButton() {
        if (::setupButton.isInitialized) setupButton.text =
            if (isMikoDefaultAssistant()) "✓  Miko is your assistant" else "Set up phone assistant"
        if (::floatingButton.isInitialized) {
            val active = getSharedPreferences("miko_companion_state", MODE_PRIVATE).getBoolean("floating", false)
            floatingButton.text = if (active) "Hide floating Miko" else "Show floating Miko"
            if (::statusLabel.isInitialized) statusLabel.text =
                if (active) "●  Miko is floating" else "○  Miko is currently hidden"
        }
    }

    private fun beginMikoSetup() {
        if (Build.VERSION.SDK_INT >= 29 && isMikoDefaultAssistant()) {
            continueMikoSetup()
            return
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                waitingForAssistantSettings = true
                startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), 904)
                return
            }
        }
        waitingForAssistantSettings = true
        try {
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch (_: Exception) {
            waitingForAssistantSettings = false
            try { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            catch (_: Exception) {
                Toast.makeText(this, "Open Settings → Apps → Choose default apps → Digital assistant app.", Toast.LENGTH_LONG).show()
            }
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

    private fun toggleFloatingCompanion() {
        val prefs = getSharedPreferences("miko_companion_state", MODE_PRIVATE)
        if (prefs.getBoolean("floating", false)) {
            stopService(Intent(this, FloatingCompanionService::class.java))
            prefs.edit().putBoolean("floating", false).apply()
        } else {
            if (!Settings.canDrawOverlays(this)) {
                waitingForOverlayPermission = true
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            startMikoFloating()
        }
        refreshSetupButton()
    }

    private fun openFloatingChat() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            pendingOpenChat = true
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val intent = Intent(this, FloatingCompanionService::class.java).setAction("OPEN_CHAT")
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }

    private fun startMikoFloating() {
        val intent = Intent(this, FloatingCompanionService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        getSharedPreferences("miko_companion_state", MODE_PRIVATE).edit().putBoolean("floating", true).apply()
        Toast.makeText(this, "Miko is ready ♡", Toast.LENGTH_SHORT).show()
        refreshSetupButton()
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
        if (waitingForAssistantSettings) {
            waitingForAssistantSettings = false
            if (isMikoDefaultAssistant()) continueMikoSetup()
        }
        if (waitingForOverlayPermission) {
            waitingForOverlayPermission = false
            if (Settings.canDrawOverlays(this)) {
                if (pendingOpenChat) {
                    pendingOpenChat = false
                    openFloatingChat()
                } else startMikoFloating()
            }
        }
    }
}