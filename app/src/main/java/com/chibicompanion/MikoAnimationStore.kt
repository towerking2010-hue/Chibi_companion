package com.chibicompanion

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Imports a small, allow-listed set of transparent animated WebP clips.
 * ZIP paths are never used as filesystem paths; only approved filenames are written.
 */
object MikoAnimationStore {
    val moods = setOf(
        "idle", "thinking", "happy", "excited", "laughing", "shy",
        "tsundere", "angry", "sad", "comfort", "surprised", "sleepy",
        "listening", "greeting", "goodbye", "talking"
    )

    private const val MAX_TOTAL_BYTES = 80L * 1024L * 1024L
    private const val MAX_CLIP_BYTES = 8L * 1024L * 1024L

    fun directory(context: Context): File =
        File(context.filesDir, "miko_animations").apply { if (!exists()) mkdirs() }

    fun has(context: Context, mood: String): Boolean =
        File(directory(context), "$mood.webp").let { it.isFile && it.length() > 0L }

    fun importZip(context: Context, input: java.io.InputStream): Int {
        val destination = directory(context)
        val staged = mutableListOf<File>()
        var total = 0L
        var imported = 0
        try {
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/').lowercase()
                    val mood = name.removeSuffix(".webp")
                    if (!entry.isDirectory && name.endsWith(".webp") && mood in moods) {
                        val temp = File(destination, ".$mood.importing")
                        var clipBytes = 0L
                        FileOutputStream(temp).use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                clipBytes += count
                                total += count
                                require(clipBytes <= MAX_CLIP_BYTES) { "$name exceeds 8 MB" }
                                require(total <= MAX_TOTAL_BYTES) { "Animation pack exceeds 80 MB" }
                                output.write(buffer, 0, count)
                            }
                        }
                        require(clipBytes > 0L) { "$name is empty" }
                        staged.add(temp)
                        imported++
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            require(imported > 0) { "ZIP must contain approved mood-name WebP files" }
            staged.forEach { temp ->
                val finalName = temp.name.removePrefix(".").removeSuffix(".importing") + ".webp"
                val target = File(destination, finalName)
                if (target.exists()) target.delete()
                check(temp.renameTo(target)) { "Could not install $finalName" }
            }
            return imported
        } catch (error: Exception) {
            staged.forEach { it.delete() }
            throw error
        }
    }

    fun installed(context: Context): List<String> =
        moods.filter { has(context, it) }
}
