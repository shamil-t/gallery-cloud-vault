package com.shamil.cloudvault.domain.usecase

import com.shamil.cloudvault.domain.repository.IGalleryRepository

class SyncGalleryUseCase(private val repository: IGalleryRepository) {
    suspend operator fun invoke() = repository.syncMediaStore()
}
