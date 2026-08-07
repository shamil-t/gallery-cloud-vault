package com.shamil.image_editor_sdk.features.draw.domain

import kotlinx.serialization.Serializable

/**
 * Represents a single point in a drawing stroke.
 */
@Serializable
data class DrawPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

/**
 * Represents a single stroke/line drawn by the user.
 */
@Serializable
data class DrawStroke(
    val points: List<DrawPoint>,
    val color: Int,
    val width: Float,
    val brushType: String = "pen"
)
