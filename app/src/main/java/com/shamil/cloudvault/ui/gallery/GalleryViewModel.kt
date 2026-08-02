package com.shamil.cloudvault.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shamil.cloudvault.data.GalleryRepository
import com.shamil.cloudvault.domain.model.MediaResult
import com.shamil.cloudvault.domain.usecase.DeleteMediaUseCase
import com.shamil.cloudvault.domain.usecase.GetGalleryItemsUseCase
import com.shamil.cloudvault.domain.usecase.SyncGalleryUseCase
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class GalleryUiState {
    object Loading : GalleryUiState()
    data class Success(val items: List<GalleryItem>) : GalleryUiState()
    data class Error(val message: String) : GalleryUiState()
    object Empty : GalleryUiState()
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GalleryRepository(application)
    private val getGalleryItemsUseCase = GetGalleryItemsUseCase(repository)
    private val deleteMediaUseCase = DeleteMediaUseCase(repository)
    private val syncGalleryUseCase = SyncGalleryUseCase(repository)

    private val _selectedItems = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItems: StateFlow<Set<Long>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    val uiState: StateFlow<GalleryUiState> = getGalleryItemsUseCase()
        .map { result ->
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
                deleteMediaUseCase(id)
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

