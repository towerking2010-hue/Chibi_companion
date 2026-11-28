package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ChatAttachment(
    val file: File,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    fun description(): String = "${displayName} (${mimeType.ifBlank { "file" }}, ${sizeBytes / 1024} KB)"
}

/** Copies user-selected attachments into app-private storage; no broad storage permission is needed. */
object AttachmentStore {
    private fun directory(context: Context): File =
        File(context.filesDir, "chat_attachments").apply { if (!exists()) mkdirs() }

    fun importUri(context: Context, uri: Uri): ChatAttachment {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        var name = "attachment"
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index) ?: name
                }
            }
        }
        name = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "attachment" }
        val extension = name.substringAfterLast('.', "")
        val safeName = if (extension.isBlank()) name else name.substringBeforeLast('.')
        val destination = File(directory(context), UUID.randomUUID().toString() + "_" + safeName +
            if (extension.isNotBlank()) ".$extension" else "")
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open the selected attachment" }
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
        return ChatAttachment(destination, name, mime, destination.length())
    }

    fun saveCameraBitmap(context: Context, bitmap: Bitmap): ChatAttachment {
        val file = File(directory(context), "camera_" + UUID.randomUUID() + ".jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return ChatAttachment(file, "Camera photo.jpg", "image/jpeg", file.length())
    }

    /** Produces a bounded JPEG for vision providers without loading a full-resolution photo into memory. */
    fun jpegForVision(attachment: ChatAttachment, maxSide: Int = 1536): ByteArray? {
        if (!attachment.isImage || !attachment.file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(attachment.file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(attachment.file.absolutePath, options) ?: return null
        val scaled = if (bitmap.width > maxSide || bitmap.height > maxSide) {
            val ratio = minOf(maxSide.toFloat() / bitmap.width, maxSide.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1), true).also { if (it !== bitmap) bitmap.recycle() }
        } else bitmap
        return try {
            val bytes = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 84, bytes)
            bytes.toByteArray()
        } finally { scaled.recycle() }
    }

    fun readSmallText(attachment: ChatAttachment, limit: Int = 12000): String? {
        if (attachment.mimeType != "text/plain" && !attachment.displayName.endsWith(".txt", true) &&
            !attachment.displayName.endsWith(".md", true) && !attachment.displayName.endsWith(".csv", true)) return null
        if (!attachment.file.exists() || attachment.sizeBytes > 512_000) return null
        return runCatching { attachment.file.readText(Charsets.UTF_8).take(limit) }.getOrNull()
    }
}
