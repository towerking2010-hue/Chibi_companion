package com.chibicompanion

import android.content.Context
import android.content.ComponentName
import android.os.Build
import android.service.quicksettings.TileService

/**
 * Single source of truth for the companion's visible/hidden state.
 * The overlay service owns the actual window; this store synchronises UI and Quick Settings.
 */
object MikoPresenceStore {
    private const val PREFS = "miko_companion_state"
    private const val KEY_FLOATING = "floating"

    fun isVisible(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_FLOATING, false)

    fun setVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FLOATING, visible).apply()
        refreshQuickSettings(context)
    }

    fun refreshQuickSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, MikoQuickSettingsTileService::class.java)
                )
            }
        }
    }
}
