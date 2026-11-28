package com.chibicompanion

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast

/** System document/photo picker for the floating quick composer. */
class QuickAttachmentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(pick, PICK_FILE) }
        catch (_: Exception) { Toast.makeText(this, "No file picker is available", Toast.LENGTH_SHORT).show(); finish() }
    }

    @Deprecated("Uses the platform document picker result contract")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                val name = queryName(uri) ?: uri.lastPathSegment ?: "attachment"
                startService(Intent(this, FloatingCompanionService::class.java).apply {
                    action = FloatingCompanionService.ACTION_ATTACHMENT_READY
                    this.data = uri
                    putExtra(FloatingCompanionService.EXTRA_ATTACHMENT_NAME, name)
                    putExtra(FloatingCompanionService.EXTRA_ATTACHMENT_MIME, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }
        }
        finish()
    }

    private fun queryName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) cursor.getString(0) else null
        } catch (_: Exception) { null } finally { cursor?.close() }
    }

    companion object { private const val PICK_FILE = 811 }
}
