package com.shamil.cloudvault.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shamil.cloudvault.utils.Constants
import com.shamil.cloudvault.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

class SettingsPreferenceManager(private val context: Context) {

    private val themeKey = stringPreferencesKey(Constants.PREF_THEME_KEY)
    private val dynamicColorKey = booleanPreferencesKey(Constants.PREF_DYNAMIC_COLOR_KEY)
    private val biometricLockKey = booleanPreferencesKey(Constants.PREF_BIOMETRIC_LOCK_KEY)

    val theme: Flow<AppTheme> = context.dataStore.data
        .catch { exception ->
            Logger.e("SettingsPreferenceManager", "Error reading theme preference", exception)
        }
        .map { preferences ->
            try {
                AppTheme.valueOf(preferences[themeKey] ?: AppTheme.SYSTEM.name)
            } catch (e: Exception) {
                Logger.w("SettingsPreferenceManager", "Invalid theme value, using default")
                AppTheme.SYSTEM
            }
        }

    val dynamicColor: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            Logger.e("SettingsPreferenceManager", "Error reading dynamic color preference", exception)
        }
        .map { preferences ->
            preferences[dynamicColorKey] ?: true
        }

    val biometricLock: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            Logger.e("SettingsPreferenceManager", "Error reading biometric lock preference", exception)
        }
        .map { preferences ->
            preferences[biometricLockKey] ?: false
        }

    suspend fun setTheme(theme: AppTheme) {
        try {
            context.dataStore.edit { preferences ->
                preferences[themeKey] = theme.name
            }
            Logger.d("SettingsPreferenceManager", "Theme set to: ${theme.name}")
        } catch (e: Exception) {
            Logger.e("SettingsPreferenceManager", "Error setting theme", e)
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[dynamicColorKey] = enabled
            }
            Logger.d("SettingsPreferenceManager", "Dynamic color set to: $enabled")
        } catch (e: Exception) {
            Logger.e("SettingsPreferenceManager", "Error setting dynamic color", e)
        }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[biometricLockKey] = enabled
            }
            Logger.d("SettingsPreferenceManager", "Biometric lock set to: $enabled")
        } catch (e: Exception) {
            Logger.e("SettingsPreferenceManager", "Error setting biometric lock", e)
        }
    }

    suspend fun clearAllPreferences() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Logger.i("SettingsPreferenceManager", "All preferences cleared")
        } catch (e: Exception) {
            Logger.e("SettingsPreferenceManager", "Error clearing preferences", e)
        }
    }
}
