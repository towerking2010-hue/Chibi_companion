package com.chibicompanion

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build

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
        } else {
            val intent = Intent(this, FloatingCompanionService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        }
        qsTile?.state = if (running) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    private fun refreshTile() {
        val running = getSharedPreferences("miko_companion_state", MODE_PRIVATE).getBoolean("floating", false)
        qsTile?.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.label = if (running) "Hide Miko" else "Summon Miko"
        qsTile?.updateTile()
    }
}
