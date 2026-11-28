package com.chibicompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.media.MediaMetadataRetriever
import android.text.Html
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile

data class ChatAttachment(
    val file: File,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isPdf: Boolean get() = mimeType == "application/pdf" || displayName.endsWith(".pdf", true)
    fun description(): String = "${displayName} (${mimeType.ifBlank { "file" }}, ${sizeBytes / 1024} KB)"
}

/** Copies user-selected attachments into app-private storage; no broad storage permission is needed. */
object AttachmentStore {
    private const val MAX_ATTACHMENT_BYTES = 50L * 1024L * 1024L

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
        name = name.take(120).ifBlank { "attachment" }
        val safeFileName = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "attachment" }
        val extension = safeFileName.substringAfterLast('.', "")
        val safeName = if (extension.isBlank()) safeFileName else safeFileName.substringBeforeLast('.')
        val destination = File(directory(context), UUID.randomUUID().toString() + "_" + safeName +
            if (extension.isNotBlank()) ".$extension" else "")
        val input = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Could not open the selected attachment")
        try {
            input.use { source ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_ATTACHMENT_BYTES) {
                            throw IllegalArgumentException("Attachments must be 50 MB or smaller")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
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

    /** Render up to the first three PDF pages into one bounded preview image for vision models. */
    fun pdfPreviewJpeg(attachment: ChatAttachment, maxSide: Int = 1536): ByteArray? {
        if (!attachment.isPdf || !attachment.file.exists()) return null
        return runCatching {
            val descriptor = ParcelFileDescriptor.open(attachment.file, ParcelFileDescriptor.MODE_READ_ONLY)
            descriptor.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val count = minOf(3, renderer.pageCount)
                    if (count <= 0) return@runCatching null
                    val pageSizes = (0 until count).map { index ->
                        renderer.openPage(index).use { page ->
                            val width = 600
                            val height = (page.height.toFloat() * width / page.width).toInt().coerceAtLeast(1)
                            width to height
                        }
                    }
                    val gap = 16
                    val totalHeight = pageSizes.sumOf { it.second } + gap * (count - 1)
                    val preview = Bitmap.createBitmap(600, totalHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(preview).apply { drawColor(Color.WHITE) }
                    var top = 0
                    for (index in 0 until count) {
                        val (width, height) = pageSizes[index]
                        val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        renderer.openPage(index).use { page ->
                            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                        canvas.drawBitmap(pageBitmap, 0f, top.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
                        top += height + gap
                        pageBitmap.recycle()
                    }
                    val finalBitmap = if (preview.width > maxSide || preview.height > maxSide) {
                        val ratio = minOf(maxSide.toFloat() / preview.width, maxSide.toFloat() / preview.height)
                        Bitmap.createScaledBitmap(preview, (preview.width * ratio).toInt().coerceAtLeast(1),
                            (preview.height * ratio).toInt().coerceAtLeast(1), true).also { preview.recycle() }
                    } else preview
                    try {
                        val output = java.io.ByteArrayOutputStream()
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 84, output)
                        output.toByteArray()
                    } finally { finalBitmap.recycle() }
                }
            }
        }.getOrNull()
    }

    /** Build a contact sheet from five timestamps; video audio is not included in this preview. */
    fun videoPreviewJpeg(attachment: ChatAttachment): ByteArray? {
        if (!attachment.isVideo || !attachment.file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(attachment.file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) return null
            val positions = listOf(0L, durationMs / 4, durationMs / 2, durationMs * 3 / 4, (durationMs - 1).coerceAtLeast(0))
            val cellW = 480
            val cellH = 270
            val sheet = Bitmap.createBitmap(cellW * 3, cellH * 2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sheet).apply { drawColor(Color.WHITE) }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            positions.forEachIndexed { index, time ->
                val frame = retriever.getFrameAtTime(time * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val scale = minOf(cellW.toFloat() / frame.width, cellH.toFloat() / frame.height)
                    val width = (frame.width * scale).toInt()
                    val height = (frame.height * scale).toInt()
                    val left = (index % 3) * cellW + (cellW - width) / 2
                    val top = (index / 3) * cellH + (cellH - height) / 2
                    canvas.drawBitmap(frame, null, android.graphics.Rect(left, top, left + width, top + height), paint)
                    frame.recycle()
                }
            }
            try {
                val output = java.io.ByteArrayOutputStream()
                sheet.compress(Bitmap.CompressFormat.JPEG, 82, output)
                output.toByteArray()
            } finally { sheet.recycle() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun readSmallText(attachment: ChatAttachment, limit: Int = 12000): String? {
        if (!attachment.file.exists() || attachment.sizeBytes > 512_000) return null
        val name = attachment.displayName.lowercase()
        if (attachment.mimeType == "text/plain" || name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv")) {
            return runCatching { attachment.file.readText(Charsets.UTF_8).take(limit) }.getOrNull()
        }
        if (name.endsWith(".docx")) {
            return runCatching {
                ZipFile(attachment.file).use { zip ->
                    val entry = zip.getEntry("word/document.xml") ?: return@use null
                    val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val text = Regex("<w:t[^>]*>(.*?)</w:t>", setOf(RegexOption.DOT_MATCHES_ALL))
                        .findAll(xml).joinToString(" ") { it.groupValues[1] }
                    Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().take(limit)
                }
            }.getOrNull()
        }
        return null
    }
}
