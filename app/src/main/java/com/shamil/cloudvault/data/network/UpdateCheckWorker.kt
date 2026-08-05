package com.shamil.cloudvault.data.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shamil.cloudvault.data.model.UpdateInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UpdateCheckWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val updateManager = UpdateManager(applicationContext)
        val updateUrl = "https://raw.githubusercontent.com/shamil-t/gallery-cloud-vault/refs/heads/master/update.json"
        
        val result = updateManager.checkForUpdates(updateUrl)
        
        return result.fold(
            onSuccess = { updateInfo ->
                if (updateManager.isUpdateAvailable(updateInfo)) {
                    updateManager.showUpdateAvailableNotification(updateInfo)
                    Result.success(workDataOf(
                        KEY_UPDATE_AVAILABLE to true,
                        KEY_UPDATE_INFO to Json.encodeToString(updateInfo)
                    ))
                } else {
                    Result.success(workDataOf(KEY_UPDATE_AVAILABLE to false))
                }
            },
            onFailure = {
                Result.retry()
            }
        )
    }

    companion object {
        const val KEY_UPDATE_AVAILABLE = "update_available"
        const val KEY_UPDATE_INFO = "update_info"
    }
}
