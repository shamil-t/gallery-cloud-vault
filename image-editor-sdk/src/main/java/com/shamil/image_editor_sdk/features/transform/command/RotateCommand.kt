package com.shamil.image_editor_sdk.features.transform.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Command to rotate the image by 90-degree increments.
 */
data class RotateCommand(
    val deltaDegrees: Float
) : EditCommand {
    override fun apply(oldState: EditorState): EditorState {
        val currentRotation = oldState.transformations.rotationDegrees
        val newRotation = (currentRotation + deltaDegrees) % 360
        return oldState.copy(
            transformations = oldState.transformations.copy(rotationDegrees = newRotation)
        )
    }
}
