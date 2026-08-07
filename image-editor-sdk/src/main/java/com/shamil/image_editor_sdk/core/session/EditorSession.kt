package com.shamil.image_editor_sdk.core.session

import android.graphics.Bitmap
import com.shamil.image_editor_sdk.core.command.EditCommand
import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.ImageFrame
import com.shamil.image_editor_sdk.core.domain.Layer
import com.shamil.image_editor_sdk.core.history.HistoryManager
import kotlinx.coroutines.flow.StateFlow

/**
 * The main coordinator for an image editing session.
 * Ties together state, history, and rendering logic.
 */
class EditorSession(
    val sourceBitmap: Bitmap,
    initialFrame: ImageFrame = ImageFrame(sourceBitmap.width, sourceBitmap.height)
) {
    private val initialState = EditorState(
        frame = initialFrame,
        layers = listOf(
            Layer.ImageLayer(name = "Background"),
            Layer.AdjustmentLayer(name = "Adjustments")
        )
    )

    private val historyManager = HistoryManager(initialState)

    /**
     * Observable stream of the current editor state.
     */
    val state: StateFlow<EditorState> = historyManager.currentState

    /**
     * Observable stream of whether an undo operation is possible.
     */
    val canUndo: StateFlow<Boolean> = historyManager.canUndo

    /**
     * Observable stream of whether a redo operation is possible.
     */
    val canRedo: StateFlow<Boolean> = historyManager.canRedo

    /**
     * Executes a command and adds it to the history.
     */
    fun execute(command: EditCommand) {
        val newState = command.apply(state.value)
        historyManager.pushState(newState)
    }

    /**
     * Updates the current state for live preview without adding to history.
     */
    fun preview(command: EditCommand) {
        val newState = command.apply(state.value)
        historyManager.updateCurrentState(newState)
    }

    /**
     * Commits a command to history. Used at the end of a gesture (e.g., onDragEnd).
     */
    fun commit(command: EditCommand) {
        execute(command)
    }

    /**
     * Reverts the last operation.
     */
    fun undo() {
        historyManager.undo()
    }

    /**
     * Re-applies the last undone operation.
     */
    fun redo() {
        historyManager.redo()
    }

    /**
     * Reset the session to its initial state.
     */
    fun reset() {
        historyManager.clear()
    }

    /**
     * Releases any resources associated with this session.
     */
    fun release() {
        // Bitmap management is handled by the caller, but we clear the history
        historyManager.clear()
    }
}
