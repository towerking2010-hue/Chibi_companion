package com.chibicompanion

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.widget.Toast

class MikoQuickSettingsTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("miko_companion_state", MODE_PRIVATE)
        val running = prefs.getBoolean("floating", false)
        if (running) {
            stopService(Intent(this, FloatingCompanionService::class.java).setAction("STOP_COMPANION"))
        } else if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow Miko to display over other apps first", Toast.LENGTH_LONG).show()
            startActivityAndCollapse(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            val intent = Intent(this, FloatingCompanionService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        }
        qsTile?.state = if (running) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.label = if (running) "Miko" else "Hide Miko"
        qsTile?.updateTile()
    }

    private fun refreshTile() {
        val running = getSharedPreferences("miko_companion_state", MODE_PRIVATE).getBoolean("floating", false)
        qsTile?.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.label = if (running) "Hide Miko" else "Summon Miko"
        qsTile?.updateTile()
    }
}
