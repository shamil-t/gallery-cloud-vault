package com.shamil.cloudvault.data.network

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.shamil.cloudvault.data.model.UpdateInfo
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "UpdateManager"
    }

    suspend fun checkForUpdates(url: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    try {
                        val info = json.decodeFromString<UpdateInfo>(body)
                        Result.success(info)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse update JSON: ${e.message}", e)
                        Result.failure(Exception("Failed to parse update info"))
                    }
                } else {
                    Log.e(TAG, "Update response body is null")
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Log.e(TAG, "Update check failed with code: ${response.code}")
                Result.failure(Exception("Server returned error: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error checking for updates: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun isUpdateAvailable(updateInfo: UpdateInfo): Boolean {
        val currentVersionName = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current version name", e)
            "0.0.0"
        }

        return compareVersions(updateInfo.versionCode, currentVersionName) > 0
    }

    /**
     * Compares two version strings.
     * Returns 1 if v1 > v2, -1 if v1 < v2, 0 if equal.
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = v1.removePrefix("v").split(".")
        val cleanV2 = v2.removePrefix("v").split(".")
        
        val maxLength = maxOf(cleanV1.size, cleanV2.size)
        
        for (i in 0 until maxLength) {
            val part1 = cleanV1.getOrNull(i)?.toIntOrNull() ?: 0
            val part2 = cleanV2.getOrNull(i)?.toIntOrNull() ?: 0
            
            if (part1 > part2) return 1
            if (part1 < part2) return -1
        }
        return 0
    }

    fun installApk(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
