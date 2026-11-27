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
        CompanionUi.applySystemBars(this)

        val root = CompanionUi.column(this, 18).apply {
            setBackgroundColor(CompanionUi.bg)
            setPadding(CompanionUi.dp(this@TasksActivity, 20), CompanionUi.dp(this@TasksActivity, 18),
                CompanionUi.dp(this@TasksActivity, 20), CompanionUi.dp(this@TasksActivity, 24))
        }
        val header = CompanionUi.row(this)
        val title = CompanionUi.column(this, 0)
        title.addView(CompanionUi.eyebrow(this, "Your day"))
        title.addView(CompanionUi.heading(this, "Tasks & plans", 25f))
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "+ Add", true) { addTask() })
        root.addView(header)

        val summary = CompanionUi.card(this, 16)
        summary.addView(CompanionUi.text(this, "A little progress adds up.", 15f, CompanionUi.ink, true))
        summary.addView(CompanionUi.text(this, "Your tasks are saved privately on this phone.", 12f, CompanionUi.muted))
        root.addView(summary)

        val tools = CompanionUi.row(this)
        tools.addView(CompanionUi.button(this, "Clear completed") {
            val completed = TaskStore.all(this).count { it.done }
            if (completed == 0) toast("Nothing completed yet")
            else AlertDialog.Builder(this).setTitle("Clear completed tasks?")
                .setMessage("Remove $completed completed task(s)?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ -> TaskStore.clearCompleted(this); render() }
                .show()
        })
        root.addView(tools)

        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        list = CompanionUi.column(this, 2)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(CompanionUi.button(this, "←  Back to Miko") { finish() })
        setContentView(root)
        render()
    }

    private fun addTask() {
        val field = EditText(this).apply {
            hint = "What do you need to do?"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
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
        val completed = tasks.count { it.done }
        list.addView(CompanionUi.eyebrow(this, "Your list  •  $remaining remaining  •  $completed done"))
        CompanionUi.spacer(this, 8).also { list.addView(it) }
        if (tasks.isEmpty()) {
            val empty = CompanionUi.card(this, 18)
            empty.addView(CompanionUi.text(this, "A fresh start ✦", 18f, CompanionUi.ink, true))
            empty.addView(CompanionUi.text(this, "Add your first task and we'll keep it here for you.", 13f, CompanionUi.muted))
            list.addView(empty)
        }
        tasks.forEach { task ->
            val row = CompanionUi.row(this).apply {
                background = CompanionUi.panel(this@TasksActivity, CompanionUi.surface, 17)
                setPadding(CompanionUi.dp(this@TasksActivity, 10), CompanionUi.dp(this@TasksActivity, 7),
                    CompanionUi.dp(this@TasksActivity, 7), CompanionUi.dp(this@TasksActivity, 7))
            }
            val check = CheckBox(this).apply {
                text = task.title
                isChecked = task.done
                textSize = 14f
                setTextColor(if (task.done) CompanionUi.muted else CompanionUi.ink)
                buttonTintList = android.content.res.ColorStateList.valueOf(CompanionUi.lavender)
                setOnCheckedChangeListener { _, checked ->
                    TaskStore.setDone(this@TasksActivity, task.id, checked)
                    render()
                }
            }
            row.addView(check, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(CompanionUi.button(this, "×") {
                TaskStore.delete(this, task.id)
                render()
            }, LinearLayout.LayoutParams(CompanionUi.dp(this, 42), CompanionUi.dp(this, 44)))
            val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = CompanionUi.dp(this@TasksActivity, 8) }
            list.addView(row, lp)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
