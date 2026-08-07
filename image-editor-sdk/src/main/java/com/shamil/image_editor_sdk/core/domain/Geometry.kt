package com.shamil.image_editor_sdk.core.domain

import kotlinx.serialization.Serializable

/**
 * Defines a crop area in normalized coordinates (0.0 to 1.0).
 */
@Serializable
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

/**
 * Basic geometric transformations.
 */
@Serializable
data class Transformations(
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val straightenAngle: Float = 0f
)
