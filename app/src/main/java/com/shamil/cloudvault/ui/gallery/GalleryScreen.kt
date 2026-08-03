@file:OptIn(ExperimentalMaterial3Api::class)
package com.shamil.cloudvault.ui.gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onFullScreenToggle: (Boolean) -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val binUiState by viewModel.binUiState.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

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

    when (val state = uiState) {
        is GalleryUiState.Loading -> GalleryShimmer()
        is GalleryUiState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No items found")
            }
        }
        is GalleryUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}")
            }
        }
        is GalleryUiState.Success -> {
            GalleryContent(
                items = state.items,
                binItems = (binUiState as? GalleryUiState.Success)?.items ?: emptyList(),
                selectedItems = selectedItems,
                isSelectionMode = isSelectionMode,
                scrollBehavior = scrollBehavior,
                viewModel = viewModel,
                onFullScreenToggle = onFullScreenToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryContent(
    items: List<GalleryItem>,
    binItems: List<GalleryItem>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: GalleryViewModel,
    onFullScreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState { GalleryTab.entries.size }
    val coroutineScope = rememberCoroutineScope()
    
    var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAlbumListView by rememberSaveable { mutableStateOf(false) }

    val selectedTab = GalleryTab.entries[pagerState.currentPage]

    val itemsToDisplay = remember(items, selectedAlbumName) {
        if (selectedAlbumName != null) {
            items.filter { it.folder == selectedAlbumName }
        } else {
            items
        }
    }

    if (viewingItemIndex != null) {
        onFullScreenToggle(true)
        MediaViewer(
            items = itemsToDisplay,
            initialIndex = viewingItemIndex!!,
            onBack = {
                viewingItemIndex = null
                onFullScreenToggle(false)
            }
        )
        return
    }

    onFullScreenToggle(false)
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
                        val itemsToShare = (if (selectedTab == GalleryTab.Bin) binItems else items).filter { selectedItems.contains(it.id) }
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
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }



            if (selectedAlbumName != null) {
                GalleryGrid(
                    galleryItems = itemsToDisplay,
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
                        GalleryTab.Recent -> GalleryGrid(
                            galleryItems = items,
                            selectedItems = selectedItems,
                            onItemClick = { item ->
                                if (isSelectionMode) viewModel.toggleSelection(item.id)
                                else viewingItemIndex = items.indexOf(item)
                            },
                            onItemLongClick = { item -> viewModel.enterSelectionMode(item.id) }
                        )

                        GalleryTab.Favorites -> {
                            val favoriteItems = remember(items) { items.filter { it.isFavorite } }
                            GalleryGrid(
                                galleryItems = favoriteItems,
                                selectedItems = selectedItems,
                                onItemClick = { item ->
                                    if (isSelectionMode) viewModel.toggleSelection(item.id)
                                    else viewingItemIndex = items.indexOf(item)
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
                            if (binItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Bin is empty")
                                }
                            } else {
                                GalleryGrid(
                                    galleryItems = binItems,
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
fun GalleryGrid(
    galleryItems: List<GalleryItem>,
    selectedItems: Set<Long> = emptySet(),
    onItemClick: (GalleryItem) -> Unit,
    onItemLongClick: (GalleryItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        modifier = Modifier.fillMaxSize(),
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
