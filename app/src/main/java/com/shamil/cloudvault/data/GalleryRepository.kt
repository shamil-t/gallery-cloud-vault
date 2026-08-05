package com.shamil.cloudvault.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.shamil.cloudvault.data.local.AlbumSummary
import com.shamil.cloudvault.data.local.GalleryDatabase
import com.shamil.cloudvault.data.local.MediaEntity
import com.shamil.cloudvault.data.local.MediaThumbnail
import com.shamil.cloudvault.domain.repository.IGalleryRepository
import com.shamil.cloudvault.model.GalleryItem
import com.shamil.cloudvault.utils.Constants
import com.shamil.cloudvault.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class GalleryRepository(private val context: Context) : IGalleryRepository {
    private val database = GalleryDatabase.getDatabase(context)
    private val dao = database.galleryDao()
    private val tag = Constants.TAG_REPOSITORY

    private val pagingConfig = PagingConfig(
        pageSize = Constants.PAGE_SIZE,
        prefetchDistance = Constants.PREFETCH_DISTANCE,
        initialLoadSize = Constants.INITIAL_LOAD_SIZE,
        enablePlaceholders = true
    )

    override fun getGalleryItems(): Flow<PagingData<GalleryItem>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { dao.getAllMediaPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toGalleryItem() }
    }

    override fun getBinItems(): Flow<PagingData<GalleryItem>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { dao.getBinMediaPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toGalleryItem() }
    }

    override fun getFavoriteItems(): Flow<PagingData<GalleryItem>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { dao.getFavoriteMediaPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toGalleryItem() }
    }

    override fun getMediaByFolder(folder: String): Flow<PagingData<GalleryItem>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { dao.getMediaByFolderPaging(folder) }
    ).flow.map { pagingData ->
        pagingData.map { it.toGalleryItem() }
    }

    override fun searchMedia(query: String): Flow<PagingData<GalleryItem>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { dao.searchMediaPaging(query) }
    ).flow.map { pagingData ->
        pagingData.map { it.toGalleryItem() }
    }

    override fun getAlbums(): Flow<List<AlbumSummary>> = dao.getAlbumsSummary()

    override suspend fun syncMediaStore() = withContext(Dispatchers.IO) {
        try {
            Logger.d(tag, "Syncing media store incrementally")
            val currentGeneration = System.currentTimeMillis()
            fetchAndSyncMediaStoreItems(currentGeneration)
            dao.deleteOrphanedMedia(currentGeneration)
            Logger.i(tag, "Incremental media sync completed")
        } catch (e: Exception) {
            Logger.e(tag, "Error syncing media store", e)
        }
    }

    private suspend fun fetchAndSyncMediaStoreItems(generation: Long) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val batchSize = 500
        val batch = mutableListOf<MediaEntity>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown"
                val type = cursor.getInt(typeCol)
                val folder = cursor.getString(folderCol) ?: "Unknown"
                val date = cursor.getLong(dateCol)
                val size = cursor.getLong(sizeCol)
                val path = cursor.getString(pathCol) ?: ""
                val mimeType = cursor.getString(mimeCol) ?: "image/*"
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)

                val uri = ContentUris.withAppendedId(collection, id)

                batch.add(
                    MediaEntity(
                        id = id,
                        name = name,
                        uri = uri.toString(),
                        folder = folder,
                        date = date,
                        isVideo = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        size = size,
                        path = path,
                        mimeType = mimeType,
                        width = width,
                        height = height,
                        syncGeneration = generation
                    )
                )

                if (batch.size >= batchSize) {
                    dao.upsertMediaBatch(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                dao.upsertMediaBatch(batch)
            }
        }
    }

    fun observeMediaStore(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.conflate()

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, isFavorite)
    }

    override suspend fun moveToBin(id: Long) = withContext(Dispatchers.IO) {
        dao.moveToBin(id, System.currentTimeMillis())
    }

    override suspend fun restoreFromBin(id: Long) = withContext(Dispatchers.IO) {
        dao.restoreFromBin(id)
    }

    override suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteMediaById(id)
    }

    override suspend fun cleanupBin() = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        dao.deleteOldBinItems(threshold)
    }

    override suspend fun deleteMedia(id: Long) = moveToBin(id)

    override suspend fun getMediaById(id: Long): GalleryItem? = dao.getMediaById(id)?.toDomain()

    private fun MediaThumbnail.toGalleryItem() = GalleryItem(
        id = this.id,
        name = "", // Not needed for grid
        uri = Uri.parse(this.uri),
        folder = "",
        date = this.date,
        isVideo = this.isVideo,
        size = 0,
        path = "",
        mimeType = "",
        width = 0,
        height = 0,
        isFavorite = this.isFavorite
    )

    private fun MediaEntity.toDomain() = GalleryItem(
        id = this.id,
        name = this.name,
        uri = Uri.parse(this.uri),
        folder = this.folder,
        date = this.date,
        isVideo = this.isVideo,
        size = this.size,
        path = this.path,
        mimeType = this.mimeType,
        width = this.width,
        height = this.height,
        isFavorite = this.isFavorite,
        isDeleted = this.isDeleted,
        deletedAt = this.deletedAt
    )
}
