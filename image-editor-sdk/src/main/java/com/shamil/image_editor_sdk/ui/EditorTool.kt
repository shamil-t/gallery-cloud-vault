package com.shamil.image_editor_sdk.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Supported editing tools in the UI.
 */
enum class EditorTool(val title: String, val icon: ImageVector) {
    FILTERS("Filters", Icons.Default.AutoFixHigh),
    ADJUST("Adjust", Icons.Default.Tune),
    CROP("Crop", Icons.Default.Crop),
    DRAW("Draw", Icons.Default.Brush)
}
