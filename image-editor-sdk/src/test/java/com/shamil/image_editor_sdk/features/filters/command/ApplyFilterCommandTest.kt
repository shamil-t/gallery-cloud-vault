package com.shamil.image_editor_sdk.features.filters.command

import com.shamil.image_editor_sdk.core.domain.EditorState
import com.shamil.image_editor_sdk.core.domain.ImageFrame
import com.shamil.image_editor_sdk.core.domain.Layer
import com.shamil.image_editor_sdk.features.filters.domain.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyFilterCommandTest {

    @Test
    fun `applying filter creates a FilterLayer if not exists`() {
        val initialState = EditorState(ImageFrame(100, 100))
        val command = ApplyFilterCommand(FilterType.VINTAGE, 0.8f)
        
        val newState = command.apply(initialState)
        
        val filterLayer = newState.layers.filterIsInstance<Layer.FilterLayer>().firstOrNull()
        assertTrue(filterLayer != null)
        assertEquals("vintage", filterLayer?.filterId)
        assertEquals(0.8f, filterLayer?.intensity)
    }

    @Test
    fun `applying new filter updates existing FilterLayer`() {
        val initialFilter = Layer.FilterLayer(filterId = "mono", intensity = 1.0f)
        val initialState = EditorState(
            frame = ImageFrame(100, 100),
            layers = listOf(initialFilter)
        )
        
        val command = ApplyFilterCommand(FilterType.CINEMA, 0.5f)
        val newState = command.apply(initialState)
        
        val filterLayers = newState.layers.filterIsInstance<Layer.FilterLayer>()
        assertEquals(1, filterLayers.size)
        assertEquals("cinema", filterLayers.first().filterId)
        assertEquals(0.5f, filterLayers.first().intensity)
    }
}
