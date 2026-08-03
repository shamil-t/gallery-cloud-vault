package com.shamil.cloudvault.domain.usecase

import com.shamil.cloudvault.domain.model.MediaResult
import com.shamil.cloudvault.domain.repository.IGalleryRepository
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.*

class GetBinItemsUseCase(private val repository: IGalleryRepository) {
    operator fun invoke(): Flow<MediaResult<List<GalleryItem>>> = repository.getBinItems()
        .map<List<GalleryItem>, MediaResult<List<GalleryItem>>> { items -> 
            MediaResult.Success(items) 
        }
        .catch { e -> emit(MediaResult.Error(e)) }
}
