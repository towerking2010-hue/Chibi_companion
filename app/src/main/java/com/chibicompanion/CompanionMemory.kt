package com.chibicompanion

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small, user-controlled, encrypted long-term memory stored only on this device. */
object CompanionMemory {
    private const val PREF = "companion_memory_v1"
    private const val ALIAS = "chibi_memory_key_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private fun seal(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun open(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }

    @Synchronized fun add(ctx: Context, value: String): Boolean {
        val clean = value.trim().take(500)
        if (clean.length < 3) return false
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val items = read(ctx).toMutableList()
        if (items.any { it.equals(clean, true) }) return true
        items.add(clean)
        val array = JSONArray()
        items.takeLast(100).forEach { array.put(it) }
        prefs.edit().putString("items", seal(array.toString())).apply()
        return true
    }

    fun all(ctx: Context): List<String> {
        val encoded = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("items", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(open(encoded))
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun read(ctx: Context) = all(ctx)

    fun remove(ctx: Context, index: Int) {
        val items = all(ctx).toMutableList()
        if (index !in items.indices) return
        items.removeAt(index)
        val array = JSONArray()
        items.forEach { array.put(it) }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("items", seal(array.toString())).apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("items").apply()
    }

    fun promptContext(ctx: Context): String =
        all(ctx).takeLast(30).joinToString("\n") { "- $it" }

    /** Only explicit remember requests are saved automatically. */
    fun handleExplicitRequest(ctx: Context, message: String): String? {
        val lower = message.trim().lowercase()
        val prefix = when {
            lower.startsWith("remember that ") -> message.trim().substring(14)
            lower.startsWith("remember ") -> message.trim().substring(9)
            lower.startsWith("don't forget ") -> message.trim().substring(13)
            else -> return null
        }
        if (prefix.isBlank()) return "Tell me exactly what you want me to remember."
        add(ctx, prefix)
        return "Got it. I saved that in my private on-device memory. You can review or delete it in Memory settings. ♡"
    }
}
