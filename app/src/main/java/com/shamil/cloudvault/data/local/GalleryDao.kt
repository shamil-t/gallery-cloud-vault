package com.shamil.cloudvault.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM media_items WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getBinMedia(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(items: List<MediaEntity>)

    @Query("DELETE FROM media_items WHERE id NOT IN (:ids)")
    suspend fun deleteRemovedMedia(ids: List<Long>)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun moveToBin(id: Long, timestamp: Long)

    @Query("UPDATE media_items SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromBin(id: Long)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("DELETE FROM media_items WHERE isDeleted = 1 AND deletedAt < :timestamp")
    suspend fun deleteOldBinItems(timestamp: Long)

    @Query("SELECT id FROM media_items WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<Long>

    @Query("SELECT id FROM media_items WHERE isHidden = 1")
    suspend fun getHiddenIds(): List<Long>

    @Query("SELECT * FROM media_items WHERE isDeleted = 1")
    suspend fun getDeletedMedia(): List<MediaEntity>

    @Transaction
    suspend fun syncMedia(items: List<MediaEntity>) {
        val favorites = getFavoriteIds().toSet()
        val hidden = getHiddenIds().toSet()
        val deleted = getDeletedMedia().associateBy { it.id }

        val updatedItems = items.map { item ->
            val deletedItem = deleted[item.id]
            item.copy(
                isFavorite = favorites.contains(item.id),
                isHidden = hidden.contains(item.id),
                isDeleted = deletedItem?.isDeleted ?: false,
                deletedAt = deletedItem?.deletedAt
            )
        }

        insertMedia(updatedItems)
        deleteRemovedMedia(items.map { it.id })
    }
}
