package com.shamil.cloudvault.domain.usecase

import com.shamil.cloudvault.domain.repository.IGalleryRepository

class RestoreFromBinUseCase(private val repository: IGalleryRepository) {
    suspend operator fun invoke(id: Long) = repository.restoreFromBin(id)
}
