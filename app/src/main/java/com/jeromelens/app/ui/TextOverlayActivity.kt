package com.jeromelens.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
    }

    private lateinit var binding: ActivityTextOverlayBinding

    @Inject
    lateinit var ocrProcessor: OcrProcessor

    private val viewModel: TextOverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(EXTRA_SCREENSHOT_PATH)
        if (path == null) {
            Toast.makeText(this, "No screenshot path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(path)
                }
                if (bitmap == null) {
                    Toast.makeText(this@TextOverlayActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                binding.overlayView.setScreenshot(bitmap)

                val result = withContext(Dispatchers.Default) {
                    ocrProcessor.extractText(bitmap)
                }

                binding.overlayView.setTextBlocks(result.textBlocks)
                binding.progressBar.visibility = View.GONE
                binding.bottomBar.visibility = View.VISIBLE

                if (result.textBlocks.isEmpty()) {
                    binding.instructionText.text = "No text detected"
                }

                binding.overlayView.onSelectionChanged = { selected ->
                    if (selected.isNotBlank()) {
                        binding.selectedPreview.visibility = View.VISIBLE
                        binding.selectedPreview.text = selected
                    } else {
                        binding.selectedPreview.visibility = View.GONE
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
            viewModel.saveClip(selected, path)
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("JeromeLens", text))
    }
}
