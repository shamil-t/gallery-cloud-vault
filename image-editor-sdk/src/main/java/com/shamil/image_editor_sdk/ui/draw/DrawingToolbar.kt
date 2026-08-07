package com.shamil.image_editor_sdk.ui.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DrawingToolbar(
    onColorSelected: (Int) -> Unit,
    onBrushSizeSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Black, Color.White)

    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            ColorPicker(color = color, onClick = { onColorSelected(0) /* Placeholder for real Int color */ })
        }
    }
}

@Composable
private fun ColorPicker(
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .clickable { onClick() }
    )
}
