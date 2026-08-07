package com.shamil.image_editor_sdk.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.shamil.image_editor_sdk.core.session.EditorSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    session: EditorSession,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    val canUndo by session.canUndo.collectAsState()
    val canRedo by session.canRedo.collectAsState()

    TopAppBar(
        title = { Text("Edit Image", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
            IconButton(
                onClick = { session.undo() },
                enabled = canUndo
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) Color.White else Color.Gray)
            }
            IconButton(
                onClick = { session.redo() },
                enabled = canRedo
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) Color.White else Color.Gray)
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
            }
        }
    )
}
