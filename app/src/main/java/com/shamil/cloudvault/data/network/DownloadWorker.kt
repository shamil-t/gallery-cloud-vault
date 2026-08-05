package com.shamil.cloudvault.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "update.apk"

        createNotificationChannel()
        setForeground(createForegroundInfo(0))

        val outputFile = File(applicationContext.getExternalFilesDir(null), fileName)
        var downloadedBytes = 0L
        if (outputFile.exists()) {
            downloadedBytes = outputFile.length()
        }

        val client = OkHttpClient()
        val requestBuilder = Request.Builder()
            .url(downloadUrl)
        
        if (downloadedBytes > 0) {
            requestBuilder.header("Range", "bytes=$downloadedBytes-")
        }
        
        val request = requestBuilder.build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 206) {
                if (response.code == 416) { // Range Not Satisfiable (maybe file already fully downloaded)
                     return@withContext Result.success(workDataOf(KEY_FILE_PATH to outputFile.absolutePath))
                }
                return@withContext Result.failure()
            }

            val body = response.body ?: return@withContext Result.failure()
            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) {
                downloadedBytes + contentLength
            } else {
                contentLength
            }

            if (downloadedBytes >= totalBytes && totalBytes > 0) {
                 return@withContext Result.success(workDataOf(KEY_FILE_PATH to outputFile.absolutePath))
            }

            RandomAccessFile(outputFile, "rw").use { raf ->
                if (response.code == 206) {
                    raf.seek(downloadedBytes)
                } else {
                    raf.setLength(0)
                }
                
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesReadSize: Int
                    var totalRead = downloadedBytes

                    while (input.read(buffer).also { bytesReadSize = it } != -1) {
                        raf.write(buffer, 0, bytesReadSize)
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
            Result.retry()
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                createNotification(progress),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, createNotification(progress))
        }
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
