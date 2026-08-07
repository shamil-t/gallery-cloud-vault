package com.shamil.cloudvault.data.local

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT id, uri, isVideo, isFavorite, date FROM media_items WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllMediaPaging(): PagingSource<Int, MediaThumbnail>

    @Query("SELECT id, uri, isVideo, isFavorite, date FROM media_items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getBinMediaPaging(): PagingSource<Int, MediaThumbnail>

    @Query("SELECT id, uri, isVideo, isFavorite, date FROM media_items WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY date DESC")
    fun getFavoriteMediaPaging(): PagingSource<Int, MediaThumbnail>

    @Query("SELECT id, uri, isVideo, isFavorite, date FROM media_items WHERE isDeleted = 0 AND folder = :folderName ORDER BY date DESC")
    fun getMediaByFolderPaging(folderName: String): PagingSource<Int, MediaThumbnail>

    @Query("""
        SELECT folder, COUNT(*) as itemCount, 
        (SELECT uri FROM media_items m2 WHERE m2.folder = m1.folder AND m2.isDeleted = 0 ORDER BY m2.date DESC LIMIT 1) as coverUri,
        (SELECT isVideo FROM media_items m2 WHERE m2.folder = m1.folder AND m2.isDeleted = 0 ORDER BY m2.date DESC LIMIT 1) as isVideo,
        (SELECT id FROM media_items m2 WHERE m2.folder = m1.folder AND m2.isDeleted = 0 ORDER BY m2.date DESC LIMIT 1) as latestId
        FROM media_items m1
        WHERE isDeleted = 0
        GROUP BY folder
        ORDER BY MAX(date) DESC
    """)
    fun getAlbumsSummary(): Flow<List<AlbumSummary>>

    @Query("SELECT id, uri, isVideo, isFavorite, date FROM media_items WHERE isDeleted = 0 AND name LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchMediaPaging(query: String): PagingSource<Int, MediaThumbnail>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaIgnore(item: MediaEntity): Long

    @Query("""
        UPDATE media_items SET 
        name = :name, uri = :uri, folder = :folder, date = :date, 
        isVideo = :isVideo, size = :size, path = :path, mimeType = :mimeType, 
        width = :width, height = :height, syncGeneration = :syncGeneration 
        WHERE id = :id
    """)
    suspend fun updateMediaStoreFields(
        id: Long, name: String, uri: String, folder: String, date: Long, 
        isVideo: Boolean, size: Long, path: String, mimeType: String, 
        width: Int, height: Int, syncGeneration: Long
    )

    @Transaction
    suspend fun upsertMediaBatch(items: List<MediaEntity>) {
        items.forEach { item ->
            val rowId = insertMediaIgnore(item)
            if (rowId == -1L) {
                updateMediaStoreFields(
                    item.id, item.name, item.uri, item.folder, item.date,
                    item.isVideo, item.size, item.path, item.mimeType,
                    item.width, item.height, item.syncGeneration
                )
            }
        }
    }

    @Query("DELETE FROM media_items WHERE syncGeneration < :currentGeneration AND isDeleted = 0")
    suspend fun deleteOrphanedMedia(currentGeneration: Long)

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

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaEntity?

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getMediaByIdFlow(id: Long): Flow<MediaEntity?>
}
