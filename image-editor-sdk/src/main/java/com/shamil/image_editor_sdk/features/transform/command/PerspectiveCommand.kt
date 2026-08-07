package com.shamil.image_editor_sdk.features.transform.command

import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Command to apply perspective transformation.
 * Uses 8 values for the homography matrix.
 */
data class PerspectiveCommand(
    val matrixValues: FloatArray
) : EditCommand {
    override fun apply(oldState: EditorState): EditorState {
        // Implementation would update a perspective field in transformations
        return oldState
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerspectiveCommand) return false
        return matrixValues.contentEquals(other.matrixValues)
    }

    override fun hashCode(): Int {
        return matrixValues.contentHashCode()
    }
}
