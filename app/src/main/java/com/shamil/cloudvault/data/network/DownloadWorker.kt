package com.shamil.cloudvault.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "update.apk"

        createNotificationChannel()
        setForeground(createForegroundInfo(0))

        val client = OkHttpClient()
        val request = Request.Builder().url(downloadUrl).build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return Result.failure()

            val body = response.body ?: return Result.failure()
            val totalBytes = body.contentLength()
            val outputFile = File(applicationContext.getExternalFilesDir(null), fileName)

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesReadSize: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesReadSize = it } != -1) {
                        output.write(buffer, 0, bytesReadSize)
                        totalRead += bytesReadSize
                        if (totalBytes > 0) {
                            val progress = (totalRead * 100 / totalBytes).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                            notificationManager.notify(NOTIFICATION_ID, createNotification(progress))
                        }
                    }
                }
            }

            Result.success(workDataOf(KEY_FILE_PATH to outputFile.absolutePath))
        } catch (e: IOException) {
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, createNotification(progress))
    }

    private fun createNotification(progress: Int) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading Update")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

    companion object {
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "updates"
    }
}
