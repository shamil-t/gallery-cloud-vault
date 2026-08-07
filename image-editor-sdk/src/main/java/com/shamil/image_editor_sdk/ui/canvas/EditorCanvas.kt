package com.shamil.image_editor_sdk.ui.canvas

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.shamil.image_editor_sdk.core.domain.CropRect
import com.shamil.image_editor_sdk.core.session.EditorSession
import com.shamil.image_editor_sdk.features.transform.command.CropCommand
import com.shamil.image_editor_sdk.renderer.AgslRenderer
import com.shamil.image_editor_sdk.renderer.CanvasRenderer
import com.shamil.image_editor_sdk.renderer.ImageRenderer
import com.shamil.image_editor_sdk.ui.EditorTool
import com.shamil.image_editor_sdk.ui.transform.CropOverlay
import kotlin.math.min

/**
 * The main drawing area of the editor.
 * Handles gestures and delegates rendering to the [ImageRenderer].
 */
@Composable
fun EditorCanvas(
    session: EditorSession,
    activeTool: EditorTool,
    modifier: Modifier = Modifier,
    transformationState: TransformationState = remember { TransformationState() }
) {
    val state by session.state.collectAsState()
    val density = LocalDensity.current
    
    val renderer = remember(session.sourceBitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AgslRenderer(session.sourceBitmap)
        } else {
            CanvasRenderer(session.sourceBitmap)
        }
    }

    val isCropMode = activeTool == EditorTool.CROP

    // Reset zoom/pan when entering crop mode to avoid interaction issues
    androidx.compose.runtime.LaunchedEffect(isCropMode) {
        if (isCropMode) {
            transformationState.reset()
        }
    }
    
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        if (containerWidth > 0 && containerHeight > 0) {
            val isCropMode = activeTool == EditorTool.CROP
            val rotation = state.transformations.rotationDegrees
            val isRotatedOdd = (rotation / 90f).toInt() % 2 != 0
            
            val baseWidth = if (isCropMode) {
                session.sourceBitmap.width.toFloat()
            } else {
                (state.cropRect.right - state.cropRect.left) * session.sourceBitmap.width
            }
            
            val baseHeight = if (isCropMode) {
                session.sourceBitmap.height.toFloat()
            } else {
                (state.cropRect.bottom - state.cropRect.top) * session.sourceBitmap.height
            }

            // Swap dimensions if rotated 90 or 270 degrees
            val contentWidth = if (isRotatedOdd) baseHeight else baseWidth
            val contentHeight = if (isRotatedOdd) baseWidth else baseHeight

            val scale = min(containerWidth / contentWidth, containerHeight / contentHeight)
            val drawWidth = contentWidth * scale
            val drawHeight = contentHeight * scale
            
            val offsetX = (containerWidth - drawWidth) / 2
            val offsetY = (containerHeight - drawHeight) / 2

            // In crop mode, we want the overlay to be size of the bitmap scaled to fit
            // But if rotated, the bitmap's visual width is baseHeight * scale
            val overlayWidth = if (isRotatedOdd) drawHeight else drawWidth
            val overlayHeight = if (isRotatedOdd) drawWidth else drawHeight

            val drawingState = if (isCropMode) state.copy(cropRect = CropRect(0f, 0f, 1f, 1f)) else state

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Image Layer (Transformable)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isCropMode) {
                            if (!isCropMode) {
                                detectTransformGestures { centroid, pan, zoom, rotation ->
                                    transformationState.onGesture(centroid, pan, zoom, rotation)
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = transformationState.scale
                            scaleY = transformationState.scale
                            translationX = transformationState.offset.x
                            translationY = transformationState.offset.y
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(with(density) { drawWidth.toDp() }, with(density) { drawHeight.toDp() })
                            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                    ) {
                        drawIntoCanvas { canvas ->
                            renderer.draw(canvas.nativeCanvas, drawingState, size.width, size.height)
                        }
                    }
                }

                // Overlay Layer (Fixed to screen in Crop mode)
                if (isCropMode) {
                    val overlayBaseWidth = if (isRotatedOdd) drawHeight else drawWidth
                    val overlayBaseHeight = if (isRotatedOdd) drawWidth else drawHeight
                    
                    Box(
                        modifier = Modifier
                            .size(with(density) { overlayBaseWidth.toDp() }, with(density) { overlayBaseHeight.toDp() })
                            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                            .graphicsLayer { rotationZ = rotation }
                    ) {
                        CropOverlay(
                            cropRect = state.cropRect,
                            aspectRatio = state.aspectRatio,
                            onCropChange = { session.preview(CropCommand(it)) },
                            onCropCommit = { session.commit(CropCommand(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
