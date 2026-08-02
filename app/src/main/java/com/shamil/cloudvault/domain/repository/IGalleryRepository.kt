package com.shamil.cloudvault.domain.repository

import com.shamil.cloudvault.model.GalleryItem
import kotlinx.coroutines.flow.Flow

interface IGalleryRepository {
    fun getGalleryItems(): Flow<List<GalleryItem>>
    suspend fun syncMediaStore()
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun deleteMedia(id: Long)
}
