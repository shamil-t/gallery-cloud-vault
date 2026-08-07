package com.shamil.image_editor_sdk.core.domain

import com.shamil.image_editor_sdk.features.draw.domain.DrawStroke
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Base definition for all layers in the editor.
 * Layers are non-destructive and hierarchical.
 */
@Serializable
sealed class Layer {
    abstract val id: String
    abstract val name: String
    abstract val isVisible: Boolean
    abstract val opacity: Float
    abstract val blendMode: BlendMode
    abstract val zIndex: Int

    @Serializable
    data class ImageLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Image Layer",
        override val isVisible: Boolean = true,
        override val opacity: Float = 1.0f,
        override val blendMode: BlendMode = BlendMode.NORMAL,
        override val zIndex: Int = 0,
        val imageUri: String? = null,
        val rotation: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val translateX: Float = 0f,
        val translateY: Float = 0f
    ) : Layer()

    @Serializable
    data class AdjustmentLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Adjustments",
        override val isVisible: Boolean = true,
        override val opacity: Float = 1.0f,
        override val blendMode: BlendMode = BlendMode.NORMAL,
        override val zIndex: Int = 1,
        val adjustments: Adjustments = Adjustments()
    ) : Layer()

    @Serializable
    data class FilterLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Filter",
        override val isVisible: Boolean = true,
        override val opacity: Float = 1.0f,
        override val blendMode: BlendMode = BlendMode.NORMAL,
        override val zIndex: Int = 2,
        val filterId: String? = null,
        val intensity: Float = 1.0f
    ) : Layer()

    @Serializable
    data class DrawingLayer(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Drawing",
        override val isVisible: Boolean = true,
        override val opacity: Float = 1.0f,
        override val blendMode: BlendMode = BlendMode.NORMAL,
        override val zIndex: Int = 3,
        val strokes: List<DrawStroke> = emptyList()
    ) : Layer()
}
