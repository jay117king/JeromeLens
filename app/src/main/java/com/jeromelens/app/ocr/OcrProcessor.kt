package com.jeromelens.app.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class TextElement(
    val text: String,
    val boundingBox: Rect?
)

data class TextLine(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<TextElement>
)

data class TextBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<TextLine>
)

data class ExtractedTextResult(
    val fullText: String,
    val textBlocks: List<TextBlock>
)

@Singleton
class OcrProcessor @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): ExtractedTextResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(inputImage).await()

        val blocks = visionText.textBlocks.map { block ->
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox,
                lines = block.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox,
                        elements = line.elements.map { element ->
                            TextElement(
                                text = element.text,
                                boundingBox = element.boundingBox
                            )
                        }
                    )
                }
            )
        }

        return ExtractedTextResult(
            fullText = visionText.text,
            textBlocks = blocks
        )
    }

    fun close() {
        recognizer.close()
    }
}
