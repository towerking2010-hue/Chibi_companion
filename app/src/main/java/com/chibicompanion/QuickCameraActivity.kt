package com.chibicompanion

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

class QuickCameraActivity : Activity() {
    private var output: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Miko_" + System.currentTimeMillis() + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Miko")
        }
        output = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (output == null) { Toast.makeText(this, "Couldn't prepare the camera photo", Toast.LENGTH_SHORT).show(); finish(); return }
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, output)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(contentResolver, "Miko photo", output)
        }
        try { startActivityForResult(camera, REQUEST_CAMERA) }
        catch (_: Exception) { output?.let { contentResolver.delete(it, null, null) }; Toast.makeText(this, "No camera app is available", Toast.LENGTH_SHORT).show(); finish() }
    }
    @Deprecated("Uses platform camera activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = output
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK && uri != null) {
            startService(Intent(this, FloatingCompanionService::class.java).apply {
                action = FloatingCompanionService.ACTION_ATTACHMENT_READY
                this.data = uri
                putExtra(FloatingCompanionService.EXTRA_ATTACHMENT_NAME, "Camera photo")
                putExtra(FloatingCompanionService.EXTRA_ATTACHMENT_MIME, "image/jpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } else uri?.let { contentResolver.delete(it, null, null) }
        finish()
    }
    companion object { private const val REQUEST_CAMERA = 903 }
}
