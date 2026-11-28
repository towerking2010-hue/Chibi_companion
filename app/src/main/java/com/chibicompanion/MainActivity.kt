package com.chibicompanion

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MainActivity : Activity() {
    private val importSpritesRequest = 802
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 801)
        }
        window.statusBarColor = CompanionUi.bg
        window.navigationBarColor = CompanionUi.bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = CompanionUi.column(this, 22).apply { setBackgroundColor(CompanionUi.bg) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        val brand = CompanionUi.column(this).apply {
            addView(CompanionUi.text(this@MainActivity, "CHIBI COMPANION", 11f, CompanionUi.pink, true))
            addView(CompanionUi.text(this@MainActivity, "Your little world ♡", 23f, CompanionUi.ink, true))
        }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.text(this, "✦", 27f, CompanionUi.lavender, true))
        root.addView(header)
        root.addView(CompanionUi.spacer(this, 18))

        val hero = CompanionUi.column(this, 12).apply {
            background = CompanionUi.gradient(this@MainActivity, Color.rgb(255,235,243), Color.rgb(237,234,255), 30)
            gravity = Gravity.CENTER
        }
        hero.addView(CompanionUi.text(this, "YOUR COMPANION", 10f, CompanionUi.muted, true))
        val avatar = MikoSpriteView(this)
        hero.addView(avatar, LinearLayout.LayoutParams(-1, CompanionUi.dp(this, 280)))
        hero.addView(CompanionUi.text(this, "Hmph… I was waiting for you!", 16f, CompanionUi.ink, true))
        hero.addView(CompanionUi.text(this, "A tiny friend, always nearby.", 12f, CompanionUi.muted))
        root.addView(hero, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(CompanionUi.spacer(this, 18))

        root.addView(CompanionUi.text(this, "Make yourself at home", 18f, CompanionUi.ink, true))
        root.addView(CompanionUi.spacer(this, 10))
        root.addView(CompanionUi.button(this, "♡  Start floating companion", true) {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingCompanionService::class.java))
                Toast.makeText(this, "Your companion is now floating ♡", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.button(this, "Import Miko mouth images") {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            }
            startActivityForResult(pick, importSpritesRequest)
        })
        root.addView(CompanionUi.spacer(this, 9))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(CompanionUi.button(this, "Chat & AI settings") {
            startActivity(Intent(this@MainActivity, ChatActivity::class.java))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val gap = View(this); row.addView(gap, LinearLayout.LayoutParams(CompanionUi.dp(this,8),1))
        row.addView(CompanionUi.button(this, "Stop floating") {
            stopService(Intent(this@MainActivity, FloatingCompanionService::class.java))
            Toast.makeText(this, "Floating companion stopped", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        root.addView(CompanionUi.spacer(this, 8))
        root.addView(CompanionUi.text(this, "Drag her anywhere • double-tap to chat", 11f, CompanionUi.muted).apply { gravity = Gravity.CENTER })
        setContentView(root)
    }

    @Deprecated("Activity result API retained for compatibility with the current project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != importSpritesRequest || resultCode != RESULT_OK || data?.data == null) return
        val expected = setOf("miko_mouth_closed.webp", "miko_mouth_half.webp", "miko_mouth_open.webp")
        val destination = File(filesDir, "miko_assets").apply { mkdirs() }
        var imported = 0
        try {
            contentResolver.openInputStream(data.data!!)?.use { input ->
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
                                    if (count <= 0) break
                                    total += count
                                    require(total <= 5 * 1024 * 1024) { "Miko sprite is unexpectedly large" }
                                    output.write(buffer, 0, count)
                                }
                            }
                            imported++
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Unable to open selected ZIP")
            val complete = expected.all { File(destination, it).isFile && File(destination, it).length() > 0L }
            if (!complete) error("ZIP must contain all three Miko mouth WebP images")
            Toast.makeText(this, "Miko's three mouth images imported ♡", Toast.LENGTH_LONG).show()
            recreate()
        } catch (error: Exception) {
            expected.forEach { File(destination, it).delete() }
            Toast.makeText(this, "Import failed: ${error.message ?: "invalid ZIP"}", Toast.LENGTH_LONG).show()
        }
    }
}
