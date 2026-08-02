package com.shamil.cloudvault.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shamil.cloudvault.data.preferences.AppTheme
import com.shamil.cloudvault.data.preferences.SettingsPreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferenceManager = SettingsPreferenceManager(application)

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
}
