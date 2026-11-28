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
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val importSpritesRequest = 802

    private var waitingForOverlayPermission = false
    private var waitingForAssistantSettings = false
    private var pendingOpenChat = false
    private lateinit var setupButton: TextView

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
        brand.addView(CompanionUi.eyebrow(this, "CHIBI COMPANION"))
        brand.addView(CompanionUi.heading(this, "Your little world ♡", 25f))
        brand.addView(CompanionUi.text(this, "A tiny friend, always nearby.", 13f, CompanionUi.muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "⚙") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }, LinearLayout.LayoutParams(CompanionUi.dp(this, 52), CompanionUi.dp(this, 48)))
        root.addView(header)

        val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        root.addView(CompanionUi.text(this, date, 12f, CompanionUi.muted))

        // Miko is rendered from the three owner-supplied mouth images.
        val hero = CompanionUi.column(this, 14).apply {
            gravity = Gravity.CENTER
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(255, 235, 244), Color.rgb(239, 234, 255), 30)
            setPadding(CompanionUi.dp(this@MainActivity, 12), CompanionUi.dp(this@MainActivity, 10),
                CompanionUi.dp(this@MainActivity, 12), CompanionUi.dp(this@MainActivity, 16))
        }
        val heroAvatar = MikoSpriteView(this)
        hero.addView(heroAvatar, LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 390)))
        hero.addView(CompanionUi.text(this, "Hmph… I was waiting for you!", 17f, CompanionUi.ink, true).apply {
            gravity = Gravity.CENTER
        })
        hero.addView(CompanionUi.text(this, "A tiny friend, always nearby.", 13f, CompanionUi.muted).apply {
            gravity = Gravity.CENTER
        })
        root.addView(hero)
        root.addView(CompanionUi.button(this, "♡  Start / stop floating Miko", true) { toggleFloatingCompanion() })

        val quickControl = CompanionUi.card(this, 15)
        quickControl.addView(CompanionUi.eyebrow(this, "Always within reach"))
        quickControl.addView(CompanionUi.text(this, "Control Miko without opening this app", 15f, CompanionUi.ink, true))
        quickControl.addView(CompanionUi.text(this,
            "Add the Miko tile to Quick Settings once. Then tap Summon Miko or Hide Miko from anywhere.",
            12f, CompanionUi.muted))
        quickControl.addView(CompanionUi.button(this, "How to add the Quick Settings tile") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Add Miko to Quick Settings")
                .setMessage("1. Swipe down from the top of your screen twice.\n\n2. Tap the pencil or Edit button.\n\n3. Find the Miko tile in available tiles.\n\n4. Drag it into your active tiles and tap Done.\n\nNow you can summon or hide Miko without opening her app.")
                .setPositiveButton("Got it", null)
                .show()
        })
        root.addView(quickControl)
        root.addView(CompanionUi.button(this, "Import Miko mouth images") { pickMikoImages() })
        val quickLinks = CompanionUi.row(this)
        quickLinks.addView(CompanionUi.button(this, "Chat & AI settings") {
            startActivity(Intent(this, ChatActivity::class.java))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        quickLinks.addView(CompanionUi.button(this, "Stop floating") {
            stopService(Intent(this, FloatingCompanionService::class.java))
            refreshSetupButton()
            Toast.makeText(this, "Floating companion stopped", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = CompanionUi.dp(this@MainActivity, 8) })
        root.addView(quickLinks)
        val productivity = CompanionUi.row(this)
        productivity.addView(CompanionUi.button(this, "Tasks") {
            startActivity(Intent(this, TasksActivity::class.java))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        productivity.addView(CompanionUi.button(this, "Notes") {
            startActivity(Intent(this, NotesActivity::class.java))
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = CompanionUi.dp(this@MainActivity, 8) })
        root.addView(productivity)

        val overview = CompanionUi.card(this, 16)
        overview.addView(CompanionUi.eyebrow(this, "A little overview"))
        overview.addView(CompanionUi.spacer(this, 8))
        val overviewRow = CompanionUi.row(this)
        val taskCount = TaskStore.all(this).count { !it.done }
        val noteCount = NoteStore.all(this).size
        val tasksSummary = CompanionUi.column(this, 0)
        tasksSummary.addView(CompanionUi.heading(this, taskCount.toString(), 23f))
        tasksSummary.addView(CompanionUi.text(this, "Tasks left", 11f, CompanionUi.muted))
        val notesSummary = CompanionUi.column(this, 0)
        notesSummary.addView(CompanionUi.heading(this, noteCount.toString(), 23f))
        notesSummary.addView(CompanionUi.text(this, "Private notes", 11f, CompanionUi.muted))
        overviewRow.addView(tasksSummary, LinearLayout.LayoutParams(0, -2, 1f))
        overviewRow.addView(notesSummary, LinearLayout.LayoutParams(0, -2, 1f))
        overview.addView(overviewRow)
        root.addView(overview)

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