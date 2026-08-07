package com.shamil.cloudvault.ui.gallery

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.shamil.cloudvault.CloudVaultApp
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shamil.cloudvault.data.GalleryRepository
import com.shamil.cloudvault.data.local.AlbumSummary
import com.shamil.cloudvault.data.preferences.SettingsPreferenceManager
import com.shamil.cloudvault.data.worker.BinCleanupWorker
import com.shamil.cloudvault.domain.usecase.*
import com.shamil.cloudvault.model.GalleryItem
import com.shamil.cloudvault.utils.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

sealed class BinUiModel {
    data class Item(val item: GalleryItem) : BinUiModel()
    data class Header(val daysLeft: Int) : BinUiModel()
}

sealed class GalleryUiModel {
    data class Item(val item: GalleryItem) : GalleryUiModel()
    data class Header(val date: String) : GalleryUiModel()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GalleryViewModel(
    private val repository: GalleryRepository,
    private val preferenceManager: SettingsPreferenceManager
) : ViewModel() {
    private val getGalleryItemsUseCase = GetGalleryItemsUseCase(repository)
    private val moveToBinUseCase = MoveToBinUseCase(repository)
    private val restoreFromBinUseCase = RestoreFromBinUseCase(repository)
    private val deletePermanentlyUseCase = DeletePermanentlyUseCase(repository)
    private val syncGalleryUseCase = SyncGalleryUseCase(repository)
    private val cleanupBinUseCase = CleanupBinUseCase(repository)

    init {
        // We need application context for WorkManager, so we'll handle scheduling differently or pass it
    }

    fun initWork(application: Application) {
        scheduleBinCleanup(application)
        viewModelScope.launch {
            cleanupBinUseCase()
            refresh() // Initial sync
        }
        
        // Automatic sync when MediaStore changes
        repository.observeMediaStore()
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    private fun scheduleBinCleanup(application: Application) {
        val cleanupRequest = PeriodicWorkRequestBuilder<BinCleanupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(application)
            .enqueueUniquePeriodicWork(
                "BinCleanupWork",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
    }

    private val _selectedItems = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItems: StateFlow<Set<Long>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    val gridColumnCount: StateFlow<Int> = preferenceManager.gridColumnCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Constants.DEFAULT_GRID_COLUMN_COUNT
        )

    val galleryItemsPaging: Flow<PagingData<GalleryUiModel>> = getGalleryItemsUseCase()
        .map { insertDateHeaders(it) }
        .cachedIn(viewModelScope)

    val favoriteItemsPaging: Flow<PagingData<GalleryUiModel>> = repository.getFavoriteItems()
        .map { insertDateHeaders(it) }
        .cachedIn(viewModelScope)

    val albums: Flow<List<AlbumSummary>> = repository.getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val binUiStatePaging: Flow<PagingData<BinUiModel>> = repository.getBinItems()
        .map { pagingData ->
            pagingData.map { BinUiModel.Item(it) }
                .insertSeparators { before, after ->
                    val now = System.currentTimeMillis()
                    val dayMillis = 24 * 60 * 60 * 1000L
                    
                    val daysLeftBefore = before?.item?.deletedAt?.let { 30 - ((now - it) / dayMillis).toInt() }
                    val daysLeftAfter = after?.item?.deletedAt?.let { 30 - ((now - it) / dayMillis).toInt() }

                    if (after != null && daysLeftBefore != daysLeftAfter) {
                        BinUiModel.Header(daysLeftAfter ?: 0)
                    } else {
                        null
                    }
                }
        }
        .cachedIn(viewModelScope)

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: Flow<PagingData<GalleryUiModel>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) emptyFlow()
            else repository.searchMedia(query).map { insertDateHeaders(it) }
        }
        .cachedIn(viewModelScope)

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun getMediaByFolder(folder: String): Flow<PagingData<GalleryUiModel>> = 
        repository.getMediaByFolder(folder)
            .map { insertDateHeaders(it) }
            .cachedIn(viewModelScope)

    private fun insertDateHeaders(pagingData: PagingData<GalleryItem>): PagingData<GalleryUiModel> {
        return pagingData.map { GalleryUiModel.Item(it) as GalleryUiModel }
            .insertSeparators { before, after ->
                val beforeItem = (before as? GalleryUiModel.Item)?.item
                val afterItem = (after as? GalleryUiModel.Item)?.item
                
                if (afterItem != null && (beforeItem == null || !isSameDay(beforeItem.date, afterItem.date))) {
                    GalleryUiModel.Header(formatDateHeader(afterItem.date))
                } else {
                    null
                }
            }
    }

    private fun isSameDay(seconds1: Long, seconds2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = seconds1 * 1000 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = seconds2 * 1000 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDateHeader(seconds: Long): String {
        val now = Calendar.getInstance()
        val itemDate = Calendar.getInstance().apply { timeInMillis = seconds * 1000 }
        
        return when {
            isSameDay(now.timeInMillis / 1000, seconds) -> "Today"
            isSameDay((now.timeInMillis - 24 * 60 * 60 * 1000) / 1000, seconds) -> "Yesterday"
            else -> {
                val format = if (now.get(Calendar.YEAR) == itemDate.get(Calendar.YEAR)) {
                    "MMMM d"
                } else {
                    "MMMM d, yyyy"
                }
                SimpleDateFormat(format, Locale.getDefault()).format(itemDate.time)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            syncGalleryUseCase()
        }
    }

    fun toggleSelection(itemId: Long) {
        _selectedItems.update { current ->
            if (current.contains(itemId)) {
                val next = current - itemId
                if (next.isEmpty()) _isSelectionMode.value = false
                next
            } else {
                _isSelectionMode.value = true
                current + itemId
            }
        }
    }

    fun enterSelectionMode(itemId: Long) {
        _isSelectionMode.value = true
        _selectedItems.value = setOf(itemId)
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedItems.value.forEach { id ->
                moveToBinUseCase(id)
            }
            clearSelection()
        }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            _selectedItems.value.forEach { id ->
                restoreFromBinUseCase(id)
            }
            clearSelection()
        }
    }

    fun permanentlyDeleteSelected() {
        viewModelScope.launch {
            _selectedItems.value.forEach { id ->
                deletePermanentlyUseCase(id)
            }
            clearSelection()
        }
    }

    fun deleteMedia(itemId: Long) {
        viewModelScope.launch {
            moveToBinUseCase(itemId)
        }
    }

    fun toggleFavorite(itemId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(itemId, isFavorite)
        }
    }

    fun updateGridColumnCount(count: Int) {
        viewModelScope.launch {
            preferenceManager.setGridColumnCount(count)
        }
    }

    fun getMediaFlow(id: Long): Flow<GalleryItem?> = repository.getMediaFlow(id)

    suspend fun getMediaById(id: Long): GalleryItem? = repository.getMediaById(id)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as CloudVaultApp
                val repository = application.repository
                val preferenceManager = SettingsPreferenceManager(application)

                return GalleryViewModel(
                    repository = repository,
                    preferenceManager = preferenceManager
                ).apply {
                    initWork(application)
                } as T
            }
        }
    }
}
