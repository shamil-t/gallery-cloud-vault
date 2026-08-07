package com.shamil.image_editor_sdk.core.domain

import kotlinx.serialization.Serializable

/**
 * The complete immutable state of an editing session.
 */
@Serializable
data class EditorState(
    val frame: ImageFrame = ImageFrame(0, 0),
    val layers: List<Layer> = emptyList(),
    val selectedLayerId: String? = null,
    val cropRect: CropRect = CropRect(),
    val transformations: Transformations = Transformations(),
    val aspectRatio: AspectRatio = AspectRatio.FREE
)
