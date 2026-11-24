package com.chibicompanion

/** One-shot screenshot bytes stay in memory and are cleared after the vision request. */
object ScreenCaptureStore {
    @Volatile var latestJpeg: ByteArray? = null
}
