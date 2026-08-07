package com.shamil.image_editor_sdk.ui.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Manages the viewport transformations (Zoom, Pan).
 * This is separate from image edits (Crop/Rotate).
 */
class TransformationState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float
    ) {
        scale = (scale * zoom).coerceIn(0.1f, 10f)
        offset += pan
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
}
