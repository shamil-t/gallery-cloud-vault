package com.shamil.cloudvault.domain.usecase

import com.shamil.cloudvault.domain.model.MediaResult
import com.shamil.cloudvault.domain.repository.IGalleryRepository
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.*

class GetGalleryItemsUseCase(private val repository: IGalleryRepository) {
    operator fun invoke(): Flow<MediaResult<List<GalleryItem>>> = repository.getGalleryItems()
        .map { items -> 
            if (items.isEmpty()) MediaResult.Loading 
            else MediaResult.Success(items) 
        }
        .catch { e -> emit(MediaResult.Error(e)) }
}
