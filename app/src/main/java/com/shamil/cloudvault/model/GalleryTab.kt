package com.shamil.cloudvault.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

enum class GalleryTab(val title: String, val icon: ImageVector) {
    Recent("Recent", Icons.Default.History),
    Albums("Albums", Icons.Default.Collections),
    Favorites("Favorites", Icons.Default.Favorite),
    Bin("Bin", Icons.Default.Delete)
}
