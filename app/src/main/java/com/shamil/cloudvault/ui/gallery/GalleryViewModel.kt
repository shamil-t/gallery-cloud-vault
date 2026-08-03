package com.shamil.cloudvault.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shamil.cloudvault.data.GalleryRepository
import com.shamil.cloudvault.data.worker.BinCleanupWorker
import com.shamil.cloudvault.domain.model.MediaResult
import com.shamil.cloudvault.domain.usecase.*
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

sealed class GalleryUiState {
    object Loading : GalleryUiState()
    data class Success(val items: List<GalleryItem>) : GalleryUiState()
    data class Error(val message: String) : GalleryUiState()
    object Empty : GalleryUiState()
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GalleryRepository(application)
    private val getGalleryItemsUseCase = GetGalleryItemsUseCase(repository)
    private val getBinItemsUseCase = GetBinItemsUseCase(repository)
    private val moveToBinUseCase = MoveToBinUseCase(repository)
    private val restoreFromBinUseCase = RestoreFromBinUseCase(repository)
    private val deletePermanentlyUseCase = DeletePermanentlyUseCase(repository)
    private val syncGalleryUseCase = SyncGalleryUseCase(repository)
    private val cleanupBinUseCase = CleanupBinUseCase(repository)

    init {
        scheduleBinCleanup()
        viewModelScope.launch {
            cleanupBinUseCase()
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

    val uiState: StateFlow<GalleryUiState> = getGalleryItemsUseCase()
        .map<MediaResult<List<GalleryItem>>, GalleryUiState> { result ->
            when (result) {
                is MediaResult.Loading -> GalleryUiState.Loading
                is MediaResult.Success -> {
                    if (result.data.isEmpty()) GalleryUiState.Empty
                    else GalleryUiState.Success(result.data)
                }
                is MediaResult.Error -> GalleryUiState.Error(result.exception.message ?: "Unknown error")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GalleryUiState.Loading
        )

    val binUiState: StateFlow<GalleryUiState> = getBinItemsUseCase()
        .map<MediaResult<List<GalleryItem>>, GalleryUiState> { result ->
            when (result) {
                is MediaResult.Loading -> GalleryUiState.Loading
                is MediaResult.Success -> {
                    if (result.data.isEmpty()) GalleryUiState.Empty
                    else GalleryUiState.Success(result.data)
                }
                is MediaResult.Error -> GalleryUiState.Error(result.exception.message ?: "Unknown error")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GalleryUiState.Loading
        )

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
}

