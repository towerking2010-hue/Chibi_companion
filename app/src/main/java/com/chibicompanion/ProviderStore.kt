package com.chibicompanion

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Provider(val name: String, val key: String, val model: String, val endpoint: String = "")

object ProviderStore {
    private const val PREF = "provider_store"
    private const val ALIAS = "chibi_api_key_v1"

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
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, secretKey())
        val data = c.iv + c.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }
    private fun decrypt(value: String): String {
        val data = Base64.decode(value, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        return String(c.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
    }

    fun add(ctx: Context, name: String, key: String, model: String, endpoint: String = "") {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val count = p.getInt("count", 0)
        p.edit().putString("p${count}_name", name).putString("p${count}_key", encrypt(key))
            .putString("p${count}_model", model).putString("p${count}_endpoint", endpoint)
            .putInt("count", count + 1).apply()
    }
    fun all(ctx: Context): List<Provider> {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return (0 until p.getInt("count", 0)).mapNotNull { i ->
            val name = p.getString("p${i}_name", null) ?: return@mapNotNull null
            val key = p.getString("p${i}_key", null) ?: return@mapNotNull null
            runCatching { Provider(name, decrypt(key), p.getString("p${i}_model", "") ?: "", p.getString("p${i}_endpoint", "") ?: "") }.getOrNull()
        }
    }
    fun clear(ctx: Context) { ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply() }
    fun removeAt(ctx: Context, index: Int) {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val current = all(ctx).toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        val edit = p.edit().clear().putInt("count", current.size)
        current.forEachIndexed { i, provider ->
            edit.putString("p${i}_name", provider.name)
                .putString("p${i}_key", encrypt(provider.key))
                .putString("p${i}_model", provider.model)
                .putString("p${i}_endpoint", provider.endpoint)
        }
        edit.apply()
    }
}
