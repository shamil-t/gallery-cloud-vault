package com.shamil.cloudvault.domain.usecase

import androidx.paging.PagingData
import com.shamil.cloudvault.domain.repository.IGalleryRepository
import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.Flow

class GetBinItemsUseCase(private val repository: IGalleryRepository) {
    operator fun invoke(): Flow<PagingData<GalleryItem>> = repository.getBinItems()
}
