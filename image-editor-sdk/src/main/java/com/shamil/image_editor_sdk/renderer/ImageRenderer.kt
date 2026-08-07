package com.shamil.image_editor_sdk.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import com.shamil.image_editor_sdk.core.domain.EditorState

/**
 * Interface for rendering the [EditorState] into a visual output.
 * Implementations can use AGSL, OpenGL, or Canvas.
 */
interface ImageRenderer {
    /**
     * Renders the current state directly onto the provided [canvas].
     * This is used for hardware-accelerated previews.
     */
    fun draw(canvas: Canvas, state: EditorState, width: Float, height: Float)

    /**
     * Renders the current state for preview on screen as a Bitmap.
     * @deprecated Use [draw] for hardware-accelerated Compose previews.
     */
    fun renderPreview(state: EditorState): Bitmap?

    /**
     * Renders the current state at full resolution for export.
     */
    fun renderFull(state: EditorState): Bitmap?

    /**
     * Renders a small thumbnail of the current state.
     */
    fun renderThumbnail(state: EditorState, size: Int): Bitmap?

    /**
     * Releases any GPU or native resources.
     */
    fun release()
}
