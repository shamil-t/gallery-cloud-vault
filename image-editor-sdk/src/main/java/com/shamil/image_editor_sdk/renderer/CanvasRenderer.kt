package com.shamil.image_editor_sdk.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import com.shamil.image_editor_sdk.core.domain.Adjustments
import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.Layer

/**
 * Software-based renderer using Android's [Canvas] and [ColorMatrix].
 * Used as a fallback or for quick previews on older devices.
 */
class CanvasRenderer(private val sourceBitmap: Bitmap) : ImageRenderer {

    override fun draw(canvas: Canvas, state: EditorState, width: Float, height: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Find adjustments
        val adj = state.layers.filterIsInstance<Layer.AdjustmentLayer>().firstOrNull()?.adjustments
        if (adj != null) {
            paint.colorFilter = createAdjustmentFilter(adj)
        }

        val bmpW = sourceBitmap.width.toFloat()
        val bmpH = sourceBitmap.height.toFloat()
        
        val cropX = state.cropRect.left * bmpW
        val cropY = state.cropRect.top * bmpH
        val cropW = (state.cropRect.right - state.cropRect.left) * bmpW
        val cropH = (state.cropRect.bottom - state.cropRect.top) * bmpH

        canvas.save()
        
        // 1. Move to canvas center
        canvas.translate(width / 2f, height / 2f)
        
        // 2. Scale to fit
        val rotation = state.transformations.rotationDegrees
        val isRotatedOdd = (rotation / 90f).toInt() % 2 != 0
        val scale = if (isRotatedOdd) {
            minOf(width / cropH, height / cropW)
        } else {
            minOf(width / cropW, height / cropH)
        }
        canvas.scale(scale, scale)

        // 3. Flip
        val flipX = if (state.transformations.isFlippedHorizontal) -1f else 1f
        val flipY = if (state.transformations.isFlippedVertical) -1f else 1f
        canvas.scale(flipX, flipY)

        // 4. Rotate
        canvas.rotate(rotation)

        // 5. Move crop center to 0,0
        canvas.translate(-(cropX + cropW / 2f), -(cropY + cropH / 2f))

        state.layers.sortedBy { it.zIndex }.forEach { layer ->
            if (!layer.isVisible) return@forEach

            when (layer) {
                is Layer.ImageLayer -> {
                    canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
                }
                is Layer.AdjustmentLayer -> {}
                is Layer.FilterLayer -> {}
                is Layer.DrawingLayer -> {}
            }
        }
        canvas.restore()
    }

    private fun createAdjustmentFilter(adj: Adjustments): ColorMatrixColorFilter {
        val cm = ColorMatrix()
        
        // Brightness
        val b = adj.brightness * 255
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )))

        // Contrast
        val c = adj.contrast
        val t = (1.0f - c) / 2.0f * 255.0f
        cm.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )))

        // Saturation
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(adj.saturation)
        cm.postConcat(saturationMatrix)
        
        // Exposure (Approximate with scale)
        val e = Math.pow(2.0, adj.exposure.toDouble()).toFloat()
        cm.postConcat(ColorMatrix(floatArrayOf(
            e, 0f, 0f, 0f, 0f,
            0f, e, 0f, 0f, 0f,
            0f, 0f, e, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))

        // Temperature & Tint (Simple Approximation)
        val temp = adj.temperature * 0.1f
        val tint = adj.tint * 0.1f
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f + temp, 0f, 0f, 0f, 0f,
            0f, 1f + tint, 0f, 0f, 0f,
            0f, 0f, 1f - temp, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))

        return ColorMatrixColorFilter(cm)
    }

    override fun renderPreview(state: EditorState): Bitmap? {
        val cropWidth = ((state.cropRect.right - state.cropRect.left) * sourceBitmap.width).toInt()
        val cropHeight = ((state.cropRect.bottom - state.cropRect.top) * sourceBitmap.height).toInt()
        val output = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        draw(canvas, state, cropWidth.toFloat(), cropHeight.toFloat())
        return output
    }

    override fun renderFull(state: EditorState): Bitmap? = renderPreview(state)

    override fun renderThumbnail(state: EditorState, size: Int): Bitmap? {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        draw(canvas, state, size.toFloat(), size.toFloat())
        return output
    }

    override fun release() {}
}
