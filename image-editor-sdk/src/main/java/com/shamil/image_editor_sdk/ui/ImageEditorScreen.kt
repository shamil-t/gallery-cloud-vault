package com.shamil.image_editor_sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shamil.image_editor_sdk.core.session.EditorSession
import com.shamil.image_editor_sdk.ui.adjustments.AdjustmentsPanel
import com.shamil.image_editor_sdk.ui.canvas.EditorCanvas
import com.shamil.image_editor_sdk.ui.components.EditorTopBar
import com.shamil.image_editor_sdk.ui.filters.FilterList
import com.shamil.image_editor_sdk.ui.transform.CropControls

@Composable
fun ImageEditorScreen(
    session: EditorSession,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    var activeTool by remember { mutableStateOf(EditorTool.ADJUST) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Image Canvas (Edge-to-Edge)
        EditorCanvas(
            session = session,
            activeTool = activeTool,
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar (Overlay)
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            EditorTopBar(
                session = session,
                onClose = onClose,
                onSave = onSave
            )
        }

        // Bottom Controls (Overlay)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(modifier = Modifier.padding(vertical = 8.dp)) {
                        when (activeTool) {
                            EditorTool.FILTERS -> FilterList(session = session)
                            EditorTool.ADJUST -> AdjustmentsPanel(session = session)
                            EditorTool.CROP -> CropControls(session = session)
                            EditorTool.DRAW -> Text("Drawing coming soon", modifier = Modifier.padding(16.dp))
                        }
                    }

                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ) {
                        EditorTool.entries.forEach { tool ->
                            NavigationBarItem(
                                selected = activeTool == tool,
                                onClick = { activeTool = tool },
                                icon = { Icon(tool.icon, contentDescription = tool.title) },
                                label = { Text(tool.title) }
                            )
                        }
                    }
                }
            }
        }
    }
}
