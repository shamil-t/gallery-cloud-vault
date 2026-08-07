@file:OptIn(ExperimentalMaterial3Api::class)

package com.shamil.cloudvault.ui.gallery

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shamil.cloudvault.data.GalleryRepository
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaViewerPaging(
    items: LazyPagingItems<GalleryUiModel>,
    initialIndex: Int,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onEdit: (GalleryItem) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.itemCount }
    var showDetails by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Access current item from Paging for the Pager structure
    val pagingModel = if (pagerState.currentPage < items.itemCount && pagerState.currentPage >= 0) {
        items[pagerState.currentPage]
    } else {
        null
    }
    
    val pagingItem = (pagingModel as? GalleryUiModel.Item)?.item

    // Observe the FULL metadata reactive Flow for the current item
    val fullItem by remember(pagingItem?.id) {
        if (pagingItem != null) {
            viewModel.getMediaFlow(pagingItem.id)
        } else {
            flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = pagingItem)

    // Reset zoom state when moving to a new page
    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            userScrollEnabled = !isZoomed
        ) { page ->
            when (val model = items[page]) {
                is GalleryUiModel.Item -> {
                    val item = model.item
                    if (item.isVideo) {
                        VideoMediaItem(
                            item = item,
                            isActive = pagerState.currentPage == page
                        )
                    } else {
                        ZoomableImage(
                            item = item,
                            onZoomChange = { isZoomed = it }
                        )
                    }
                }
                is GalleryUiModel.Header -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = model.date,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        // Top Bar overlay with WindowInsets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            if (fullItem != null) {
                if (!fullItem!!.isVideo) {
                    IconButton(onClick = { onEdit(fullItem!!) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                }
                IconButton(onClick = { shareMedia(context, fullItem!!) }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
                IconButton(onClick = {
                    coroutineScope.launch {
                        viewModel.toggleFavorite(fullItem!!.id, !fullItem!!.isFavorite)
                    }
                }) {
                    Icon(
                        imageVector = if (fullItem!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (fullItem!!.isFavorite) MaterialTheme.colorScheme.error else Color.White
                    )
                }
                IconButton(onClick = { showDetails = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
                }
                IconButton(onClick = {
                    coroutineScope.launch {
                        viewModel.deleteMedia(fullItem!!.id)
                        if (items.itemCount <= 1) {
                            onBack()
                        }
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }

        if (showDetails && fullItem != null) {
            MediaDetailsBottomSheet(
                item = fullItem!!,
                onDismiss = { showDetails = false }
            )
        }
    }
}

@Composable
fun ZoomableImage(
    item: GalleryItem,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 3f
                                val centerX = density.run { maxWidth.toPx() } / 2f
                                val centerY = density.run { maxHeight.toPx() } / 2f
                                offset = Offset(
                                    centerX - tapOffset.x,
                                    centerY - tapOffset.y
                                ) * (scale - 1f)
                            }
                            onZoomChange(scale > 1.05f)
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Use a more cooperative transform detector
                    awaitEachGesture {
                        var zoom = 1f
                        var pan = Offset.Zero
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (!canceled) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                if (!pastTouchSlop) {
                                    zoom *= zoomChange
                                    pan += panChange
                                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                    val zoomMotion = Math.abs(1 - zoom) * centroidSize
                                    val panMotion = pan.getDistance()

                                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                        pastTouchSlop = true
                                    }
                                }

                                if (pastTouchSlop) {
                                    // Only consume if we are already zoomed in or trying to zoom in
                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    val isActuallyZoomed = newScale > 1.05f || scale > 1.05f
                                    
                                    // CRITICAL: Only consume if we are zoomed. 
                                    // If scale is 1f, we DON'T consume horizontal pan to let Pager swipe.
                                    if (isActuallyZoomed) {
                                        val isZooming = zoomChange != 1f
                                        val isPanning = panChange != Offset.Zero
                                        
                                        if (isZooming || isPanning) {
                                            // Special check for HorizontalPager: if scale is 1f and we are only panning horizontally, 
                                            // we might want to NOT consume to let pager swipe. 
                                            // But here we already checked isActuallyZoomed.
                                            event.changes.forEach { it.consume() }
                                        }
                                        scale = newScale
                                        offset += panChange
                                        onZoomChange(scale > 1.05f)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .crossfade(500)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun MediaDetailsBottomSheet(
    item: GalleryItem,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Properties",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PropertyRow("Name", item.name)
            PropertyRow("Path", item.path)
            PropertyRow("Size", formatFileSize(item.size))
            if (item.width > 0 && item.height > 0) {
                PropertyRow("Resolution", "${item.width} x ${item.height}")
            }
            PropertyRow("Type", item.mimeType)
            PropertyRow("Date", formatDate(item.date))
        }
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        size / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}

private fun formatDate(seconds: Long): String {
    val date = Date(seconds * 1000)
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}

private fun shareMedia(context: android.content.Context, item: GalleryItem) {
    try {
        val file = File(item.path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoMediaItem(
    item: GalleryItem,
    isActive: Boolean
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.uri))
            prepare()
            playWhenReady = false
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
