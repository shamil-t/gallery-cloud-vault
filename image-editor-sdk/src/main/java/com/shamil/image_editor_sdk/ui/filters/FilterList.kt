package com.shamil.image_editor_sdk.ui.filters

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.shamil.image_editor_sdk.core.session.EditorSession
import com.shamil.image_editor_sdk.features.filters.command.ApplyFilterCommand
import com.shamil.image_editor_sdk.features.filters.domain.FilterType
import com.shamil.image_editor_sdk.renderer.AgslRenderer
import com.shamil.image_editor_sdk.renderer.CanvasRenderer

@Composable
fun FilterList(
    session: EditorSession,
    modifier: Modifier = Modifier
) {
    val filters = FilterType.entries
    val state by session.state.collectAsState()
    
    val renderer = remember(session.sourceBitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AgslRenderer(session.sourceBitmap)
        } else {
            CanvasRenderer(session.sourceBitmap)
        }
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(filters) { filter ->
            // Generate a thumbnail for each filter
            val filterState = state.copy(
                layers = state.layers.filter { it !is com.shamil.image_editor_sdk.core.domain.Layer.FilterLayer } + 
                         com.shamil.image_editor_sdk.core.domain.Layer.FilterLayer(filterId = filter.id)
            )
            val thumbnail = remember(filter) { renderer.renderThumbnail(filterState, 128) }

            FilterItem(
                filter = filter,
                thumbnail = thumbnail,
                onClick = { session.execute(ApplyFilterCommand(filter)) }
            )
        }
    }
}

@Composable
private fun FilterItem(
    filter: FilterType,
    thumbnail: Bitmap?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onClick() }
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = filter.name,
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
