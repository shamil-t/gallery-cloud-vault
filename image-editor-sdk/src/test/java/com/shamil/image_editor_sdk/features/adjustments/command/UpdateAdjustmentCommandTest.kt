package com.shamil.image_editor_sdk.features.adjustments.command

import com.shamil.image_editor_sdk.core.domain.Adjustments
import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.ImageFrame
import com.shamil.image_editor_sdk.core.domain.Layer
import com.shamil.image_editor_sdk.features.adjustments.domain.AdjustmentType
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateAdjustmentCommandTest {

    @Test
    fun `brightness adjustment updates correctly`() {
        val initialAdj = Adjustments(brightness = 0f)
        val adjLayer = Layer.AdjustmentLayer(adjustments = initialAdj)
        val initialState = EditorState(
            frame = ImageFrame(100, 100),
            layers = listOf(adjLayer)
        )

        val command = UpdateAdjustmentCommand(AdjustmentType.BRIGHTNESS, 0.5f)
        val newState = command.apply(initialState)

        val updatedLayer = newState.layers.first() as Layer.AdjustmentLayer
        assertEquals(0.5f, updatedLayer.adjustments.brightness)
    }

    @Test
    fun `contrast adjustment updates correctly`() {
        val initialAdj = Adjustments(contrast = 1f)
        val adjLayer = Layer.AdjustmentLayer(adjustments = initialAdj)
        val initialState = EditorState(
            frame = ImageFrame(100, 100),
            layers = listOf(adjLayer)
        )

        val command = UpdateAdjustmentCommand(AdjustmentType.CONTRAST, 1.5f)
        val newState = command.apply(initialState)

        val updatedLayer = newState.layers.first() as Layer.AdjustmentLayer
        assertEquals(1.5f, updatedLayer.adjustments.contrast)
    }
}
