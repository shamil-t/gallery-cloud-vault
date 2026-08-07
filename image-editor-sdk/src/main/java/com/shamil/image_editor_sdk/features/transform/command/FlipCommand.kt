package com.shamil.image_editor_sdk.features.transform.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Command to flip the image horizontally or vertically.
 */
data class FlipCommand(
    val horizontal: Boolean = false,
    val vertical: Boolean = false
) : EditCommand {
    override fun apply(oldState: EditorState): EditorState {
        val current = oldState.transformations
        return oldState.copy(
            transformations = current.copy(
                isFlippedHorizontal = if (horizontal) !current.isFlippedHorizontal else current.isFlippedHorizontal,
                isFlippedVertical = if (vertical) !current.isFlippedVertical else current.isFlippedVertical
            )
        )
    }
}
