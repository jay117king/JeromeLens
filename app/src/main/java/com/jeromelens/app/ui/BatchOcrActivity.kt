package com.jeromelens.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jeromelens.app.databinding.ActivityBatchOcrBinding
import com.jeromelens.app.ocr.OcrProcessor
import com.jeromelens.app.util.Categories
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Processes up to [Categories.MAX_BATCH_IMAGES] user-selected images with OCR
 * and assigns them to a category before saving to history.
 */
@AndroidEntryPoint
class BatchOcrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URIS = "image_uris"
        private const val MAX_IMAGE_DIMENSION = 1920
    }

    private lateinit var binding: ActivityBatchOcrBinding

    @Inject
    lateinit var ocrProcessor: OcrProcessor

    private val viewModel: BatchOcrViewModel by viewModels()

    private var uris: List<Uri> = emptyList()
    private var currentIndex = 0
    private val results = mutableListOf<Pair<String, String?>>() // text to path/uri string

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatchOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.take(Categories.MAX_BATCH_IMAGES)
            ?: emptyList()

        if (uris.isEmpty()) {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupCategorySpinner()
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSaveAll.setOnClickListener { saveAll() }

        processNext()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Categories.PREDEFINED
        )
        binding.categorySpinner.adapter = adapter
        binding.categorySpinner.setSelection(0)
    }

    private fun processNext() {
        if (currentIndex >= uris.size) {
            binding.progressBar.visibility = View.GONE
            binding.statusText.text = "Done! ${results.size} image(s) processed. Choose category and save."
            binding.btnSaveAll.isEnabled = results.isNotEmpty()
            binding.previewText.text = results.joinToString("\n\n---\n\n") { it.first.take(200) }
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveAll.isEnabled = false
        binding.statusText.text = "Processing ${currentIndex + 1} of ${uris.size}…"

        lifecycleScope.launch {
            try {
                val uri = uris[currentIndex]
                val bitmap = withContext(Dispatchers.IO) { loadBitmap(uri) }
                if (bitmap == null) {
                    Toast.makeText(this@BatchOcrActivity, "Failed to load image ${currentIndex + 1}", Toast.LENGTH_SHORT).show()
                    currentIndex++
                    processNext()
                    return@launch
                }

                val result = withContext(Dispatchers.Default) {
                    ocrProcessor.extractText(bitmap)
                }

                val text = result.fullText.trim()
                if (text.isNotBlank()) {
                    results.add(text to uri.toString())
                    binding.previewText.text = text.take(500)
                } else {
                    Toast.makeText(this@BatchOcrActivity, "No text in image ${currentIndex + 1}", Toast.LENGTH_SHORT).show()
                }

                currentIndex++
                processNext()
            } catch (e: Exception) {
                Toast.makeText(this@BatchOcrActivity, "OCR error: ${e.message}", Toast.LENGTH_SHORT).show()
                currentIndex++
                processNext()
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                // Re-open stream for actual decode
                contentResolver.openInputStream(uri)?.use { stream2 ->
                    options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    BitmapFactory.decodeStream(stream2, null, options)
                }
            }
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

    private fun saveAll() {
        val category = binding.categorySpinner.selectedItem as? String ?: Categories.UNCATEGORIZED
        if (results.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveAll.isEnabled = false
        binding.statusText.text = "Saving…"

        viewModel.saveBatch(results, category) {
            Toast.makeText(this, "Saved ${results.size} clip(s) to \"$category\"", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
