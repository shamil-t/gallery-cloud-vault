package com.shamil.cloudvault.ui.settings

import android.app.Application
import android.util.Log
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shamil.cloudvault.data.preferences.AppTheme
import com.shamil.cloudvault.data.preferences.AppThemeStyle
import com.shamil.cloudvault.data.preferences.SettingsPreferenceManager
import com.shamil.cloudvault.data.model.UpdateInfo
import com.shamil.cloudvault.data.network.DownloadWorker
import com.shamil.cloudvault.data.network.UpdateCheckWorker
import com.shamil.cloudvault.data.network.UpdateManager
import com.shamil.cloudvault.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.*
import kotlinx.serialization.json.Json

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

    val currentVersionName = updateManager.getCurrentVersionName()

    init {
        observeDownloadWork()
        observeUpdateCheckWork()
    }

    private fun observeUpdateCheckWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("manual_update_check").collect { workInfos ->
                val workInfo = workInfos.firstOrNull()
                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                    val available = workInfo.outputData.getBoolean(UpdateCheckWorker.KEY_UPDATE_AVAILABLE, false)
                    if (available) {
                        val infoJson = workInfo.outputData.getString(UpdateCheckWorker.KEY_UPDATE_INFO)
                        if (infoJson != null) {
                            try {
                                val updateInfo = Json.decodeFromString<UpdateInfo>(infoJson)
                                if (_updateState.value is UpdateState.Idle || _updateState.value is UpdateState.Checking) {
                                    _updateState.value = UpdateState.Available(updateInfo)
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsViewModel", "Failed to parse auto-update info", e)
                            }
                        }
                    } else if (_updateState.value is UpdateState.Checking) {
                        _updateState.value = UpdateState.NotAvailable
                    }
                }
            }
        }
    }

    private fun observeDownloadWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("update_download").collect { workInfos ->
                val workInfo = workInfos.firstOrNull()
                when (workInfo?.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
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
                    else -> {
                        // If it's idle or not found, we don't change state unless it was already set by checkForUpdates
                    }
                }
            }
        }
    }

    val theme = preferenceManager.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.SYSTEM
    )

    val themeStyle = preferenceManager.themeStyle.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppThemeStyle.AZURE
    )

    val appIconStyle = preferenceManager.appIconStyle.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppThemeStyle.AZURE
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

    val gridColumnCount = preferenceManager.gridColumnCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Constants.DEFAULT_GRID_COLUMN_COUNT
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferenceManager.setTheme(theme)
        }
    }

    fun setThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch {
            preferenceManager.setThemeStyle(style)
        }
    }

    fun setAppIconStyle(style: AppThemeStyle) {
        viewModelScope.launch {
            preferenceManager.setAppIconStyle(style)
            updateAppIcon(style)
        }
    }

    private fun updateAppIcon(style: AppThemeStyle) {
        val application = getApplication<Application>()
        val packageManager = application.packageManager
        val packageName = application.packageName

        val azureComponent = ComponentName(packageName, "$packageName.MainActivityAzure")
        val forestComponent = ComponentName(packageName, "$packageName.MainActivityForest")
        val sunsetComponent = ComponentName(packageName, "$packageName.MainActivitySunset")
        val lavenderComponent = ComponentName(packageName, "$packageName.MainActivityLavender")

        val components = listOf(azureComponent, forestComponent, sunsetComponent, lavenderComponent)
        val targetComponent = when (style) {
            AppThemeStyle.AZURE -> azureComponent
            AppThemeStyle.FOREST -> forestComponent
            AppThemeStyle.SUNSET -> sunsetComponent
            AppThemeStyle.LAVENDER -> lavenderComponent
        }

        components.forEach { component ->
            val newState = if (component == targetComponent) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            if (packageManager.getComponentEnabledSetting(component) != newState) {
                packageManager.setComponentEnabledSetting(
                    component,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            }
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

    fun setGridColumnCount(count: Int) {
        viewModelScope.launch {
            preferenceManager.setGridColumnCount(count)
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            // Replace with your actual update URL
            val updateUrl = "https://raw.githubusercontent.com/shamil-t/gallery-cloud-vault/refs/heads/master/update.json"
            val result = updateManager.checkForUpdates(updateUrl)
            
            result.onSuccess { updateInfo ->
                if (updateManager.isUpdateAvailable(updateInfo)) {
                    _updateState.value = UpdateState.Available(updateInfo)
                } else {
                    _updateState.value = UpdateState.NotAvailable
                }
            }.onFailure { exception ->
                _updateState.value = UpdateState.Error(exception.message ?: "Failed to check for updates")
            }
        }
    }

    fun downloadUpdate(updateInfo: UpdateInfo) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                DownloadWorker.KEY_DOWNLOAD_URL to updateInfo.latestReleaseUrl,
                DownloadWorker.KEY_FILE_NAME to "cloudvault_${updateInfo.versionName}.apk"
            ))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, java.util.concurrent.TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniqueWork(
            "update_download",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun installUpdate(filePath: String) {
        updateManager.installApk(filePath)
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
