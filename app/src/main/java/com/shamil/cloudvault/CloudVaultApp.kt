package com.shamil.cloudvault

import android.app.Application
import com.shamil.cloudvault.BuildConfig
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.shamil.cloudvault.data.network.UpdateCheckWorker
import com.shamil.cloudvault.utils.Constants
import com.shamil.cloudvault.data.GalleryRepository

class CloudVaultApp : Application(), ImageLoaderFactory {
    
    val repository: GalleryRepository by lazy {
        GalleryRepository.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        scheduleUpdateCheck()
    }

    private fun scheduleUpdateCheck() {
        val updateCheckRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "manual_update_check",
            ExistingWorkPolicy.REPLACE,
            updateCheckRequest
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(Constants.MEMORY_CACHE_SIZE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve(Constants.IMAGE_CACHE_DIR))
                    .maxSizePercent(Constants.DISK_CACHE_SIZE_PERCENT)
                    .build()
            }
            .allowHardware(true)
            .crossfade(true)
            .apply {
                // Only enable debug logging in debug builds
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
