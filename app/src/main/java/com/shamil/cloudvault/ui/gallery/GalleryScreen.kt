@file:OptIn(ExperimentalMaterial3Api::class)
package com.shamil.cloudvault.ui.gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import coil.size.Size
import com.shamil.cloudvault.model.AlbumItem
import com.shamil.cloudvault.model.GalleryItem
import com.shamil.cloudvault.model.GalleryTab
import com.shamil.cloudvault.ui.components.GalleryShimmer
import com.shamil.cloudvault.utils.Constants
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onFullScreenToggle: (Boolean) -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val allItems by viewModel.allGalleryItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val gridColumnCount by viewModel.gridColumnCount.collectAsStateWithLifecycle()

    val galleryItemsPaging = viewModel.galleryItemsPaging.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            hasPermission = true
            viewModel.refresh()
        }
        Log.d("GalleryScreen :: Permissions", permissions.toString())
    }

    fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            requestPermissions()
        }
    }

    if (!hasPermission) {
        PermissionScreen(onRequestPermission = { requestPermissions() })
        return
    }

    // Paging handles its own states
    if (galleryItemsPaging.loadState.refresh is LoadState.Loading) {
        GalleryShimmer()
    } else {
        GalleryContent(
            items = allItems,
            galleryItemsPaging = galleryItemsPaging,
            selectedItems = selectedItems,
            isSelectionMode = isSelectionMode,
            gridColumnCount = gridColumnCount,
            scrollBehavior = scrollBehavior,
            viewModel = viewModel,
            onFullScreenToggle = onFullScreenToggle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryContent(
    items: List<GalleryItem>,
    galleryItemsPaging: LazyPagingItems<GalleryItem>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    gridColumnCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: GalleryViewModel,
    onFullScreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState { GalleryTab.entries.size }
    val coroutineScope = rememberCoroutineScope()

    val favoriteItemsPaging = viewModel.favoriteItemsPaging.collectAsLazyPagingItems()
    val binUiStatePaging = viewModel.binUiStatePaging.collectAsLazyPagingItems()

    val recentGridState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()
    val binGridState = rememberLazyGridState()
    val albumGridState = rememberLazyGridState()

    var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAlbumListView by rememberSaveable { mutableStateOf(false) }

    val selectedTab = GalleryTab.entries[pagerState.currentPage]

    BackHandler(enabled = viewingItemIndex != null || isSelectionMode || selectedAlbumName != null) {
        when {
            viewingItemIndex != null -> {
                viewingItemIndex = null
                onFullScreenToggle(false)
            }
            isSelectionMode -> viewModel.clearSelection()
            selectedAlbumName != null -> selectedAlbumName = null
        }
    }

    val itemsToDisplay = remember(items, selectedAlbumName) {
        if (selectedAlbumName != null) {
            items.filter { it.folder == selectedAlbumName }
        } else {
            items
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        onFullScreenToggle(viewingItemIndex != null)
        
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedItems.size,
                        isInBin = selectedTab == GalleryTab.Bin,
                        onCancel = { viewModel.clearSelection() },
                        onDelete = { viewModel.deleteSelected() },
                        onRestore = { viewModel.restoreSelected() },
                        onDeletePermanently = { viewModel.permanentlyDeleteSelected() },
                        onShare = {
                            val itemsToShare = items.filter { selectedItems.contains(it.id) }
                            if (itemsToShare.isNotEmpty()) shareMultipleMedia(context, itemsToShare)
                            viewModel.clearSelection()
                        },
                        onFavorite = {
                            selectedItems.forEach { id ->
                                viewModel.toggleFavorite(id, true)
                            }
                            viewModel.clearSelection()
                        }
                    )

                } else {
                    MediumTopAppBar(
                        title = { Text(selectedAlbumName ?: "Gallery") },
                        navigationIcon = {
                            if (selectedAlbumName != null) {
                                IconButton(onClick = { selectedAlbumName = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            if (selectedAlbumName == null && selectedTab == GalleryTab.Albums) {
                                IconButton(onClick = { isAlbumListView = !isAlbumListView }) {
                                    Icon(
                                        imageVector = if (isAlbumListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                        contentDescription = "Toggle View"
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (selectedAlbumName == null && !isSelectionMode) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        GalleryTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (tab.title.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedAlbumName != null) {
                    GalleryGrid(
                        galleryItems = itemsToDisplay,
                        columnCount = gridColumnCount,
                        onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                        state = albumGridState,
                        selectedItems = selectedItems,
                        onItemClick = { item ->
                            if (isSelectionMode) viewModel.toggleSelection(item.id)
                            else viewingItemIndex = itemsToDisplay.indexOf(item)
                        },
                        onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isSelectionMode
                    ) { page ->
                        when (GalleryTab.entries[page]) {
                            GalleryTab.Recent -> GalleryGridPaging(
                                galleryItems = galleryItemsPaging,
                                columnCount = gridColumnCount,
                                onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                                state = recentGridState,
                                selectedItems = selectedItems,
                                onItemClick = { item ->
                                    if (isSelectionMode) viewModel.toggleSelection(item.id)
                                    else {
                                        // Paging3 indexOf is expensive, we find it in the allItems list
                                        viewingItemIndex = items.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                                    }
                                },
                                onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                            )

                            GalleryTab.Favorites -> {
                                GalleryGridPaging(
                                    galleryItems = favoriteItemsPaging,
                                    columnCount = gridColumnCount,
                                    onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                                    state = favoritesGridState,
                                    selectedItems = selectedItems,
                                    onItemClick = { item ->
                                        if (isSelectionMode) viewModel.toggleSelection(item.id)
                                        else {
                                            // Show only favorites in viewer if possible, or all
                                            viewingItemIndex = items.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                                        }
                                    },
                                    onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                                )
                            }

                            GalleryTab.Albums -> {
                                val albums = remember(items) {
                                    items.groupBy { it.folder }
                                        .map { (name, media) ->
                                            val firstItem = media.first()
                                            AlbumItem(
                                                id = firstItem.id,
                                                name = name,
                                                cover = firstItem.uri,
                                                count = media.size,
                                                isVideo = firstItem.isVideo
                                            )
                                        }
                                }
                                if (isAlbumListView) {
                                    AlbumList(albums) { selectedAlbumName = it.name }
                                } else {
                                    AlbumGrid(albums) { selectedAlbumName = it.name }
                                }
                            }

                            GalleryTab.Bin -> {
                                if (binUiStatePaging.itemCount == 0 && binUiStatePaging.loadState.refresh is LoadState.NotLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Bin is empty")
                                    }
                                } else {
                                    BinGridPaging(
                                        binUiModels = binUiStatePaging,
                                        columnCount = gridColumnCount,
                                        onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                                        state = binGridState,
                                        selectedItems = selectedItems,
                                        onItemClick = { item ->
                                            if (isSelectionMode) viewModel.toggleSelection(item.id)
                                        },
                                        onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (viewingItemIndex != null) {
            MediaViewer(
                items = itemsToDisplay,
                initialIndex = viewingItemIndex!!,
                onBack = {
                    viewingItemIndex = null
                    onFullScreenToggle(false)
                }
            )
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    isInBin: Boolean = false,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit = {},
    onDeletePermanently: () -> Unit = {},
    onShare: () -> Unit,
    onFavorite: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        },
        actions = {
            if (isInBin) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore")
                }
                IconButton(onClick = onDeletePermanently) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently")
                }
            } else {
                IconButton(onClick = onFavorite) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}


@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome to Cloud Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To view and manage your media, we need permission to access your gallery.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Access")
            }
        }
    }
}

@Composable
fun BinGridPaging(
    binUiModels: LazyPagingItems<BinUiModel>,
    columnCount: Int,
    onColumnCountChange: (Int) -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    selectedItems: Set<Long> = emptySet(),
    onItemClick: (GalleryItem) -> Unit,
    onItemLongClick: (GalleryItem) -> Unit
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(columnCount) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                event.changes.forEach { it.consume() }
                                zoomScale *= zoom
                                if (zoomScale > 1.4f) {
                                    if (columnCount > Constants.MIN_GRID_COLUMNS) {
                                        onColumnCountChange(columnCount - 1)
                                        zoomScale = 1f
                                    }
                                } else if (zoomScale < 0.7f) {
                                    if (columnCount < Constants.MAX_GRID_COLUMNS) {
                                        onColumnCountChange(columnCount + 1)
                                        zoomScale = 1f
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = binUiModels.itemCount,
            key = binUiModels.itemKey { model ->
                when (model) {
                    is BinUiModel.Item -> "item_${model.item.id}"
                    is BinUiModel.Header -> "header_${model.daysLeft}"
                }
            },
            span = { index ->
                when (binUiModels[index]) {
                    is BinUiModel.Header -> GridItemSpan(maxLineSpan)
                    else -> GridItemSpan(1)
                }
            }
        ) { index ->
            when (val model = binUiModels[index]) {
                is BinUiModel.Header -> BinHeader(daysLeft = model.daysLeft)
                is BinUiModel.Item -> GalleryItem(
                    item = model.item,
                    isSelected = selectedItems.contains(model.item.id),
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
                null -> {}
            }
        }
    }
}

@Composable
fun BinHeader(daysLeft: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = if (daysLeft == 1) "1 day until permanent deletion" else "$daysLeft days until permanent deletion",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GalleryGridPaging(
    galleryItems: LazyPagingItems<GalleryItem>,
    columnCount: Int,
    onColumnCountChange: (Int) -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    selectedItems: Set<Long> = emptySet(),
    onItemClick: (GalleryItem) -> Unit,
    onItemLongClick: (GalleryItem) -> Unit
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(columnCount) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                event.changes.forEach { it.consume() }
                                zoomScale *= zoom
                                if (zoomScale > 1.4f) {
                                    if (columnCount > Constants.MIN_GRID_COLUMNS) {
                                        onColumnCountChange(columnCount - 1)
                                        zoomScale = 1f
                                    }
                                } else if (zoomScale < 0.7f) {
                                    if (columnCount < Constants.MAX_GRID_COLUMNS) {
                                        onColumnCountChange(columnCount + 1)
                                        zoomScale = 1f
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = galleryItems.itemCount,
            key = galleryItems.itemKey { it.id }
        ) { index ->
            val item = galleryItems[index]
            if (item != null) {
                GalleryItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
            }
        }
    }
}

@Composable
fun GalleryGrid(
    galleryItems: List<GalleryItem>,
    columnCount: Int,
    onColumnCountChange: (Int) -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    selectedItems: Set<Long> = emptySet(),
    onItemClick: (GalleryItem) -> Unit,
    onItemLongClick: (GalleryItem) -> Unit
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(columnCount) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                event.changes.forEach { it.consume() }
                                zoomScale *= zoom
                                if (zoomScale > 1.4f) {
                                    if (columnCount > Constants.MIN_GRID_COLUMNS) {
                                        onColumnCountChange(columnCount - 1)
                                        zoomScale = 1f
                                    }
                                } else if (zoomScale < 0.7f) {
                                    if (columnCount < Constants.MAX_GRID_COLUMNS) {
                                        onColumnCountChange(columnCount + 1)
                                        zoomScale = 1f
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = galleryItems, key = { it.id }) { item ->
            GalleryItem(
                item = item,
                isSelected = selectedItems.contains(item.id),
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
        }
    }
}

@Composable
fun AlbumList(
    albumItems: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(albumItems, key = { it.id }) { album ->
            ListItem(
                headlineContent = { Text(album.name) },
                supportingContent = { Text("${album.count} items") },
                leadingContent = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.cover)
                            .size(Size.ORIGINAL)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                },
                modifier = Modifier.clickable { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumGrid(
    albumItems: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = albumItems,
            key = { it.id }
        ) { album ->
            AlbumItem(
                album = album,
                onClick = onAlbumClick
            )
        }
    }
}

@Composable
fun AlbumItem(
    album: AlbumItem,
    modifier: Modifier = Modifier,
    onClick: (AlbumItem) -> Unit
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(album) },
    ) {

        Column {

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.cover)
                    .apply {
                        if (album.isVideo) {
                            videoFrameMicros(1000000)
                        }
                    }
                    .crossfade(true)
                    .build(),
                contentDescription = album.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(8.dp)
            ) {

                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${album.count} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(
    modifier: Modifier = Modifier,
    item: GalleryItem,
    isSelected: Boolean = false,
    onClick: (GalleryItem) -> Unit = {},
    onLongClick: (GalleryItem) -> Unit = {}
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = { onClick(item) },
                onLongClick = { onLongClick(item) }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .size(256) // Fast thumbnail size
                .apply {
                    if (item.isVideo) {
                        videoFrameMicros(1000000)
                    }
                }
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(false) // Disable crossfade for "immediate" feel
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = "Video",
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )
        }
        
        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        
        if (isSelected) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

private fun shareMultipleMedia(context: Context, items: List<GalleryItem>) {
    try {
        val uris = ArrayList(items.map { item ->
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(item.path))
        })
        
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (items.all { !it.isVideo }) "image/*" else if (items.all { it.isVideo }) "video/*" else "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Preview
@Composable
private fun PreviewGallery() {
    GalleryScreen(onFullScreenToggle = {})
}
