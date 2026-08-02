@file:OptIn(ExperimentalMaterial3Api::class)
package com.shamil.cloudvault.ui.gallery

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shamil.cloudvault.data.GalleryRepository
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaViewer(
    items: List<GalleryItem>,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    var showDetails by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { GalleryRepository(context) }

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
            if (page < items.size) {
                val item = items[page]
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
        }

        // Top Bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp)
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
            IconButton(onClick = {
                val currentItem = items[pagerState.currentPage]
                shareMedia(context, currentItem)
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(onClick = {
                val currentItem = items[pagerState.currentPage]
                coroutineScope.launch {
                    repository.toggleFavorite(currentItem.id, !currentItem.isFavorite)
                }
            }) {
                val isFavorite = if (pagerState.currentPage < items.size) items[pagerState.currentPage].isFavorite else false
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White
                )
            }
            IconButton(onClick = { showDetails = true }) {
                Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
            }
            IconButton(onClick = {
                val currentItem = items[pagerState.currentPage]
                coroutineScope.launch {
                    // In a real app, we'd use MediaStore to delete the file
                    // For now, we'll just remove it from our local DB
                    repository.deleteMedia(currentItem.id)
                    // If it was the last item, go back
                    if (items.size <= 1) {
                        onBack()
                    }
                }
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }

        if (showDetails && pagerState.currentPage < items.size) {
            MediaDetailsBottomSheet(
                item = items[pagerState.currentPage],
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.fastAny { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange

                                val centroid = event.calculateCentroid(useCurrent = false)
                                val panMotion = pan.getDistance()
                                val zoomMotion = abs(1 - zoom) * centroid.getDistance()

                                if (panMotion > touchSlop || zoomMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                if (scale > 1f || zoomChange != 1f) {
                                    // If we are zoomed in or starting to zoom, we consume the events
                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    val newOffset = if (newScale > 1f) offset + panChange else Offset.Zero
                                    
                                    scale = newScale
                                    offset = newOffset
                                    onZoomChange(newScale > 1f)

                                    event.changes.fastForEach {
                                        if (it.positionChanged()) {
                                            it.consume()
                                        }
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.fastAny { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            modifier = Modifier
                .fillMaxSize()
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
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
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
            playWhenReady = false // Don't autoplay by default
        }
    }

    // Handle playback based on active state
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
                    // Ensure the player doesn't aggressively steal all touches
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
