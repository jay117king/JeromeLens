package com.jeromelens.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.jeromelens.app.ui.TextOverlayActivity
import java.io.File

/**
 * Hybrid screenshot detector:
 * 1. Primary: MediaStore ContentObserver (most reliable on modern Android)
 * 2. Secondary: FileObserver on common Screenshots folders
 * Includes debouncing to avoid multiple triggers for the same screenshot.
 */
class ScreenshotDetectionService : AccessibilityService() {

    companion object {
        private const val TAG = "ScreenshotDetector"
        private const val DEBOUNCE_MS = 1200L
        @Volatile
        var isRunning = false
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaObserver: ContentObserver? = null
    private var fileObserver: FileObserver? = null

    private var lastProcessedPath: String? = null
    private var lastProcessedTime = 0L

    private val screenshotDirs = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/Screenshots",
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/Screenshots",
        "/storage/emulated/0/Pictures/Screenshots",
        "/storage/emulated/0/DCIM/Screenshots",
        "/storage/emulated/0/Pictures/Screenshot",
        "/storage/emulated/0/DCIM/Screenshot"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        startMediaStoreObserver()
        startFileObservers()
        Log.i(TAG, "ScreenshotDetectionService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for detection; kept for service lifecycle
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        mediaObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        mediaObserver = null
        fileObserver?.stopWatching()
        fileObserver = null
        mainHandler.removeCallbacksAndMessages(null)
        Log.i(TAG, "Service destroyed")
    }

    private fun startMediaStoreObserver() {
        mediaObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Query the latest image that looks like a screenshot
                queryLatestScreenshot()
            }
        }
        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver!!
            )
            Log.i(TAG, "MediaStore ContentObserver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register MediaStore observer", e)
        }
    }

    private fun queryLatestScreenshot() {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )

            // Look at very recent images (last 10 seconds)
            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
            val selectionArgs = arrayOf(((System.currentTimeMillis() / 1000) - 10).toString())

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                    if (dataIndex >= 0) {
                        val path = cursor.getString(dataIndex) ?: return
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                        val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                        val lowerName = name.lowercase()
                        val lowerPath = path.lowercase()

                        // Heuristic: name or path contains "screenshot"
                        val isScreenshot = lowerName.contains("screenshot") ||
                                lowerPath.contains("screenshot") ||
                                lowerName.startsWith("img_") // some devices

                        if (isScreenshot && size > 5000) {
                            maybeProcess(path)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryLatestScreenshot failed", e)
        }
    }

    private fun startFileObservers() {
        val existingDirs = screenshotDirs.filter { File(it).exists() && File(it).isDirectory }
        if (existingDirs.isEmpty()) {
            Log.w(TAG, "No classic screenshot directories found (MediaStore still active)")
            return
        }

        // Watch the most common one; FileObserver is secondary
        val path = existingDirs.first()
        try {
            fileObserver = object : FileObserver(path, CREATE or CLOSE_WRITE or MOVED_TO) {
                override fun onEvent(event: Int, relativePath: String?) {
                    if (relativePath == null) return
                    val lower = relativePath.lowercase()
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) {
                        val file = File(path, relativePath)
                        if (file.exists() && file.length() > 5000) {
                            maybeProcess(file.absolutePath)
                        }
                    }
                }
            }
            fileObserver?.startWatching()
            Log.i(TAG, "FileObserver watching: $path")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FileObserver", e)
        }
    }

    private fun maybeProcess(path: String) {
        val now = System.currentTimeMillis()
        // Debounce: same path or any path within DEBOUNCE_MS
        if (path == lastProcessedPath && (now - lastProcessedTime) < DEBOUNCE_MS) {
            return
        }
        if ((now - lastProcessedTime) < 800) {
            // Too soon after last trigger
            return
        }

        lastProcessedPath = path
        lastProcessedTime = now

        Log.i(TAG, "New screenshot detected: $path")
        processScreenshot(path)
    }

    private fun processScreenshot(path: String) {
        // Small delay so the file is fully flushed to disk
        mainHandler.postDelayed({
            try {
                val intent = Intent(this, TextOverlayActivity::class.java).apply {
                    putExtra(TextOverlayActivity.EXTRA_SCREENSHOT_PATH, path)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch TextOverlayActivity", e)
            }
        }, 500)
    }
}
