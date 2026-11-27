package com.chibicompanion

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var search: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CompanionUi.applySystemBars(this)

        val root = CompanionUi.column(this, 18).apply {
            setBackgroundColor(CompanionUi.bg)
            setPadding(CompanionUi.dp(this@NotesActivity, 20), CompanionUi.dp(this@NotesActivity, 18),
                CompanionUi.dp(this@NotesActivity, 20), CompanionUi.dp(this@NotesActivity, 24))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titles = CompanionUi.column(this, 0)
        titles.addView(CompanionUi.eyebrow(this, "Your space"))
        titles.addView(CompanionUi.heading(this, "Private notes", 25f))
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(CompanionUi.button(this, "＋ New", true) { editNote(null) })
        root.addView(header)
        search = EditText(this).apply {
            hint = "Search your notes…"
            setSingleLine(true)
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
            background = CompanionUi.panel(this@NotesActivity, android.graphics.Color.rgb(37,43,76), 16)
            setPadding(CompanionUi.dp(this@NotesActivity, 14), CompanionUi.dp(this@NotesActivity, 12),
                CompanionUi.dp(this@NotesActivity, 14), CompanionUi.dp(this@NotesActivity, 12))
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render() }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search)
        val privacy = CompanionUi.card(this, 13)
        privacy.addView(CompanionUi.text(this, "🔒  Private on this device", 13f, CompanionUi.success, true))
        privacy.addView(CompanionUi.text(this, "Notes are encrypted with Android Keystore and aren't synced to a cloud service.", 11f, CompanionUi.muted))
        root.addView(privacy)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        list = CompanionUi.column(this, 2)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(CompanionUi.button(this, "← Back to Miko") { finish() })
        setContentView(root)
        render()
    }

    private fun render() {
        list.removeAllViews()
        val query = search.text.toString().trim()
        val notes = NoteStore.all(this).filter {
            query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true)
        }
        if (notes.isEmpty()) {
            list.addView(CompanionUi.text(this,
                if (query.isBlank()) "No notes yet. Tap New to save something important. ♡" else "No notes match that search.",
                14f, CompanionUi.muted))
            return
        }
        notes.forEach { note ->
            val card = CompanionUi.column(this, 14).apply {
                background = CompanionUi.panel(this@NotesActivity, CompanionUi.surface, 18)
                setOnClickListener { editNote(note) }
                isClickable = true
                isFocusable = true
            }
            card.addView(CompanionUi.text(this, note.title, 16f, CompanionUi.ink, true))
            if (note.body.isNotBlank()) card.addView(CompanionUi.text(this, note.body.take(260), 12f, CompanionUi.muted))
            val date = SimpleDateFormat("d MMM • h:mm a", Locale.getDefault()).format(Date(note.updatedAt))
            val footer = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            footer.addView(CompanionUi.text(this, date, 10f, CompanionUi.muted), LinearLayout.LayoutParams(0, -2, 1f))
            footer.addView(CompanionUi.button(this, "Delete") {
                AlertDialog.Builder(this).setTitle("Delete note?")
                    .setMessage(note.title).setNegativeButton("Keep", null)
                    .setPositiveButton("Delete") { _, _ -> NoteStore.delete(this, note.id); render() }.show()
            })
            card.addView(footer)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = CompanionUi.dp(this, 10)
            list.addView(card, lp)
        }
    }

    private fun editNote(note: MikoNote?) {
        val form = CompanionUi.column(this, 8)
        val title = EditText(this).apply {
            hint = "Title"
            setText(note?.title.orEmpty())
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val body = EditText(this).apply {
            hint = "Write your note…"
            setText(note?.body.orEmpty())
            setTextColor(CompanionUi.ink)
            setHintTextColor(CompanionUi.muted)
            minLines = 5
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        form.addView(title)
        form.addView(body)
        AlertDialog.Builder(this).setTitle(if (note == null) "New note" else "Edit note")
            .setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                if (NoteStore.save(this, note?.id, title.text.toString(), body.text.toString())) render()
                else toast("Write a title or some note text first")
            }.show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
