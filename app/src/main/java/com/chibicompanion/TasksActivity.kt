package com.chibicompanion

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

class TasksActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CompanionUi.bg
        window.navigationBarColor = CompanionUi.bg
        window.decorView.systemUiVisibility = 0

        val root = CompanionUi.column(this, 14).apply { setBackgroundColor(CompanionUi.bg) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val title = CompanionUi.column(this, 2)
        title.addView(CompanionUi.text(this, "MIKO • YOUR DAY", 10f, CompanionUi.pink, true))
        title.addView(CompanionUi.text(this, "Tasks & plans", 25f, CompanionUi.ink, true))
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "＋ Add", true) { addTask() })
        root.addView(header)
        root.addView(CompanionUi.text(this, "A simple private task list stored on this phone.", 12f, CompanionUi.muted))
        root.addView(CompanionUi.button(this, "Clear completed tasks") {
            val completed = TaskStore.all(this).count { it.done }
            if (completed == 0) toast("Nothing completed yet")
            else AlertDialog.Builder(this).setTitle("Clear completed tasks?")
                .setMessage("Remove $completed completed task(s)?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ -> TaskStore.clearCompleted(this); render() }
                .show()
        })
        val scroll = ScrollView(this).apply { isFillViewport = true }
        list = CompanionUi.column(this, 10)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(CompanionUi.button(this, "← Back to Miko") { finish() })
        setContentView(root)
        render()
    }

    private fun addTask() {
        val field = EditText(this).apply {
            hint = "What do you need to do?"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(CompanionUi.ink)
        }
        AlertDialog.Builder(this).setTitle("Add a task").setView(field)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                if (TaskStore.add(this, field.text.toString())) render()
                else toast("Give your task a name first")
            }.show()
    }

    private fun render() {
        list.removeAllViews()
        val tasks = TaskStore.all(this)
        val remaining = tasks.count { !it.done }
        list.addView(CompanionUi.text(this, "$remaining remaining • ${tasks.count { it.done }} completed", 12f, CompanionUi.pink, true))
        if (tasks.isEmpty()) {
            val empty = CompanionUi.column(this, 8)
            empty.addView(CompanionUi.text(this, "Nothing on your list yet. ♡", 17f, CompanionUi.ink, true))
            empty.addView(CompanionUi.text(this, "Tap Add and Miko will keep your tasks here.", 12f, CompanionUi.muted))
            list.addView(empty)
        }
        tasks.forEach { task ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = CompanionUi.panel(this@TasksActivity, android.graphics.Color.rgb(30,36,65), 16)
                setPadding(CompanionUi.dp(this@TasksActivity, 10), CompanionUi.dp(this@TasksActivity, 6),
                    CompanionUi.dp(this@TasksActivity, 6), CompanionUi.dp(this@TasksActivity, 6))
            }
            val check = CheckBox(this).apply {
                text = task.title
                isChecked = task.done
                textSize = 14f
                setTextColor(if (task.done) CompanionUi.muted else CompanionUi.ink)
                setOnCheckedChangeListener { _, checked -> TaskStore.setDone(this@TasksActivity, task.id, checked); render() }
            }
            row.addView(check, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(CompanionUi.button(this, "×") { TaskStore.delete(this, task.id); render() },
                LinearLayout.LayoutParams(CompanionUi.dp(this, 42), CompanionUi.dp(this, 44)))
            list.addView(row)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
