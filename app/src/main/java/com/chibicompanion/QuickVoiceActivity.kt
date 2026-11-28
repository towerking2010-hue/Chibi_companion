package com.chibicompanion

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast

class QuickVoiceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC)
        } else launchRecognizer()
    }
    private fun launchRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Miko")
        }
        try { startActivityForResult(intent, REQUEST_SPEECH) }
        catch (_: Exception) { Toast.makeText(this, "Speech recognition isn't available", Toast.LENGTH_SHORT).show(); finish() }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) launchRecognizer() else finish()
    }
    @Deprecated("Uses platform speech recognition activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) startService(Intent(this, FloatingCompanionService::class.java).apply {
                action = FloatingCompanionService.ACTION_VOICE_READY
                putExtra(FloatingCompanionService.EXTRA_VOICE_TEXT, words)
            })
        }
        finish()
    }
    companion object { private const val REQUEST_MIC = 901; private const val REQUEST_SPEECH = 902 }
}
