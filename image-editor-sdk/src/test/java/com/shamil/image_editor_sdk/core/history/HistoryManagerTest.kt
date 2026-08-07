package com.shamil.image_editor_sdk.core.history

import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.ImageFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryManagerTest {

    private val initialState = EditorState(ImageFrame(100, 100))
    private val historyManager = HistoryManager(initialState)

    @Test
    fun `initial state is set correctly`() {
        assertEquals(initialState, historyManager.currentState.value)
        assertFalse(historyManager.canUndo.value)
        assertFalse(historyManager.canRedo.value)
    }

    @Test
    fun `pushing new state enables undo`() {
        val newState = initialState.copy(selectedLayerId = "test_layer")
        historyManager.pushState(newState)

        assertEquals(newState, historyManager.currentState.value)
        assertTrue(historyManager.canUndo.value)
        assertFalse(historyManager.canRedo.value)
    }

    @Test
    fun `undo reverts to previous state`() {
        val newState = initialState.copy(selectedLayerId = "test_layer")
        historyManager.pushState(newState)
        
        val revertedState = historyManager.undo()

        assertEquals(initialState, revertedState)
        assertEquals(initialState, historyManager.currentState.value)
        assertFalse(historyManager.canUndo.value)
        assertTrue(historyManager.canRedo.value)
    }

    @Test
    fun `redo reapplies undone state`() {
        val newState = initialState.copy(selectedLayerId = "test_layer")
        historyManager.pushState(newState)
        historyManager.undo()
        
        val reappliedState = historyManager.redo()

        assertEquals(newState, reappliedState)
        assertEquals(newState, historyManager.currentState.value)
        assertTrue(historyManager.canUndo.value)
        assertFalse(historyManager.canRedo.value)
    }

    @Test
    fun `pushing new state clears redo stack`() {
        val state1 = initialState.copy(selectedLayerId = "layer1")
        val state2 = initialState.copy(selectedLayerId = "layer2")
        
        historyManager.pushState(state1)
        historyManager.undo()
        assertTrue(historyManager.canRedo.value)

        historyManager.pushState(state2)
        assertFalse(historyManager.canRedo.value)
    }
}
