package com.shamil.cloudvault.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shamil.cloudvault.data.GalleryRepository
import com.shamil.cloudvault.utils.Logger

class BinCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Logger.d("BinCleanupWorker", "Starting bin cleanup")
            val repository = GalleryRepository(applicationContext)
            repository.cleanupBin()
            Logger.d("BinCleanupWorker", "Bin cleanup completed")
            Result.success()
        } catch (e: Exception) {
            Logger.e("BinCleanupWorker", "Error during bin cleanup", e)
            Result.retry()
        }
    }
}
