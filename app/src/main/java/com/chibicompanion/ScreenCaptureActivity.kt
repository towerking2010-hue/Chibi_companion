package com.chibicompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream

/** Requests Android's one-time screen-sharing consent and captures a single frame. */
class ScreenCaptureActivity : Activity() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Android screen-capture consent callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        runCatching {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            // Android 14+ requires a callback before creating a virtual display.
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { }
            }, Handler(mainLooper))
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            display = projection?.createVirtualDisplay(
                "Chibi-one-shot-screen",
                width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface, null, Handler(Looper.getMainLooper())
            )
            Handler(Looper.getMainLooper()).postDelayed({ captureFrame() }, 700)
        }.onFailure {
            cleanup()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun captureFrame() {
        try {
            val image = reader?.acquireLatestImage() ?: throw IllegalStateException("No screen frame received")
            image.use {
                val plane = it.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * it.width
                val padded = Bitmap.createBitmap(
                    it.width + rowPadding / pixelStride, it.height, Bitmap.Config.ARGB_8888
                )
                padded.copyPixelsFromBuffer(buffer)
                val bitmap = Bitmap.createBitmap(padded, 0, 0, it.width, it.height)
                padded.recycle()
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 72, output)
                bitmap.recycle()
                ScreenCaptureStore.latestJpeg = output.toByteArray()
            }
            setResult(RESULT_OK)
        } catch (_: Exception) {
            ScreenCaptureStore.latestJpeg = null
            setResult(RESULT_CANCELED)
        } finally {
            cleanup()
            finish()
        }
    }

    private fun cleanup() {
        runCatching { display?.release() }
        runCatching { reader?.close() }
        runCatching { projection?.stop() }
        display = null
        reader = null
        projection = null
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    companion object { private const val REQUEST_CAPTURE = 701 }
}
