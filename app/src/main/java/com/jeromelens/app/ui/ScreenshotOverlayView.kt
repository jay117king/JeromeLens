package com.jeromelens.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jeromelens.app.ocr.TextBlock
import kotlin.math.min

class ScreenshotOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var textBlocks: List<TextBlock> = emptyList()
    private val selectedBlocks = mutableSetOf<Int>()
    private var scaleX = 1f
    private var scaleY = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private val highlightPaint = Paint().apply {
        color = Color.parseColor("#4D2196F3")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    var onSelectionChanged: ((String) -> Unit)? = null

    fun setScreenshot(bmp: Bitmap) {
        bitmap = bmp
        // Recalculate immediately if we already have size, otherwise wait for onSizeChanged
        if (width > 0 && height > 0) {
            calculateScale()
        }
        invalidate()
    }

    fun setTextBlocks(blocks: List<TextBlock>) {
        textBlocks = blocks
        selectedBlocks.clear()
        invalidate()
    }

    private fun calculateScale() {
        val bmp = bitmap ?: return
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()
        if (bmpW <= 0f || bmpH <= 0f) return

        val scale = min(viewW / bmpW, viewH / bmpH)
        scaleX = scale
        scaleY = scale
        offsetX = (viewW - bmpW * scale) / 2f
        offsetY = (viewH - bmpH * scale) / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateScale()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        // Re-ensure scale is correct (defensive)
        if (scaleX == 1f && width > 0 && bmp.width != width) {
            calculateScale()
        }

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleX, scaleY)
        canvas.drawBitmap(bmp, 0f, 0f, null)

        selectedBlocks.forEach { index ->
            textBlocks.getOrNull(index)?.boundingBox?.let { box ->
                canvas.drawRect(box, highlightPaint)
                canvas.drawRect(box, borderPaint)
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Convert view coordinates → bitmap coordinates
                val x = ((event.x - offsetX) / scaleX).toInt()
                val y = ((event.y - offsetY) / scaleY).toInt()

                textBlocks.forEachIndexed { index, block ->
                    val box = block.boundingBox
                    if (box != null && box.contains(x, y)) {
                        if (selectedBlocks.contains(index)) {
                            selectedBlocks.remove(index)
                        } else {
                            selectedBlocks.add(index)
                        }
                        invalidate()
                        onSelectionChanged?.invoke(getSelectedText())
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun getSelectedText(): String {
        return selectedBlocks.sorted()
            .mapNotNull { textBlocks.getOrNull(it)?.text }
            .joinToString("\n")
    }

    fun selectAll() {
        selectedBlocks.clear()
        textBlocks.indices.forEach { selectedBlocks.add(it) }
        invalidate()
        onSelectionChanged?.invoke(getSelectedText())
    }
}
