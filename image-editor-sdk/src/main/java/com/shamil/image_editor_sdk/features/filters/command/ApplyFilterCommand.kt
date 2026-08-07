package com.shamil.image_editor_sdk.features.filters.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.Layer
import com.shamil.image_editor_sdk.features.filters.domain.FilterType

/**
 * Command to apply a filter to the image.
 */
data class ApplyFilterCommand(
    val filterType: FilterType,
    val intensity: Float = 1.0f
) : EditCommand {

    override fun apply(oldState: EditorState): EditorState {
        val existingFilterLayer = oldState.layers.filterIsInstance<Layer.FilterLayer>().firstOrNull()
        
        val updatedLayers = if (existingFilterLayer != null) {
            oldState.layers.map { layer ->
                if (layer is Layer.FilterLayer) {
                    layer.copy(filterId = filterType.id, intensity = intensity)
                } else {
                    layer
                }
            }
        } else {
            oldState.layers + Layer.FilterLayer(filterId = filterType.id, intensity = intensity)
        }

        return oldState.copy(layers = updatedLayers)
    }
}
