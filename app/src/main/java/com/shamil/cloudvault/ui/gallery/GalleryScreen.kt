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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.shamil.cloudvault.R
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
import com.shamil.cloudvault.data.local.AlbumSummary
import com.shamil.cloudvault.data.local.MediaThumbnail
import com.shamil.cloudvault.utils.BitmapUtils
import com.shamil.image_editor_sdk.core.session.EditorSession
import com.shamil.image_editor_sdk.ui.ImageEditorScreen
import com.shamil.image_editor_sdk.renderer.AgslRenderer
import com.shamil.image_editor_sdk.renderer.CanvasRenderer
import com.shamil.image_editor_sdk.renderer.ImageRenderer
import android.graphics.Bitmap
import android.provider.MediaStore
import android.content.ContentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onFullScreenToggle: (Boolean) -> Unit,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val context = LocalContext.current
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val gridColumnCount by viewModel.gridColumnCount.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val galleryItemsPaging = viewModel.galleryItemsPaging.collectAsLazyPagingItems()
    val galleryItemsOnlyPaging = viewModel.galleryItemsOnlyPaging.collectAsLazyPagingItems()
    val searchResultsPaging = viewModel.searchResults.collectAsLazyPagingItems()
    val searchResultsOnlyPaging = viewModel.searchResultsOnly.collectAsLazyPagingItems()

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

    Box(modifier = Modifier.fillMaxSize()) {
        GalleryContent(
            galleryItemsPaging = galleryItemsPaging,
            galleryItemsOnlyPaging = galleryItemsOnlyPaging,
            searchResultsPaging = searchResultsPaging,
            searchResultsOnlyPaging = searchResultsOnlyPaging,
            selectedItems = selectedItems,
            isSelectionMode = isSelectionMode,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            gridColumnCount = gridColumnCount,
            scrollBehavior = scrollBehavior,
            viewModel = viewModel,
            onFullScreenToggle = onFullScreenToggle
        )

        // Only show shimmer as an overlay on initial load to prevent flickering
        if (galleryItemsPaging.loadState.refresh is LoadState.Loading && galleryItemsPaging.itemCount == 0) {
            GalleryShimmer()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryContent(
    galleryItemsPaging: LazyPagingItems<GalleryUiModel>,
    galleryItemsOnlyPaging: LazyPagingItems<GalleryItem>,
    searchResultsPaging: LazyPagingItems<GalleryUiModel>,
    searchResultsOnlyPaging: LazyPagingItems<GalleryItem>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    gridColumnCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: GalleryViewModel,
    onFullScreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = GalleryTab.Albums.ordinal) { GalleryTab.entries.size }
    val coroutineScope = rememberCoroutineScope()

    val favoriteItemsPaging = viewModel.favoriteItemsPaging.collectAsLazyPagingItems()
    val favoriteItemsOnlyPaging = viewModel.favoriteItemsOnlyPaging.collectAsLazyPagingItems()
    val binUiStatePaging = viewModel.binUiStatePaging.collectAsLazyPagingItems()
    val albums by viewModel.albums.collectAsStateWithLifecycle(initialValue = emptyList())

    val recentGridState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()
    val binGridState = rememberLazyGridState()
    val albumGridState = rememberLazyGridState()
    val searchGridState = rememberLazyGridState()

    var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var viewingSearchItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAlbumListView by rememberSaveable { mutableStateOf(false) }

    var editingItem by remember { mutableStateOf<GalleryItem?>(null) }
    var editorSession by remember { mutableStateOf<EditorSession?>(null) }

    val selectedTab = GalleryTab.entries[pagerState.currentPage]

    BackHandler(enabled = viewingItemIndex != null || viewingSearchItemIndex != null || isSelectionMode || selectedAlbumName != null || editingItem != null || isSearchActive) {
        when {
            editingItem != null -> {
                editingItem = null
                editorSession = null
            }
            viewingItemIndex != null -> {
                viewingItemIndex = null
            }
            viewingSearchItemIndex != null -> {
                viewingSearchItemIndex = null
            }
            isSelectionMode -> viewModel.clearSelection()
            selectedAlbumName != null -> selectedAlbumName = null
            isSearchActive -> viewModel.setSearchActive(false)
        }
    }

    // Filtered paging for selected album
    val albumItemsPaging = remember(selectedAlbumName) {
        selectedAlbumName?.let { viewModel.getMediaByFolder(it) }
    }?.collectAsLazyPagingItems()

    val albumItemsOnlyPaging = remember(selectedAlbumName) {
        selectedAlbumName?.let { viewModel.getMediaByFolderOnly(it) }
    }?.collectAsLazyPagingItems()

    Box(modifier = Modifier.fillMaxSize()) {
        val shouldBeFullScreen = viewingItemIndex != null || viewingSearchItemIndex != null || selectedAlbumName != null || editingItem != null
        LaunchedEffect(shouldBeFullScreen) {
            onFullScreenToggle(shouldBeFullScreen)
        }
        
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
                            // Sharing logic
                        },
                        onFavorite = {
                            selectedItems.forEach { id ->
                                viewModel.toggleFavorite(id, true)
                            }
                            viewModel.clearSelection()
                        }
                    )

                } else if (isSearchActive) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.search(it) },
                        onClose = { viewModel.setSearchActive(false) }
                    )
                } else {
                    MediumTopAppBar(
                        title = { Text(selectedAlbumName ?: stringResource(R.string.gallery_title)) },
                        navigationIcon = {
                            if (selectedAlbumName != null) {
                                IconButton(onClick = { selectedAlbumName = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.gallery_back))
                                }
                            }
                        },
                        actions = {
                            if (selectedAlbumName == null) {
                                IconButton(onClick = { viewModel.setSearchActive(true) }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_hint))
                                }
                                if (selectedTab == GalleryTab.Albums) {
                                    IconButton(onClick = { isAlbumListView = !isAlbumListView }) {
                                        Icon(
                                            imageVector = if (isAlbumListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                            contentDescription = stringResource(R.string.gallery_toggle_view)
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (isSearchActive) {
                    if (searchResultsPaging.itemCount == 0 && searchResultsPaging.loadState.refresh is LoadState.NotLoading) {
                        EmptyState(title = stringResource(R.string.search_no_results), description = stringResource(R.string.search_try_different))
                    } else {
                        GalleryGridPaging(
                            galleryItems = searchResultsPaging,
                            columnCount = gridColumnCount,
                            onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                            state = searchGridState,
                            selectedItems = selectedItems,
                            onItemClick = { item, combinedIndex ->
                                if (isSelectionMode) viewModel.toggleSelection(item.id)
                                else {
                                    // Calculate media-only index
                                    var mediaIndex = 0
                                    for (i in 0 until combinedIndex) {
                                        if (searchResultsPaging[i] is GalleryUiModel.Item) mediaIndex++
                                    }
                                    viewingSearchItemIndex = mediaIndex
                                }
                            },
                            onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                        )
                    }
                } else {
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

                    if (selectedAlbumName != null && albumItemsPaging != null) {
                        GalleryGridPaging(
                            galleryItems = albumItemsPaging,
                            columnCount = gridColumnCount,
                            onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                            state = albumGridState,
                            selectedItems = selectedItems,
                            onItemClick = { item, combinedIndex ->
                                if (isSelectionMode) viewModel.toggleSelection(item.id)
                                else {
                                    var mediaIndex = 0
                                    for (i in 0 until combinedIndex) {
                                        if (albumItemsPaging[i] is GalleryUiModel.Item) mediaIndex++
                                    }
                                    viewingItemIndex = mediaIndex
                                }
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
                                GalleryTab.Recent -> {
                                    if (galleryItemsPaging.itemCount == 0 && galleryItemsPaging.loadState.refresh is LoadState.NotLoading) {
                                        EmptyState(title = stringResource(R.string.gallery_no_items), description = stringResource(R.string.gallery_sync_hint))
                                    } else {
                                        GalleryGridPaging(
                                            galleryItems = galleryItemsPaging,
                                            columnCount = gridColumnCount,
                                            onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                                            state = recentGridState,
                                            selectedItems = selectedItems,
                                            onItemClick = { item, combinedIndex ->
                                                if (isSelectionMode) viewModel.toggleSelection(item.id)
                                                else {
                                                    var mediaIndex = 0
                                                    for (i in 0 until combinedIndex) {
                                                        if (galleryItemsPaging[i] is GalleryUiModel.Item) mediaIndex++
                                                    }
                                                    viewingItemIndex = mediaIndex
                                                }
                                            },
                                            onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                                        )
                                    }
                                }

                                GalleryTab.Favorites -> {
                                    if (favoriteItemsPaging.itemCount == 0 && favoriteItemsPaging.loadState.refresh is LoadState.NotLoading) {
                                        EmptyState(title = stringResource(R.string.favorites_empty), description = stringResource(R.string.favorites_hint), icon = Icons.Default.FavoriteBorder)
                                    } else {
                                        GalleryGridPaging(
                                            galleryItems = favoriteItemsPaging,
                                            columnCount = gridColumnCount,
                                            onColumnCountChange = { viewModel.updateGridColumnCount(it) },
                                            state = favoritesGridState,
                                            selectedItems = selectedItems,
                                            onItemClick = { item, combinedIndex ->
                                                if (isSelectionMode) viewModel.toggleSelection(item.id)
                                                else {
                                                    var mediaIndex = 0
                                                    for (i in 0 until combinedIndex) {
                                                        if (favoriteItemsPaging[i] is GalleryUiModel.Item) mediaIndex++
                                                    }
                                                    viewingItemIndex = mediaIndex
                                                }
                                            },
                                            onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                                        )
                                    }
                                }

                                GalleryTab.Albums -> {
                                    if (albums.isEmpty()) {
                                        EmptyState(title = stringResource(R.string.albums_empty), icon = Icons.Default.Folder)
                                    } else {
                                        if (isAlbumListView) {
                                            AlbumSummaryList(albums) { selectedAlbumName = it.folder }
                                        } else {
                                            AlbumSummaryGrid(albums) { selectedAlbumName = it.folder }
                                        }
                                    }
                                }

                                GalleryTab.Bin -> {
                                    if (binUiStatePaging.itemCount == 0 && binUiStatePaging.loadState.refresh is LoadState.NotLoading) {
                                        EmptyState(title = stringResource(R.string.bin_empty), description = stringResource(R.string.bin_hint), icon = Icons.Default.DeleteOutline)
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
        }

        if (viewingItemIndex != null || viewingSearchItemIndex != null) {
            val currentPagingItems = when {
                viewingSearchItemIndex != null -> searchResultsOnlyPaging
                selectedAlbumName != null -> albumItemsOnlyPaging
                selectedTab == GalleryTab.Recent -> galleryItemsOnlyPaging
                selectedTab == GalleryTab.Favorites -> favoriteItemsOnlyPaging
                else -> null
            }
            
            if (currentPagingItems != null) {
                MediaViewerPaging(
                    items = currentPagingItems,
                    initialIndex = viewingSearchItemIndex ?: viewingItemIndex!!,
                    viewModel = viewModel,
                    onBack = {
                        viewingItemIndex = null
                        viewingSearchItemIndex = null
                    },
                    onEdit = { item ->
                        coroutineScope.launch {
                            val bitmap = BitmapUtils.loadBitmap(context, item.uri)
                            if (bitmap != null) {
                                editorSession = EditorSession(bitmap)
                                editingItem = item
                            }
                        }
                    }
                )
            }
        }

        if (editingItem != null && editorSession != null) {
            var isSaving by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }

            Box(modifier = Modifier.fillMaxSize()) {
                ImageEditorScreen(
                    session = editorSession!!,
                    onClose = {
                        editingItem = null
                        editorSession = null
                    },
                    onSave = {
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                val state = editorSession!!.state.value
                                val renderer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    AgslRenderer(editorSession!!.sourceBitmap)
                                } else {
                                    CanvasRenderer(editorSession!!.sourceBitmap)
                                }
                                
                                val finalBitmap = withContext(Dispatchers.Default) {
                                    renderer.renderFull(state)
                                }

                                if (finalBitmap != null) {
                                    saveBitmapToMediaStore(context, finalBitmap, editingItem!!.name)
                                    snackbarHostState.showSnackbar("Image saved successfully")
                                    viewModel.refresh()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                snackbarHostState.showSnackbar("Failed to save image")
                            } finally {
                                isSaving = false
                                editingItem = null
                                editorSession = null
                            }
                        }
                    }
                )

                if (isSaving) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.saving_progress), color = Color.White)
                            }
                        }
                    }
                }
                
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                )
            }
        }
    }
}

private suspend fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, originalName: String) = withContext(Dispatchers.IO) {
    val name = "Edited_${System.currentTimeMillis()}_$originalName"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CloudVault")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }

        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(it, contentValues, null, null)
    }
}

@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String? = null,
    icon: ImageVector = Icons.Default.PhotoLibrary
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 80.dp),
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
    galleryItems: LazyPagingItems<GalleryUiModel>,
    columnCount: Int,
    onColumnCountChange: (Int) -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    selectedItems: Set<Long> = emptySet(),
    onItemClick: (GalleryItem, Int) -> Unit,
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
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = galleryItems.itemCount,
            key = galleryItems.itemKey { model ->
                when (model) {
                    is GalleryUiModel.Item -> "item_${model.item.id}"
                    is GalleryUiModel.Header -> "header_${model.date}"
                }
            },
            span = { index ->
                when (galleryItems[index]) {
                    is GalleryUiModel.Header -> GridItemSpan(maxLineSpan)
                    else -> GridItemSpan(1)
                }
            }
        ) { index ->
            when (val model = galleryItems[index]) {
                is GalleryUiModel.Header -> DateHeader(date = model.date)
                is GalleryUiModel.Item -> {
                    GalleryItem(
                        item = model.item,
                        isSelected = selectedItems.contains(model.item.id),
                        onClick = { onItemClick(it, index) },
                        onLongClick = onItemLongClick
                    )
                }
                null -> {}
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
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
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 80.dp),
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
fun AlbumSummaryList(
    albumItems: List<AlbumSummary>,
    onAlbumClick: (AlbumSummary) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(albumItems, key = { it.folder }) { album ->
            ListItem(
                headlineContent = { Text(album.folder) },
                supportingContent = { Text("${album.itemCount} items") },
                leadingContent = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.coverUri)
                            .size(300)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                },
                modifier = Modifier.clickable { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumSummaryGrid(
    albumItems: List<AlbumSummary>,
    onAlbumClick: (AlbumSummary) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = albumItems,
            key = { it.folder }
        ) { album ->
            AlbumSummaryItem(
                album = album,
                onClick = onAlbumClick
            )
        }
    }
}

@Composable
fun AlbumSummaryItem(
    album: AlbumSummary,
    modifier: Modifier = Modifier,
    onClick: (AlbumSummary) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(album) },
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.coverUri)
                    .apply {
                        if (album.isVideo) {
                            videoFrameMicros(1000000)
                        }
                    }
                    .size(500)
                    .crossfade(300)
                    .build(),
                contentDescription = album.folder,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = album.folder,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${album.itemCount} items",
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
                .size(300) // Optimal thumbnail size
                .apply {
                    if (item.isVideo) {
                        videoFrameMicros(1000000)
                    }
                }
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(300) // Smooth crossfade like Google Photos
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
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
