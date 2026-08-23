package com.jeromelens.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.jeromelens.app.ui.TextOverlayActivity
import java.io.File

class ScreenshotDetectionService : AccessibilityService() {

    companion object {
        private const val TAG = "ScreenshotDetector"
        @Volatile
        var isRunning = false
    }

    private var fileObserver: FileObserver? = null
    private val screenshotDirs = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/Screenshots",
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/Screenshots",
        "/storage/emulated/0/Pictures/Screenshots",
        "/storage/emulated/0/DCIM/Screenshots"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        startWatching()
        Log.i(TAG, "ScreenshotDetectionService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        fileObserver?.stopWatching()
        fileObserver = null
        Log.i(TAG, "Service destroyed")
    }

    private fun startWatching() {
        val existingDirs = screenshotDirs.filter { File(it).exists() }
        if (existingDirs.isEmpty()) {
            Log.w(TAG, "No screenshot directories found")
            return
        }

        val path = existingDirs.first()
        fileObserver = object : FileObserver(path, CREATE or CLOSE_WRITE or MOVED_TO) {
            override fun onEvent(event: Int, relativePath: String?) {
                if (relativePath == null) return
                val lower = relativePath.lowercase()
                if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                    val file = File(path, relativePath)
                    if (file.exists() && file.length() > 1000) {
                        Log.i(TAG, "New screenshot detected: ${file.absolutePath}")
                        processScreenshot(file.absolutePath)
                    }
                }
            }
        }
        fileObserver?.startWatching()
        Log.i(TAG, "Watching: $path")
    }

    private fun processScreenshot(path: String) {
        android.os.Handler(mainLooper).postDelayed({
            val intent = Intent(this, TextOverlayActivity::class.java).apply {
                putExtra(TextOverlayActivity.EXTRA_SCREENSHOT_PATH, path)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }, 400)
    }
}
