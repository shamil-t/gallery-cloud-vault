package com.shamil.image_editor_sdk.features.adjustments.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Supported adjustment types for the UI and command routing.
 */
enum class AdjustmentType(val icon: ImageVector) {
    BRIGHTNESS(Icons.Default.BrightnessLow),
    CONTRAST(Icons.Default.Contrast),
    SATURATION(Icons.Default.WaterDrop),
    EXPOSURE(Icons.Default.Exposure),
    TEMPERATURE(Icons.Default.DeviceThermostat),
    TINT(Icons.Default.InvertColors)
}
