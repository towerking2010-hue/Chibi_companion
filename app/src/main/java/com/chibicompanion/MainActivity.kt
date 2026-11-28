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
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    companion object { private const val ACTION_SUMMON_MIKO = "com.chibicompanion.action.SUMMON_MIKO" }

    private val importSpritesRequest = 802

    private var waitingForOverlayPermission = false
    private var waitingForAssistantSettings = false
    private var pendingOpenChat = false
    private lateinit var setupButton: TextView
    private lateinit var floatingToggleButton: TextView

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
        val root = CompanionUi.column(this, 18).apply {
            setPadding(CompanionUi.dp(this@MainActivity, 20), CompanionUi.dp(this@MainActivity, 16),
                CompanionUi.dp(this@MainActivity, 20), CompanionUi.dp(this@MainActivity, 30))
        }
        scroll.addView(root)

        val header = CompanionUi.row(this)
        val brand = CompanionUi.column(this, 0)
        brand.addView(CompanionUi.eyebrow(this, "MIKO  /  YOUR COMPANION"))
        brand.addView(CompanionUi.heading(this, "A little closer ♡", 27f))
        brand.addView(CompanionUi.text(this, "Your day, with Miko by your side.", 13f, CompanionUi.muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "⚙") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }, LinearLayout.LayoutParams(CompanionUi.dp(this, 52), CompanionUi.dp(this, 48)))
        root.addView(header)

        val date = SimpleDateFormat("EEEE  ·  d MMMM", Locale.getDefault()).format(Date())
        root.addView(CompanionUi.text(this, date, 11f, CompanionUi.muted))

        val hero = CompanionUi.column(this, 14).apply {
            gravity = Gravity.CENTER
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(255, 237, 246), Color.rgb(235, 231, 251), 30)
            setPadding(CompanionUi.dp(this@MainActivity, 14), CompanionUi.dp(this@MainActivity, 12),
                CompanionUi.dp(this@MainActivity, 14), CompanionUi.dp(this@MainActivity, 18))
            elevation = CompanionUi.dp(this@MainActivity, 2).toFloat()
        }
        val heroTop = CompanionUi.row(this)
        heroTop.addView(CompanionUi.eyebrow(this, "YOUR LITTLE WORLD"))
        heroTop.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val status = CompanionUi.text(this, "●  HERE WITH YOU", 10f, CompanionUi.success, true).apply {
            background = CompanionUi.panel(this@MainActivity, Color.rgb(255, 255, 255), 20)
            setPadding(CompanionUi.dp(this@MainActivity, 10), CompanionUi.dp(this@MainActivity, 7),
                CompanionUi.dp(this@MainActivity, 10), CompanionUi.dp(this@MainActivity, 7))
        }
        heroTop.addView(status)
        hero.addView(heroTop)
        val heroAvatar = MikoSpriteView(this)
        hero.addView(heroAvatar, LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 350)))
        hero.addView(CompanionUi.heading(this, "Hmph… I was waiting for you!", 18f).apply {
            gravity = Gravity.CENTER
        })
        hero.addView(CompanionUi.text(this, "Come on, tell me what's happening.", 13f, CompanionUi.muted).apply {
            gravity = Gravity.CENTER
        })
        root.addView(hero)

        floatingToggleButton = CompanionUi.button(this, "♡  Summon Miko", true) { toggleFloatingCompanion() }
        root.addView(floatingToggleButton)

        root.addView(CompanionUi.eyebrow(this, "YOUR SPACE"))
        val actions = CompanionUi.row(this)
        val chatTile = CompanionUi.card(this, 15, Color.WHITE).apply {
            isClickable = true; isFocusable = true
            setOnClickListener { startActivity(Intent(this@MainActivity, ChatActivity::class.java)) }
        }
        chatTile.addView(CompanionUi.heading(this, "✦", 23f))
        chatTile.addView(CompanionUi.text(this, "Chat with Miko", 14f, CompanionUi.ink, true))
        chatTile.addView(CompanionUi.text(this, "Talk, ask, explore", 11f, CompanionUi.muted))
        val tasksTile = CompanionUi.card(this, 15, Color.WHITE).apply {
            isClickable = true; isFocusable = true
            setOnClickListener { startActivity(Intent(this@MainActivity, TasksActivity::class.java)) }
        }
        tasksTile.addView(CompanionUi.heading(this, "☼", 23f))
        tasksTile.addView(CompanionUi.text(this, "Tasks & plans", 14f, CompanionUi.ink, true))
        tasksTile.addView(CompanionUi.text(this, "${TaskStore.all(this).count { !it.done }} still to do", 11f, CompanionUi.muted))
        actions.addView(chatTile, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = CompanionUi.dp(this@MainActivity, 6) })
        actions.addView(tasksTile, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = CompanionUi.dp(this@MainActivity, 6) })
        root.addView(actions)

        val secondActions = CompanionUi.row(this)
        val notesTile = CompanionUi.card(this, 15, Color.WHITE).apply {
            isClickable = true; isFocusable = true
            setOnClickListener { startActivity(Intent(this@MainActivity, NotesActivity::class.java)) }
        }
        notesTile.addView(CompanionUi.heading(this, "▤", 23f))
        notesTile.addView(CompanionUi.text(this, "Private notes", 14f, CompanionUi.ink, true))
        notesTile.addView(CompanionUi.text(this, "${NoteStore.all(this).size} saved", 11f, CompanionUi.muted))
        val controlsTile = CompanionUi.card(this, 15, Color.WHITE).apply {
            isClickable = true; isFocusable = true
            setOnClickListener { showSummonOptions() }
        }
        controlsTile.addView(CompanionUi.heading(this, "⌁", 23f))
        controlsTile.addView(CompanionUi.text(this, "Summon anywhere", 14f, CompanionUi.ink, true))
        controlsTile.addView(CompanionUi.text(this, "Quick Settings & shortcut", 11f, CompanionUi.muted))
        secondActions.addView(notesTile, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = CompanionUi.dp(this@MainActivity, 6) })
        secondActions.addView(controlsTile, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = CompanionUi.dp(this@MainActivity, 6) })
        root.addView(secondActions)

        val presenceCard = CompanionUi.card(this, 18, Color.WHITE)
        presenceCard.addView(CompanionUi.eyebrow(this, "ALWAYS WITHIN REACH"))
        presenceCard.addView(CompanionUi.heading(this, "Miko can stay above your apps", 17f))
        presenceCard.addView(CompanionUi.text(this,
            "Use the launcher shortcut for a fast summon, or keep her Quick Settings tile available while using your phone.",
            12f, CompanionUi.muted))
        presenceCard.addView(CompanionUi.button(this, "Set up summon & Quick Settings") { showSummonOptions() })
        presenceCard.addView(CompanionUi.button(this, "Import Miko's three mouth images") { pickMikoImages() })
        root.addView(presenceCard)

        val setupCard = CompanionUi.card(this, 18, Color.rgb(246, 242, 251))
        setupCard.addView(CompanionUi.eyebrow(this, "OPTIONAL"))
        setupCard.addView(CompanionUi.text(this,
            "Choose Miko as your phone's digital assistant if your Android version and device support it. The floating shortcut works separately.",
            12f, CompanionUi.muted))
        setupButton = CompanionUi.button(this, "Set up phone assistant") { beginMikoSetup() }
        setupCard.addView(setupButton)
        root.addView(setupCard)

        root.addView(CompanionUi.text(this, "Private by default  ·  No paid AI calls unless you configure a provider",
            10f, CompanionUi.muted).apply { gravity = Gravity.CENTER; setPadding(0, CompanionUi.dp(this@MainActivity, 8), 0, 0) })
        setContentView(scroll)
        refreshSetupButton()
        refreshFloatingButton()
        if (intent?.action == ACTION_SUMMON_MIKO) {
            window.decorView.post { summonMikoFromShortcut() }
        }
    }

    private fun showSummonOptions() {
        MikoSheet.show(
            this,
            "QUICK ACCESS",
            "Bring Miko closer",
            "Choose the fastest way to summon her. Both options work without navigating through the Miko app each time.",
            listOf(
                "Home screen: press and hold the Miko app icon, then tap Summon Miko.",
                "Quick Settings: swipe down twice, tap Edit, and drag the Miko tile into your active tiles.",
                "After setup, tap the Miko tile once to summon her and again to hide her."
            ),
            "Got it"
        )
    }
    private fun summonMikoFromShortcut() {
        val running = MikoPresenceStore.isVisible(this)
        if (running) {
            Toast.makeText(this, "Miko is already here ♡", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startMikoFloating()
    }


    private fun pickMikoImages() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        startActivityForResult(pick, importSpritesRequest)
    }

    private fun importMouthSprites(uri: Uri) {
        val expected = setOf("miko_mouth_closed.webp", "miko_mouth_half.webp", "miko_mouth_open.webp")
        val destination = File(filesDir, "miko_assets").apply { mkdirs() }
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.substringAfterLast('/')
                        if (!entry.isDirectory && name in expected) {
                            val target = File(destination, name)
                            FileOutputStream(target).use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    require(total <= 5 * 1024 * 1024) { "Miko sprite is unexpectedly large" }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Unable to open selected ZIP")
            require(expected.all { File(destination, it).isFile && File(destination, it).length() > 0L }) {
                "ZIP must contain all three Miko mouth WebP images"
            }
            Toast.makeText(this, "Miko's three mouth images imported ♡", Toast.LENGTH_LONG).show()
            val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val running = manager.getRunningServices(Int.MAX_VALUE).any {
                it.service.className == FloatingCompanionService::class.java.name
            }
            if (running) startService(Intent(this, FloatingCompanionService::class.java).setAction("RELOAD_MIKO_ASSETS"))
            recreate()
        } catch (error: Exception) {
            expected.forEach { File(destination, it).delete() }
            Toast.makeText(this, "Import failed: ${error.message ?: "invalid ZIP"}", Toast.LENGTH_LONG).show()
        }
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
    }

    private fun refreshFloatingButton() {
        if (::floatingToggleButton.isInitialized) {
            val running = MikoPresenceStore.isVisible(this)
            floatingToggleButton.text = if (running) "♡  Hide floating Miko" else "♡  Summon Miko"
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
        if (MikoPresenceStore.isVisible(this)) {
            stopService(Intent(this, FloatingCompanionService::class.java))
            MikoPresenceStore.setVisible(this, false)
        } else {
            if (!Settings.canDrawOverlays(this)) {
                waitingForOverlayPermission = true
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            startMikoFloating()
        }
        refreshSetupButton()
        refreshFloatingButton()
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
        MikoPresenceStore.setVisible(this, true)
        Toast.makeText(this, "Miko is ready ♡", Toast.LENGTH_SHORT).show()
        refreshSetupButton()
        refreshFloatingButton()
    }

    @Deprecated("Deprecated by Android; retained for compatibility with assistant setup and ZIP import")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == importSpritesRequest) {
            if (resultCode == RESULT_OK && data?.data != null) importMouthSprites(data.data!!)
            return
        }
        if (requestCode == 904) {
            if (isMikoDefaultAssistant()) continueMikoSetup()
            else Toast.makeText(this, "Miko wasn't selected. You can try again anytime.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSetupButton()
        refreshFloatingButton()
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