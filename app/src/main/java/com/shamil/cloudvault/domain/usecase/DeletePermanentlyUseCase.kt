package com.shamil.cloudvault.domain.usecase

import com.shamil.cloudvault.domain.repository.IGalleryRepository

class DeletePermanentlyUseCase(private val repository: IGalleryRepository) {
    suspend operator fun invoke(id: Long) = repository.deletePermanently(id)
}
