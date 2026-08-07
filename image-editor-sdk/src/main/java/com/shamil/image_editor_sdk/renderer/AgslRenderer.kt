package com.shamil.image_editor_sdk.renderer

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.Layer

/**
 * GPU-accelerated renderer using AGSL (Android Graphics Shading Language).
 * High performance, low latency, and professional-grade color math.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AgslRenderer(private val sourceBitmap: Bitmap) : ImageRenderer {

    private val shaderCode = """
        uniform shader image;
        uniform float brightness;
        uniform float contrast;
        uniform float saturation;
        uniform float exposure;
        uniform float temperature;
        uniform float tint;
        uniform int filterType; // 0: None, 1: Vintage, 2: Mono, 3: Cinema
        uniform float filterIntensity;

        half4 main(float2 fragCoord) {
            half4 color = image.eval(fragCoord);
            
            // 1. Exposure
            color.rgb *= exp2(exposure);
            
            // 2. Brightness
            color.rgb += brightness;
            
            // 3. Contrast
            color.rgb = (color.rgb - 0.5) * contrast + 0.5;
            
            // 4. Temperature & Tint
            color.r += temperature * 0.15;
            color.b -= temperature * 0.15;
            color.g += tint * 0.1;
            
            // 5. Saturation
            half luma = dot(color.rgb, half3(0.2126, 0.7152, 0.0722));
            color.rgb = mix(half3(luma), color.rgb, saturation);
            
            // 6. Filters
            half3 filtered = color.rgb;
            if (filterType == 1) { // Vintage
                filtered = color.rgb * half3(1.1, 1.0, 0.8) + half3(0.1, 0.05, 0.0);
            } else if (filterType == 2) { // Mono
                filtered = half3(luma);
            } else if (filterType == 3) { // Cinema
                filtered = color.rgb * half3(0.8, 1.2, 1.1);
            }
            
            color.rgb = mix(color.rgb, filtered, filterIntensity);
            
            return color;
        }
    """.trimIndent()

    private val shader = RuntimeShader(shaderCode)
    private val paint = Paint().apply {
        shader = this@AgslRenderer.shader
    }

    override fun draw(canvas: Canvas, state: EditorState, width: Float, height: Float) {
        val adjLayer = state.layers.filterIsInstance<Layer.AdjustmentLayer>().firstOrNull()
        val adj = adjLayer?.adjustments
        val filterLayer = state.layers.filterIsInstance<Layer.FilterLayer>().firstOrNull()

        val srcShader = BitmapShader(sourceBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = android.graphics.Matrix()

        // 1. Center the bitmap in its own coordinate space
        val bmpW = sourceBitmap.width.toFloat()
        val bmpH = sourceBitmap.height.toFloat()
        
        // 2. Apply Crop (move crop center to 0,0)
        val cropX = state.cropRect.left * bmpW
        val cropY = state.cropRect.top * bmpH
        val cropW = (state.cropRect.right - state.cropRect.left) * bmpW
        val cropH = (state.cropRect.bottom - state.cropRect.top) * bmpH
        
        matrix.postTranslate(-(cropX + cropW / 2f), -(cropY + cropH / 2f))
        
        // 3. Rotate
        matrix.postRotate(state.transformations.rotationDegrees)
        
        // 4. Flip
        val flipX = if (state.transformations.isFlippedHorizontal) -1f else 1f
        val flipY = if (state.transformations.isFlippedVertical) -1f else 1f
        matrix.postScale(flipX, flipY)
        
        // 5. Scale to fit target dimensions
        // Note: width/height passed here are the VISIBLE dimensions.
        // If rotated 90, we need to scale so that cropH fits width and cropW fits height.
        val rotation = state.transformations.rotationDegrees
        val isRotatedOdd = (rotation / 90f).toInt() % 2 != 0
        
        val scale = if (isRotatedOdd) {
            minOf(width / cropH, height / cropW)
        } else {
            minOf(width / cropW, height / cropH)
        }
        
        matrix.postScale(scale, scale)
        
        // 6. Move to canvas center
        matrix.postTranslate(width / 2f, height / 2f)
        
        srcShader.setLocalMatrix(matrix)

        shader.setInputBuffer("image", srcShader)
        shader.setFloatUniform("brightness", adj?.brightness ?: 0f)
        shader.setFloatUniform("contrast", adj?.contrast ?: 1f)
        shader.setFloatUniform("saturation", adj?.saturation ?: 1f)
        shader.setFloatUniform("exposure", adj?.exposure ?: 0f)
        shader.setFloatUniform("temperature", adj?.temperature ?: 0f)
        shader.setFloatUniform("tint", adj?.tint ?: 0f)
        
        val filterTypeInt = when(filterLayer?.filterId) {
            "vintage" -> 1
            "mono" -> 2
            "cinema" -> 3
            else -> 0
        }
        shader.setIntUniform("filterType", filterTypeInt)
        shader.setFloatUniform("filterIntensity", filterLayer?.intensity ?: 1f)
        
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    override fun renderPreview(state: EditorState): Bitmap? = sourceBitmap

    override fun renderFull(state: EditorState): Bitmap? {
        // For full resolution, we create a new bitmap of the requested frame size or source size
        // If there's a crop, the output size should match the cropped dimensions at full scale
        val bmpW = sourceBitmap.width.toFloat()
        val bmpH = sourceBitmap.height.toFloat()
        
        val cropW = (state.cropRect.right - state.cropRect.left) * bmpW
        val cropH = (state.cropRect.bottom - state.cropRect.top) * bmpH
        
        // Handle rotation for output dimensions
        val isRotatedOdd = (state.transformations.rotationDegrees / 90f).toInt() % 2 != 0
        val outW = if (isRotatedOdd) cropH else cropW
        val outH = if (isRotatedOdd) cropW else cropH

        val output = Bitmap.createBitmap(outW.toInt(), outH.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Reuse the draw logic but with output dimensions
        draw(canvas, state, outW, outH)
        
        return output
    }

    override fun renderThumbnail(state: EditorState, size: Int): Bitmap? {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val adjLayer = state.layers.filterIsInstance<Layer.AdjustmentLayer>().firstOrNull()
        val adj = adjLayer?.adjustments
        val filterLayer = state.layers.filterIsInstance<Layer.FilterLayer>().firstOrNull()

        if (adj != null || filterLayer != null) {
            val cm = ColorMatrix()
            if (adj != null) {
                // Exposure
                val e = Math.pow(2.0, adj.exposure.toDouble()).toFloat()
                cm.postConcat(ColorMatrix(floatArrayOf(e, 0f, 0f, 0f, 0f, 0f, e, 0f, 0f, 0f, 0f, 0f, e, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
                
                // Brightness & Contrast
                val b = adj.brightness * 255
                val c = adj.contrast
                val t = (1.0f - c) / 2.0f * 255.0f
                cm.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, b + t, 0f, c, 0f, 0f, b + t, 0f, 0f, c, 0f, b + t, 0f, 0f, 0f, 1f, 0f)))
                
                val satMatrix = ColorMatrix()
                satMatrix.setSaturation(adj.saturation)
                cm.postConcat(satMatrix)
            }

            if (filterLayer != null) {
                val filterCm = ColorMatrix()
                when (filterLayer.filterId) {
                    "vintage" -> filterCm.set(floatArrayOf(0.9f, 0.5f, 0.1f, 0f, 0f, 0.3f, 0.8f, 0.1f, 0f, 0f, 0.2f, 0.3f, 0.5f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
                    "mono" -> filterCm.setSaturation(0f)
                    "cinema" -> filterCm.set(floatArrayOf(0.8f, 0f, 0f, 0f, 0f, 0f, 1.1f, 0f, 0f, 0f, 0f, 0f, 1.2f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
                }
                cm.postConcat(filterCm)
            }
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }

        val srcRect = android.graphics.Rect(
            (state.cropRect.left * sourceBitmap.width).toInt(),
            (state.cropRect.top * sourceBitmap.height).toInt(),
            (state.cropRect.right * sourceBitmap.width).toInt(),
            (state.cropRect.bottom * sourceBitmap.height).toInt()
        )
        val destRect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawBitmap(sourceBitmap, srcRect, destRect, paint)
        return output
    }

    override fun release() {}
}
