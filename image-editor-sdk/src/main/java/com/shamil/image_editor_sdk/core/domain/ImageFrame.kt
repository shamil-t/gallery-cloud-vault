package com.shamil.image_editor_sdk.core.domain

import kotlinx.serialization.Serializable

/**
 * Represents the physical and logical boundaries of the image being edited.
 */
@Serializable
data class ImageFrame(
    val width: Int,
    val height: Int,
    val orientation: Int = 0,
    val density: Float = 1.0f
) {
    val aspectRatio: Float
        get() = if (height != 0) width.toFloat() / height else 1f

    val isPortrait: Boolean
        get() = height > width
}
