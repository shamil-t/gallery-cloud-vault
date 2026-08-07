package com.shamil.image_editor_sdk.ui.adjustments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shamil.image_editor_sdk.core.domain.Layer
import com.shamil.image_editor_sdk.core.session.EditorSession
import com.shamil.image_editor_sdk.features.adjustments.command.UpdateAdjustmentCommand
import com.shamil.image_editor_sdk.features.adjustments.domain.AdjustmentType
import com.shamil.image_editor_sdk.ui.components.AdjustmentSlider

@Composable
fun AdjustmentsPanel(
    session: EditorSession,
    modifier: Modifier = Modifier
) {
    val state by session.state.collectAsState()
    val adjLayer = state.layers.filterIsInstance<Layer.AdjustmentLayer>().firstOrNull() ?: return
    val adj = adjLayer.adjustments

    var selectedType by remember { mutableStateOf(AdjustmentType.BRIGHTNESS) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Compact Slider
        val currentValue = when (selectedType) {
            AdjustmentType.BRIGHTNESS -> adj.brightness
            AdjustmentType.CONTRAST -> adj.contrast
            AdjustmentType.SATURATION -> adj.saturation
            AdjustmentType.EXPOSURE -> adj.exposure
            AdjustmentType.TEMPERATURE -> adj.temperature
            AdjustmentType.TINT -> adj.tint
        }

        val range = when (selectedType) {
            AdjustmentType.CONTRAST, AdjustmentType.SATURATION -> 0f..2f
            else -> -1f..1f
        }

        AdjustmentSlider(
            value = currentValue,
            range = range,
            onValueChange = { session.execute(UpdateAdjustmentCommand(selectedType, it)) },
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )

        // Horizontal Tool List
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(AdjustmentType.entries) { type ->
                AdjustmentItem(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { selectedType = type }
                )
            }
        }
    }
}

@Composable
private fun AdjustmentItem(
    type: AdjustmentType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = type.icon, // I need to add icons to AdjustmentType
            contentDescription = type.name,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
