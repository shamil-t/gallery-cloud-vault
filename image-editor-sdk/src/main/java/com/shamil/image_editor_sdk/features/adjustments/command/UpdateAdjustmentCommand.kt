package com.shamil.image_editor_sdk.features.adjustments.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.Layer
import com.shamil.image_editor_sdk.features.adjustments.domain.AdjustmentType

/**
 * Command to update a specific image adjustment value.
 */
data class UpdateAdjustmentCommand(
    val type: AdjustmentType,
    val value: Float
) : EditCommand {

    override fun apply(oldState: EditorState): EditorState {
        val updatedLayers = oldState.layers.map { layer ->
            if (layer is Layer.AdjustmentLayer) {
                layer.copy(
                    adjustments = when (type) {
                        AdjustmentType.BRIGHTNESS -> layer.adjustments.copy(brightness = value)
                        AdjustmentType.CONTRAST -> layer.adjustments.copy(contrast = value)
                        AdjustmentType.SATURATION -> layer.adjustments.copy(saturation = value)
                        AdjustmentType.EXPOSURE -> layer.adjustments.copy(exposure = value)
                        AdjustmentType.TEMPERATURE -> layer.adjustments.copy(temperature = value)
                        AdjustmentType.TINT -> layer.adjustments.copy(tint = value)
                    }
                )
            } else {
                layer
            }
        }

        // If no adjustment layer exists, we could create one, but for now we assume one exists or is added by default
        return oldState.copy(layers = updatedLayers)
    }
}
