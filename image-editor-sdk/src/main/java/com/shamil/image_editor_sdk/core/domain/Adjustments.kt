package com.shamil.image_editor_sdk.core.domain

import kotlinx.serialization.Serializable

/**
 * Data model for basic image adjustments.
 * Values are typically normalized between -1.0 and 1.0 (or 0.0 to 2.0).
 */
@Serializable
data class Adjustments(
    val brightness: Float = 0f,    // -1.0 to 1.0
    val contrast: Float = 1f,      // 0.0 to 2.0
    val saturation: Float = 1f,    // 0.0 to 2.0
    val exposure: Float = 0f,      // -1.0 to 1.0
    val temperature: Float = 0f,   // -1.0 to 1.0
    val tint: Float = 0f           // -1.0 to 1.0
)
