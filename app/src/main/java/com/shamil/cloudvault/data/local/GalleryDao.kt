package com.shamil.cloudvault.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM media_items ORDER BY date DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(items: List<MediaEntity>)

    @Query("DELETE FROM media_items WHERE id NOT IN (:ids)")
    suspend fun deleteRemovedMedia(ids: List<Long>)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("SELECT id FROM media_items WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<Long>

    @Query("SELECT id FROM media_items WHERE isHidden = 1")
    suspend fun getHiddenIds(): List<Long>
    
    @Transaction
    suspend fun syncMedia(items: List<MediaEntity>) {
        val favorites = getFavoriteIds().toSet()
        val hidden = getHiddenIds().toSet()

        val updatedItems = items.map {
            it.copy(
                isFavorite = favorites.contains(it.id),
                isHidden = hidden.contains(it.id)
            )
        }

        insertMedia(updatedItems)
        deleteRemovedMedia(items.map { it.id })
    }
}
