package com.shamil.cloudvault.domain.repository

import com.shamil.cloudvault.model.GalleryItem
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface IGalleryRepository {
    fun getGalleryItems(): Flow<List<GalleryItem>>
    fun getGalleryItemsPaging(): Flow<PagingData<GalleryItem>>
    fun getBinItems(): Flow<List<GalleryItem>>
    fun getBinItemsPaging(): Flow<PagingData<GalleryItem>>
    fun getFavoriteItemsPaging(): Flow<PagingData<GalleryItem>>
    suspend fun syncMediaStore()
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun moveToBin(id: Long)
    suspend fun restoreFromBin(id: Long)
    suspend fun deletePermanently(id: Long)
    suspend fun cleanupBin()
    suspend fun deleteMedia(id: Long)
}
