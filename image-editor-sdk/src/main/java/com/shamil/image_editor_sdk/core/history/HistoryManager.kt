package com.shamil.image_editor_sdk.core.history

import com.shamil.image_editor_sdk.core.domain.EditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

/**
 * Manages the undo and redo stacks for the editor session.
 * Optimized for performance and memory usage.
 */
class HistoryManager(initialState: EditorState) {

    private val undoStack = Stack<EditorState>()
    private val redoStack = Stack<EditorState>()

    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<EditorState> = _currentState.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /**
     * Push a new state to the history.
     */
    fun pushState(newState: EditorState) {
        if (newState == _currentState.value) return

        undoStack.push(_currentState.value)
        redoStack.clear()
        
        _currentState.value = newState
        updateStatus()
    }

    /**
     * Directly update the current state without adding to history.
     * Used for live previews (sliders, handle dragging).
     */
    fun updateCurrentState(newState: EditorState) {
        if (newState == _currentState.value) return
        _currentState.value = newState
    }

    /**
     * Reverts to the previous state.
     */
    fun undo(): EditorState? {
        if (undoStack.isEmpty()) return null

        redoStack.push(_currentState.value)
        val prevState = undoStack.pop()
        
        _currentState.value = prevState
        updateStatus()
        return prevState
    }

    /**
     * Re-applies a previously undone state.
     */
    fun redo(): EditorState? {
        if (redoStack.isEmpty()) return null

        undoStack.push(_currentState.value)
        val nextState = redoStack.pop()
        
        _currentState.value = nextState
        updateStatus()
        return nextState
    }

    private fun updateStatus() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    /**
     * Clears all history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateStatus()
    }
}
