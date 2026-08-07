package com.shamil.image_editor_sdk.features.transform.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.AspectRatio
import com.shamil.image_editor_sdk.core.domain.CropRect
import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Command to apply a specific aspect ratio to the current crop.
 */
data class ApplyAspectRatioCommand(
    val aspectRatio: AspectRatio
) : EditCommand {
    override fun apply(oldState: EditorState): EditorState {
        val ratio = aspectRatio.ratio ?: return oldState
        
        val currentW = oldState.cropRect.right - oldState.cropRect.left
        val currentH = oldState.cropRect.bottom - oldState.cropRect.top
        val centerX = (oldState.cropRect.left + oldState.cropRect.right) / 2f
        val centerY = (oldState.cropRect.top + oldState.cropRect.bottom) / 2f
        
        // Target ratio is W/H = ratio
        // We try to fit the largest possible rectangle with this ratio into the current frame (0..1)
        
        var newW: Float
        var newH: Float
        
        // Image aspect ratio (normalized is 1:1 if we consider the whole frame as 1x1)
        // But the frame itself might not be square.
        // Let's assume the normalized coordinates are relative to the bitmap dimensions.
        // Bitmap dimensions are in oldState.frame.width/height.
        val frameRatio = oldState.frame.width.toFloat() / oldState.frame.height.toFloat()
        
        // Target normalized ratio: (newW * width) / (newH * height) = ratio
        // newW / newH = ratio / frameRatio
        val normalizedTargetRatio = ratio / frameRatio
        
        if (1.0f / 1.0f > normalizedTargetRatio) {
            // Frame is wider than target. Maximize height.
            newH = 1.0f
            newW = normalizedTargetRatio
        } else {
            // Frame is taller than target. Maximize width.
            newW = 1.0f
            newH = 1.0f / normalizedTargetRatio
        }
        
        val newLeft = (0.5f - newW / 2f).coerceAtLeast(0f)
        val newTop = (0.5f - newH / 2f).coerceAtLeast(0f)
        val newRight = (0.5f + newW / 2f).coerceAtMost(1f)
        val newBottom = (0.5f + newH / 2f).coerceAtMost(1f)
        
        return oldState.copy(
            cropRect = CropRect(newLeft, newTop, newRight, newBottom),
            aspectRatio = aspectRatio
        )
    }
}
