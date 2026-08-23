package com.jeromelens.app.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jeromelens.app.databinding.ActivityMainBinding
import com.jeromelens.app.service.FloatingBubbleService
import com.jeromelens.app.service.ScreenshotDetectionService
import com.jeromelens.app.util.Categories
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bubbleRunning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions are needed for full functionality", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult

        val limited = uris.take(Categories.MAX_BATCH_IMAGES)
        if (uris.size > Categories.MAX_BATCH_IMAGES) {
            Toast.makeText(
                this,
                "Selected ${uris.size} images. Only the first ${Categories.MAX_BATCH_IMAGES} will be processed.",
                Toast.LENGTH_LONG
            ).show()
        }

        val intent = Intent(this, BatchOcrActivity::class.java).apply {
            putParcelableArrayListExtra(
                BatchOcrActivity.EXTRA_URIS,
                ArrayList(limited)
            )
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNeededPermissions()

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnEnableOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnToggleBubble.setOnClickListener {
            toggleBubble()
        }

        binding.btnUploadImages.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun toggleBubble() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant Overlay permission first", Toast.LENGTH_LONG).show()
            binding.btnEnableOverlay.visibility = View.VISIBLE
            return
        }
        if (bubbleRunning) {
            FloatingBubbleService.stop(this)
            bubbleRunning = false
            Toast.makeText(this, "Floating bubble stopped", Toast.LENGTH_SHORT).show()
        } else {
            FloatingBubbleService.start(this)
            bubbleRunning = true
            Toast.makeText(this, "Floating bubble started", Toast.LENGTH_SHORT).show()
        }
        updateStatus()
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun updateStatus() {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val accessibilityOn = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo.serviceInfo.packageName == packageName } ||
            ScreenshotDetectionService.isRunning

        binding.accessibilityStatus.text = if (accessibilityOn) {
            "Accessibility Enabled"
        } else {
            "Accessibility Not enabled"
        }
        binding.btnEnableAccessibility.visibility = if (accessibilityOn) View.GONE else View.VISIBLE

        val canDraw = Settings.canDrawOverlays(this)
        binding.overlayStatus.text = if (canDraw) {
            "Overlay Granted"
        } else {
            "Overlay Not granted"
        }
        binding.btnEnableOverlay.visibility = if (canDraw) View.GONE else View.VISIBLE

        binding.btnToggleBubble.text = if (bubbleRunning) {
            "Stop Floating Bubble"
        } else {
            "Start Floating Bubble"
        }
    }
}
