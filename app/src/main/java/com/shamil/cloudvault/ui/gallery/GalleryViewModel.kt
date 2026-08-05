package com.shamil.cloudvault.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.concurrent.TimeUnit

sealed class BinUiModel {
    data class Item(val item: GalleryItem) : BinUiModel()
    data class Header(val daysLeft: Int) : BinUiModel()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GalleryRepository(application)
    private val preferenceManager = SettingsPreferenceManager(application)
    private val getGalleryItemsUseCase = GetGalleryItemsUseCase(repository)
    private val moveToBinUseCase = MoveToBinUseCase(repository)
    private val restoreFromBinUseCase = RestoreFromBinUseCase(repository)
    private val deletePermanentlyUseCase = DeletePermanentlyUseCase(repository)
    private val syncGalleryUseCase = SyncGalleryUseCase(repository)
    private val cleanupBinUseCase = CleanupBinUseCase(repository)

    init {
        scheduleBinCleanup()
        viewModelScope.launch {
            cleanupBinUseCase()
            // Periodic sync would be handled by WorkManager or ContentObserver
        }
    }

    private fun scheduleBinCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<BinCleanupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(getApplication())
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

    val galleryItemsPaging: Flow<PagingData<GalleryItem>> = getGalleryItemsUseCase()
        .cachedIn(viewModelScope)

    val favoriteItemsPaging: Flow<PagingData<GalleryItem>> = repository.getFavoriteItems()
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

    private val _searchQuery = MutableStateFlow("")
    val searchResults: Flow<PagingData<GalleryItem>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) emptyFlow()
            else repository.searchMedia(query)
        }
        .cachedIn(viewModelScope)

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun getMediaByFolder(folder: String): Flow<PagingData<GalleryItem>> = 
        repository.getMediaByFolder(folder).cachedIn(viewModelScope)

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

    suspend fun getMediaById(id: Long): GalleryItem? = repository.getMediaById(id)
}
