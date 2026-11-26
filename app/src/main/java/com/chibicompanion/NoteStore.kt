package com.chibicompanion

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class MikoNote(val id: String, val title: String, val body: String, val updatedAt: Long)

object NoteStore {
    private const val PREFS = "miko_notes_v1"
    private const val KEY = "items"
    private const val ALIAS = "miko_notes_aes_v1"

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        gen.init(android.security.keystore.KeyGenParameterSpec.Builder(
            ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return gen.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val bytes = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size > 12) { "Invalid encrypted notes" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }

    @Synchronized fun all(context: Context): List<MikoNote> {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(decrypt(encoded))
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                MikoNote(item.getString("id"), item.getString("title"), item.optString("body"), item.optLong("updatedAt"))
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    @Synchronized fun save(context: Context, id: String?, title: String, body: String): Boolean {
        val cleanTitle = title.trim().take(100)
        val cleanBody = body.trim().take(12000)
        if (cleanTitle.isBlank() && cleanBody.isBlank()) return false
        val notes = all(context).toMutableList()
        val noteId = id ?: UUID.randomUUID().toString()
        val finalTitle = cleanTitle.ifBlank { cleanBody.lineSequence().firstOrNull()?.take(60) ?: "Untitled note" }
        val note = MikoNote(noteId, finalTitle, cleanBody, System.currentTimeMillis())
        val index = notes.indexOfFirst { it.id == noteId }
        if (index >= 0) notes[index] = note else notes.add(note)
        persist(context, notes.take(300))
        return true
    }

    @Synchronized fun delete(context: Context, id: String) {
        persist(context, all(context).filterNot { it.id == id })
    }

    private fun persist(context: Context, notes: List<MikoNote>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(JSONObject().put("id", note.id).put("title", note.title)
                .put("body", note.body).put("updatedAt", note.updatedAt))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, encrypt(array.toString())).apply()
    }
}
