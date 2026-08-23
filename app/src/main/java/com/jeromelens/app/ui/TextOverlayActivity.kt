package com.jeromelens.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.jeromelens.app.R
import com.jeromelens.app.databinding.ActivityTextOverlayBinding
import com.jeromelens.app.ocr.OcrProcessor
import com.jeromelens.app.util.DetectedEntity
import com.jeromelens.app.util.EntityType
import com.jeromelens.app.util.SmartEntityParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TextOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
        private const val MAX_IMAGE_DIMENSION = 1920
    }

    private lateinit var binding: ActivityTextOverlayBinding

    @Inject
    lateinit var ocrProcessor: OcrProcessor

    private val viewModel: TextOverlayViewModel by viewModels()

    private var screenshotPath: String? = null
    private var currentEntities: List<DetectedEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        screenshotPath = intent.getStringExtra(EXTRA_SCREENSHOT_PATH)
        if (screenshotPath == null) {
            Toast.makeText(this, "No screenshot path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(screenshotPath!!)
                }
                if (bitmap == null) {
                    Toast.makeText(this@TextOverlayActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                binding.overlayView.setScreenshot(bitmap)

                binding.overlayView.post {
                    binding.overlayView.requestLayout()
                }

                val result = withContext(Dispatchers.Default) {
                    ocrProcessor.extractText(bitmap)
                }

                binding.overlayView.setTextBlocks(result.textBlocks)
                binding.progressBar.visibility = View.GONE
                binding.bottomBar.visibility = View.VISIBLE

                if (result.textBlocks.isEmpty()) {
                    binding.instructionText.text = "No text detected"
                } else {
                    binding.instructionText.text = getString(R.string.select_text)
                }

                // Smart entity detection on full text
                currentEntities = SmartEntityParser.parse(result.fullText)
                renderSmartActions(currentEntities)

                binding.overlayView.onSelectionChanged = { selected ->
                    if (selected.isNotBlank()) {
                        binding.selectedPreview.visibility = View.VISIBLE
                        binding.selectedPreview.text = selected
                        // Re-parse selection for more precise actions
                        val selectionEntities = SmartEntityParser.parse(selected)
                        if (selectionEntities.isNotEmpty()) {
                            renderSmartActions(selectionEntities)
                        }
                    } else {
                        binding.selectedPreview.visibility = View.GONE
                        renderSmartActions(currentEntities)
                    }
                }

                if (result.textBlocks.size in 1..8) {
                    binding.overlayView.selectAll()
                }

            } catch (e: Exception) {
                Toast.makeText(this@TextOverlayActivity, "OCR failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        binding.btnCopy.setOnClickListener {
            val selected = binding.overlayView.getSelectedText()
            if (selected.isBlank()) {
                Toast.makeText(this, "Select some text first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            copyToClipboard(selected)
            viewModel.saveClip(selected, screenshotPath)
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun renderSmartActions(entities: List<DetectedEntity>) {
        // Find or create a container for action chips. For robustness we reuse bottomBar parent.
        val container = binding.bottomBar.parent as? LinearLayout ?: return

        // Remove previous dynamic action buttons (tagged)
        val toRemove = mutableListOf<View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.tag == "smart_action") toRemove.add(child)
        }
        toRemove.forEach { container.removeView(it) }

        if (entities.isEmpty()) return

        // Add up to 4 primary actions
        entities.take(4).forEach { entity ->
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = when (entity.type) {
                    EntityType.URL -> "Open: ${entity.value.take(28)}"
                    EntityType.EMAIL -> "Email: ${entity.value}"
                    EntityType.PHONE -> "Call: ${entity.value}"
                    EntityType.CODE_BLOCK -> "Copy Code"
                    else -> SmartEntityParser.primaryActionLabel(entity.type)
                }
                tag = "smart_action"
                isAllCaps = false
                setOnClickListener { performEntityAction(entity) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            container.addView(btn, 0, lp) // insert near top of bottom area
        }
    }

    private fun performEntityAction(entity: DetectedEntity) {
        try {
            when (entity.type) {
                EntityType.URL -> {
                    var url = entity.value
                    if (!url.startsWith("http")) url = "https://$url"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                EntityType.EMAIL -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${entity.value}")
                    }
                    startActivity(intent)
                }
                EntityType.PHONE -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${entity.value.filter { it.isDigit() || it == '+' }}")
                    }
                    startActivity(intent)
                }
                EntityType.CODE_BLOCK, EntityType.PLAIN, EntityType.ADDRESS_LIKE -> {
                    copyToClipboard(entity.value)
                    viewModel.saveClip(entity.value, screenshotPath)
                    Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Action failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodeSampledBitmap(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("JeromeLens", text))
    }
}
