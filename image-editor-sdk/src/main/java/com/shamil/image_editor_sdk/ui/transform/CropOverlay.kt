package com.shamil.image_editor_sdk.ui.transform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shamil.image_editor_sdk.core.domain.AspectRatio
import com.shamil.image_editor_sdk.core.domain.CropRect
import kotlin.math.roundToInt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path

/**
 * Interactive crop overlay with Samsung-style handles and dynamic grid.
 */
@Composable
fun CropOverlay(
    cropRect: CropRect,
    aspectRatio: AspectRatio,
    onCropChange: (CropRect) -> Unit,
    onCropCommit: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        if (width <= 0f || height <= 0f) return@BoxWithConstraints

        val left = cropRect.left * width
        val top = cropRect.top * height
        val right = cropRect.right * width
        val bottom = cropRect.bottom * height

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Dark background for uncropped area
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, 0f), size = Size(width, top))
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, bottom), size = Size(width, height - bottom))
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, top), size = Size(left, bottom - top))
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(right, top), size = Size(width - right, bottom - top))

            val rectWidth = right - left
            val rectHeight = bottom - top

            // 2. Rule of thirds grid (Only show during drag)
            if (isDragging) {
                val gridColor = Color.White.copy(alpha = 0.4f)
                val gridStroke = 0.5.dp.toPx()
                
                drawLine(gridColor, Offset(left + rectWidth / 3, top), Offset(left + rectWidth / 3, bottom), gridStroke)
                drawLine(gridColor, Offset(left + 2 * rectWidth / 3, top), Offset(left + 2 * rectWidth / 3, bottom), gridStroke)
                drawLine(gridColor, Offset(left, top + rectHeight / 3), Offset(right, top + rectHeight / 3), gridStroke)
                drawLine(gridColor, Offset(left, top + 2 * rectHeight / 3), Offset(right, top + 2 * rectHeight / 3), gridStroke)
            }

            // 3. Main Border
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Samsung-style Corner Handles (Bold "L" shapes)
            val cornerLen = 20.dp.toPx()
            val cornerThick = 3.dp.toPx()
            
            // Top-Left
            drawPath(Path().apply {
                moveTo(left, top + cornerLen)
                lineTo(left, top)
                lineTo(left + cornerLen, top)
            }, Color.White, style = Stroke(width = cornerThick))
            
            // Top-Right
            drawPath(Path().apply {
                moveTo(right - cornerLen, top)
                lineTo(right, top)
                lineTo(right, top + cornerLen)
            }, Color.White, style = Stroke(width = cornerThick))
            
            // Bottom-Left
            drawPath(Path().apply {
                moveTo(left, bottom - cornerLen)
                lineTo(left, bottom)
                lineTo(left + cornerLen, bottom)
            }, Color.White, style = Stroke(width = cornerThick))
            
            // Bottom-Right
            drawPath(Path().apply {
                moveTo(right - cornerLen, bottom)
                lineTo(right, bottom)
                lineTo(right, bottom - cornerLen)
            }, Color.White, style = Stroke(width = cornerThick))

            // 5. Edge Bars
            val barLen = 16.dp.toPx()
            drawLine(Color.White, Offset(left + rectWidth / 2 - barLen, top), Offset(left + rectWidth / 2 + barLen, top), cornerThick)
            drawLine(Color.White, Offset(left + rectWidth / 2 - barLen, bottom), Offset(left + rectWidth / 2 + barLen, bottom), cornerThick)
            drawLine(Color.White, Offset(left, top + rectHeight / 2 - barLen), Offset(left, top + rectHeight / 2 + barLen), cornerThick)
            drawLine(Color.White, Offset(right, top + rectHeight / 2 - barLen), Offset(right, top + rectHeight / 2 + barLen), cornerThick)
        }

        val currentOnCropChange by androidx.compose.runtime.rememberUpdatedState(onCropChange)
        val currentOnCropCommit by androidx.compose.runtime.rememberUpdatedState(onCropCommit)

        // Handles with responsive drag logic
        // Corners
        Handle(
            Offset(left, top),
            onStart = { isDragging = true },
            onEnd = { 
                isDragging = false
                currentOnCropCommit(cropRect)
            }
        ) { drag ->
            // Use current coordinates + drag delta for more precision
            val currentL = cropRect.left * width
            val currentT = cropRect.top * height
            var nL = ((currentL + drag.x) / width).coerceIn(0f, cropRect.right - 0.1f)
            var nT = ((currentT + drag.y) / height).coerceIn(0f, cropRect.bottom - 0.1f)
            
            aspectRatio.ratio?.let { r ->
                val targetW = (cropRect.right - nL) * width
                val constrainedH = targetW / r
                nT = cropRect.bottom - constrainedH / height
                if (nT < 0f) {
                    nT = 0f
                    val maxH = cropRect.bottom * height
                    val maxW = maxH * r
                    nL = cropRect.right - maxW / width
                }
            }
            
            currentOnCropChange(cropRect.copy(left = nL.coerceAtLeast(0f), top = nT.coerceAtLeast(0f)))
        }
        Handle(
            Offset(right, top),
            onStart = { isDragging = true },
            onEnd = { 
                isDragging = false
                currentOnCropCommit(cropRect)
            }
        ) { drag ->
            val currentR = cropRect.right * width
            val currentT = cropRect.top * height
            var nR = ((currentR + drag.x) / width).coerceIn(cropRect.left + 0.1f, 1f)
            var nT = ((currentT + drag.y) / height).coerceIn(0f, cropRect.bottom - 0.1f)
            
            aspectRatio.ratio?.let { r ->
                val targetW = (nR - cropRect.left) * width
                val constrainedH = targetW / r
                nT = cropRect.bottom - constrainedH / height
                if (nT < 0f) {
                    nT = 0f
                    val maxH = cropRect.bottom * height
                    val maxW = maxH * r
                    nR = cropRect.left + maxW / width
                }
            }
            
            currentOnCropChange(cropRect.copy(right = nR.coerceAtMost(1f), top = nT.coerceAtLeast(0f)))
        }
        Handle(
            Offset(left, bottom),
            onStart = { isDragging = true },
            onEnd = { 
                isDragging = false
                currentOnCropCommit(cropRect)
            }
        ) { drag ->
            val currentL = cropRect.left * width
            val currentB = cropRect.bottom * height
            var nL = ((currentL + drag.x) / width).coerceIn(0f, cropRect.right - 0.1f)
            var nB = ((currentB + drag.y) / height).coerceIn(cropRect.top + 0.1f, 1f)
            
            aspectRatio.ratio?.let { r ->
                val targetW = (cropRect.right - nL) * width
                val constrainedH = targetW / r
                nB = cropRect.top + constrainedH / height
                if (nB > 1f) {
                    nB = 1f
                    val maxH = (1f - cropRect.top) * height
                    val maxW = maxH * r
                    nL = cropRect.right - maxW / width
                }
            }
            
            currentOnCropChange(cropRect.copy(left = nL.coerceAtLeast(0f), bottom = nB.coerceAtMost(1f)))
        }
        Handle(
            Offset(right, bottom),
            onStart = { isDragging = true },
            onEnd = { 
                isDragging = false
                currentOnCropCommit(cropRect)
            }
        ) { drag ->
            val currentR = cropRect.right * width
            val currentB = cropRect.bottom * height
            var nR = ((currentR + drag.x) / width).coerceIn(cropRect.left + 0.1f, 1f)
            var nB = ((currentB + drag.y) / height).coerceIn(cropRect.top + 0.1f, 1f)
            
            aspectRatio.ratio?.let { r ->
                val targetW = (nR - cropRect.left) * width
                val constrainedH = targetW / r
                nB = cropRect.top + constrainedH / height
                if (nB > 1f) {
                    nB = 1f
                    val maxH = (1f - cropRect.top) * height
                    val maxW = maxH * r
                    nR = cropRect.left + maxW / width
                }
            }
            
            currentOnCropChange(cropRect.copy(right = nR.coerceAtMost(1f), bottom = nB.coerceAtMost(1f)))
        }

        // Edges (Hide if fixed aspect ratio)
        if (aspectRatio == AspectRatio.FREE) {
            Handle(
                Offset((left + right) / 2, top),
                onStart = { isDragging = true },
                onEnd = { 
                    isDragging = false
                    currentOnCropCommit(cropRect)
                }
            ) { drag ->
                val nT = ((top + drag.y) / height).coerceIn(0f, cropRect.bottom - 0.1f)
                currentOnCropChange(cropRect.copy(top = nT))
            }
            Handle(
                Offset((left + right) / 2, bottom),
                onStart = { isDragging = true },
                onEnd = { 
                    isDragging = false
                    currentOnCropCommit(cropRect)
                }
            ) { drag ->
                val nB = ((bottom + drag.y) / height).coerceIn(cropRect.top + 0.1f, 1f)
                currentOnCropChange(cropRect.copy(bottom = nB))
            }
            Handle(
                Offset(left, (top + bottom) / 2),
                onStart = { isDragging = true },
                onEnd = { 
                    isDragging = false
                    currentOnCropCommit(cropRect)
                }
            ) { drag ->
                val nL = ((left + drag.x) / width).coerceIn(0f, cropRect.right - 0.1f)
                currentOnCropChange(cropRect.copy(left = nL))
            }
            Handle(
                Offset(right, (top + bottom) / 2),
                onStart = { isDragging = true },
                onEnd = { 
                    isDragging = false
                    currentOnCropCommit(cropRect)
                }
            ) { drag ->
                val nR = ((right + drag.x) / width).coerceIn(cropRect.left + 0.1f, 1f)
                currentOnCropChange(cropRect.copy(right = nR))
            }
        }
    }
}

@Composable
private fun Handle(
    position: Offset,
    onStart: () -> Unit = {},
    onEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit
) {
    val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val currentOnStart by androidx.compose.runtime.rememberUpdatedState(onStart)
    val currentOnEnd by androidx.compose.runtime.rememberUpdatedState(onEnd)

    Box(
        modifier = Modifier
            .size(44.dp) // Large touch target
            .offset {
                IntOffset(
                    (position.x - 22.dp.toPx()).roundToInt(),
                    (position.y - 22.dp.toPx()).roundToInt()
                )
            }
            .pointerInput(Unit) { // Use Unit key to prevent restart during drag
                detectDragGestures(
                    onDragStart = { currentOnStart() },
                    onDragEnd = { currentOnEnd() },
                    onDragCancel = { currentOnEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount)
                    }
                )
            }
    )
}
