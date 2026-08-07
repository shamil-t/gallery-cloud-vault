package com.shamil.image_editor_sdk.core.command

import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Interface for all non-destructive editing operations.
 */
interface EditCommand {
    /**
     * Applies the command to the given [oldState] and returns the [newState].
     */
    fun apply(oldState: EditorState): EditorState

    /**
     * Reverts the changes made by this command.
     * Note: In a pure redo/undo system with immutable states,
     * we often just store the previous state instead of implementing [undo].
     * However, for memory efficiency, we might want to store only the diff.
     */
    fun undo(currentState: EditorState, previousState: EditorState): EditorState = previousState
}
