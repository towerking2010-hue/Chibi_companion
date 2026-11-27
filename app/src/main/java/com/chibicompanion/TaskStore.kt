package com.chibicompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MikoTask(val id: String, val title: String, val createdAt: Long, val done: Boolean)

object TaskStore {
    private const val PREFS = "miko_tasks_v1"
    private const val KEY = "items"

    @Synchronized fun all(context: Context): List<MikoTask> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                MikoTask(item.optString("id"), item.optString("title"), item.optLong("createdAt"), item.optBoolean("done"))
            }.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .sortedWith(compareBy<MikoTask> { it.done }.thenByDescending { it.createdAt })
        }.getOrDefault(emptyList())
    }

    @Synchronized fun add(context: Context, title: String): Boolean {
        val clean = title.trim().take(180)
        if (clean.isBlank()) return false
        val tasks = all(context).toMutableList()
        if (tasks.any { !it.done && it.title.equals(clean, true) }) return true
        tasks.add(MikoTask(UUID.randomUUID().toString(), clean, System.currentTimeMillis(), false))
        save(context, tasks.takeLast(200))
        return true
    }

    @Synchronized fun setDone(context: Context, id: String, done: Boolean) {
        save(context, all(context).map { if (it.id == id) it.copy(done = done) else it })
    }

    @Synchronized fun delete(context: Context, id: String) {
        save(context, all(context).filterNot { it.id == id })
    }

    @Synchronized fun clearCompleted(context: Context) {
        save(context, all(context).filterNot { it.done })
    }

    private fun save(context: Context, tasks: List<MikoTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().put("id", task.id).put("title", task.title)
                .put("createdAt", task.createdAt).put("done", task.done))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
