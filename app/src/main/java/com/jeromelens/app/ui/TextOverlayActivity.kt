package com.jeromelens.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jeromelens.app.R
import com.jeromelens.app.databinding.ActivityTextOverlayBinding
import com.jeromelens.app.ocr.OcrProcessor
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

                // Force a layout pass so scale is correct before drawing highlights
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

                binding.overlayView.onSelectionChanged = { selected ->
                    if (selected.isNotBlank()) {
                        binding.selectedPreview.visibility = View.VISIBLE
                        binding.selectedPreview.text = selected
                    } else {
                        binding.selectedPreview.visibility = View.GONE
                    }
                }

                // Auto-select when there are only a few blocks
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

    private fun decodeSampledBitmap(path: String): Bitmap? {
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // lower memory

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
