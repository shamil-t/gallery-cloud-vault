package com.shamil.image_editor_sdk.ui.transform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import com.shamil.image_editor_sdk.core.domain.AspectRatio
import com.shamil.image_editor_sdk.core.session.EditorSession
import com.shamil.image_editor_sdk.features.transform.command.ApplyAspectRatioCommand
import com.shamil.image_editor_sdk.features.transform.command.FlipCommand
import com.shamil.image_editor_sdk.features.transform.command.RotateCommand

@Composable
fun CropControls(
    session: EditorSession,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Aspect Ratios
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AspectRatio.entries) { ratio ->
                AssistChip(
                    onClick = { session.execute(ApplyAspectRatioCommand(ratio)) },
                    label = { Text(ratio.title) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = androidx.compose.ui.graphics.Color.White,
                        containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)
                    ),
                    border = null
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { session.execute(RotateCommand(90f)) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = androidx.compose.ui.graphics.Color.White)
                    Text("90°", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
                }
            }

            IconButton(onClick = { session.execute(FlipCommand(horizontal = true)) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = androidx.compose.ui.graphics.Color.White)
                    Text("Flip H", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
                }
            }

            IconButton(onClick = { session.execute(FlipCommand(vertical = true)) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SwapCalls, contentDescription = "Flip Vertical", tint = androidx.compose.ui.graphics.Color.White)
                    Text("Flip V", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
