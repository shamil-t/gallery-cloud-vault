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
import com.shamil.cloudvault.data.local.GalleryDatabase
import com.shamil.cloudvault.data.local.MediaEntity
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

    override fun getGalleryItems(): Flow<List<GalleryItem>> = dao.getAllMedia()
        .map { entities ->
            entities.map { it.toDomain() }
        }
        .onStart {
            // Trigger sync when starting to collect
            Logger.d(tag, "Starting to collect gallery items, triggering sync")
            syncMediaStore()
        }
        .catch { exception ->
            Logger.e(tag, "Error collecting gallery items", exception)
        }
        .flowOn(Dispatchers.IO)

    override fun getGalleryItemsPaging(): Flow<PagingData<GalleryItem>> = Pager(
        config = PagingConfig(
            pageSize = Constants.PAGE_SIZE,
            prefetchDistance = Constants.PREFETCH_DISTANCE,
            initialLoadSize = Constants.INITIAL_LOAD_SIZE
        ),
        pagingSourceFactory = { dao.getAllMediaPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toDomain() }
    }

    override fun getBinItems(): Flow<List<GalleryItem>> = dao.getBinMedia()
        .map { entities ->
            entities.map { it.toDomain() }
        }
        .flowOn(Dispatchers.IO)

    override fun getBinItemsPaging(): Flow<PagingData<GalleryItem>> = Pager(
        config = PagingConfig(
            pageSize = Constants.PAGE_SIZE,
            prefetchDistance = Constants.PREFETCH_DISTANCE,
            initialLoadSize = Constants.INITIAL_LOAD_SIZE
        ),
        pagingSourceFactory = { dao.getBinMediaPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toDomain() }
    }

    override fun getFavoriteItemsPaging(): Flow<PagingData<GalleryItem>> = Pager(
        config = PagingConfig(
            pageSize = Constants.PAGE_SIZE,
            prefetchDistance = Constants.PREFETCH_DISTANCE,
            initialLoadSize = Constants.INITIAL_LOAD_SIZE
        ),
        pagingSourceFactory = { dao.getFavoriteMediaPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toDomain() }
    }

    override suspend fun syncMediaStore() = withContext(Dispatchers.IO) {
        try {
            Logger.d(tag, "Syncing media store")
            val mediaStoreItems = fetchMediaStoreItems()
            Logger.d(tag, "Fetched ${mediaStoreItems.size} items from MediaStore")
            dao.syncMedia(mediaStoreItems)
            Logger.i(tag, "Media sync completed successfully")
        } catch (e: Exception) {
            Logger.e(tag, "Error syncing media store", e)
        }
    }

    fun observeMediaStore(): Flow<Unit> = callbackFlow {
        Logger.d(tag, "Registering media store observer")
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Logger.d(tag, "Media store changed")
                trySend(Unit)
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            Logger.d(tag, "Media store observers registered")
        } catch (e: Exception) {
            Logger.e(tag, "Error registering content observer", e)
        }

        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
                Logger.d(tag, "Media store observers unregistered")
            } catch (e: Exception) {
                Logger.e(tag, "Error unregistering content observer", e)
            }
        }
    }.conflate()

    private fun fetchMediaStoreItems(): List<MediaEntity> {
        val list = mutableListOf<MediaEntity>()
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

        try {
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
                    try {
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

                        list.add(
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
                                height = height
                            )
                        )
                    } catch (e: Exception) {
                        Logger.w(tag, "Error processing media item", e)
                    }
                }
            } ?: Logger.w(tag, "ContentResolver query returned null")
        } catch (e: Exception) {
            Logger.e(tag, "Error fetching media store items", e)
        }
        return list
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        try {
            dao.updateFavorite(id, isFavorite)
            Logger.d(tag, "Toggled favorite for item $id to $isFavorite")
        } catch (e: Exception) {
            Logger.e(tag, "Error toggling favorite", e)
        }
    }

    override suspend fun moveToBin(id: Long) = withContext(Dispatchers.IO) {
        try {
            dao.moveToBin(id, System.currentTimeMillis())
            Logger.d(tag, "Moved item $id to bin")
        } catch (e: Exception) {
            Logger.e(tag, "Error moving to bin", e)
        }
    }

    override suspend fun restoreFromBin(id: Long) = withContext(Dispatchers.IO) {
        try {
            dao.restoreFromBin(id)
            Logger.d(tag, "Restored item $id from bin")
        } catch (e: Exception) {
            Logger.e(tag, "Error restoring from bin", e)
        }
    }

    override suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) {
        try {
            dao.deleteMediaById(id)
            Logger.d(tag, "Permanently deleted item $id")
        } catch (e: Exception) {
            Logger.e(tag, "Error permanently deleting", e)
        }
    }

    override suspend fun cleanupBin() = withContext(Dispatchers.IO) {
        try {
            val threshold = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            dao.deleteOldBinItems(threshold)
            Logger.d(tag, "Cleaned up old bin items")
        } catch (e: Exception) {
            Logger.e(tag, "Error cleaning up bin", e)
        }
    }

    override suspend fun deleteMedia(id: Long) = withContext(Dispatchers.IO) {
        moveToBin(id)
    }

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
