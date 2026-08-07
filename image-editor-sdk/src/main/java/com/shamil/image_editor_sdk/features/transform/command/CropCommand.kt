package com.shamil.image_editor_sdk.features.transform.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.CropRect
import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Command to update the crop area.
 */
data class CropCommand(
    val newCropRect: CropRect
) : EditCommand {
    override fun apply(oldState: EditorState): EditorState {
        return oldState.copy(cropRect = newCropRect)
    }
}
