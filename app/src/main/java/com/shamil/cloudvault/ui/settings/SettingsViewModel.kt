package com.shamil.cloudvault.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shamil.cloudvault.data.preferences.AppTheme
import com.shamil.cloudvault.data.preferences.SettingsPreferenceManager
import com.shamil.cloudvault.data.model.UpdateInfo
import com.shamil.cloudvault.data.network.DownloadWorker
import com.shamil.cloudvault.data.network.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.*

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val updateInfo: UpdateInfo) : UpdateState()
    object NotAvailable : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val filePath: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferenceManager = SettingsPreferenceManager(application)
    private val updateManager = UpdateManager(application)
    private val workManager = WorkManager.getInstance(application)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    val theme = preferenceManager.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.SYSTEM
    )

    val dynamicColor = preferenceManager.dynamicColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val biometricLock = preferenceManager.biometricLock.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferenceManager.setTheme(theme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setDynamicColor(enabled)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setBiometricLock(enabled)
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            // Replace with your actual update URL
            val updateUrl = "https://raw.githubusercontent.com/shamil-t/CloudVault/main/update.json"
            val updateInfo = updateManager.checkForUpdates(updateUrl)
            if (updateInfo != null) {
                if (updateManager.isUpdateAvailable(updateInfo)) {
                    _updateState.value = UpdateState.Available(updateInfo)
                } else {
                    _updateState.value = UpdateState.NotAvailable
                }
            } else {
                _updateState.value = UpdateState.Error("Failed to check for updates")
            }
        }
    }

    fun downloadUpdate(updateInfo: UpdateInfo) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                DownloadWorker.KEY_DOWNLOAD_URL to updateInfo.latestReleaseUrl,
                DownloadWorker.KEY_FILE_NAME to "cloudvault_${updateInfo.versionName}.apk"
            ))
            .build()

        workManager.enqueueUniqueWork(
            "update_download",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val filePath = workInfo.outputData.getString(DownloadWorker.KEY_FILE_PATH)
                        if (filePath != null) {
                            _updateState.value = UpdateState.ReadyToInstall(filePath)
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        _updateState.value = UpdateState.Error("Download failed")
                    }
                    else -> {}
                }
            }
        }
    }

    fun installUpdate(filePath: String) {
        updateManager.installApk(filePath)
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
