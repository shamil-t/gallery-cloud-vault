package com.shamil.cloudvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shamil.cloudvault.ui.screen.HomeScreen
import com.shamil.cloudvault.ui.settings.SettingsViewModel
import com.shamil.cloudvault.ui.theme.CloudVaultTheme

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge content
        enableEdgeToEdge()

        setContent {
            // Provide theme settings
            val theme by settingsViewModel.theme.collectAsState()
            val themeStyle by settingsViewModel.themeStyle.collectAsState()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsState()

            CloudVaultTheme(
                appTheme = theme,
                appThemeStyle = themeStyle,
                dynamicColor = dynamicColor
            ) {
                HomeScreen()
            }
        }
    }
}
