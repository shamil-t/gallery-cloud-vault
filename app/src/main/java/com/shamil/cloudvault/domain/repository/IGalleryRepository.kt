package com.shamil.cloudvault.domain.repository

import com.shamil.cloudvault.data.local.AlbumSummary
import com.shamil.cloudvault.model.GalleryItem
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface IGalleryRepository {
    fun getGalleryItems(): Flow<PagingData<GalleryItem>>
    fun getBinItems(): Flow<PagingData<GalleryItem>>
    fun getFavoriteItems(): Flow<PagingData<GalleryItem>>
    fun getMediaByFolder(folder: String): Flow<PagingData<GalleryItem>>
    fun searchMedia(query: String): Flow<PagingData<GalleryItem>>
    fun getAlbums(): Flow<List<AlbumSummary>>
    suspend fun syncMediaStore()
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun moveToBin(id: Long)
    suspend fun restoreFromBin(id: Long)
    suspend fun deletePermanently(id: Long)
    suspend fun cleanupBin()
    suspend fun deleteMedia(id: Long)
    suspend fun getMediaById(id: Long): GalleryItem?
}
