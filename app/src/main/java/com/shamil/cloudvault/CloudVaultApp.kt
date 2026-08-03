package com.shamil.cloudvault

import android.app.Application
import androidx.media3.common.BuildConfig
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.shamil.cloudvault.utils.Constants

class CloudVaultApp : Application(), ImageLoaderFactory {
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
