package com.shamil.cloudvault.ui.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shamil.cloudvault.ui.components.BottomNavigationBar
import com.shamil.cloudvault.ui.gallery.GalleryScreen
import com.shamil.cloudvault.ui.settings.SettingsScreen
import com.shamil.cloudvault.ui.vault.VaultScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Gallery : Screen("gallery", "Gallery", Icons.Default.PhotoLibrary)
    object Vault : Screen("vault", "Vault", Icons.Default.VpnKey)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var isFullScreen by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler(enabled = currentRoute == Screen.Gallery.route) {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (currentRoute == Screen.Vault.route && !isFullScreen) {
                    FloatingActionButton(
                        modifier = Modifier.padding(bottom = 72.dp), // Slightly adjusted offset
                        onClick = { /* TODO: Add image */ }
                    ) {

                        Icon(Icons.Default.Add, contentDescription = "Add Image")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Gallery.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
            ) {
                composable(Screen.Gallery.route) {
                    GalleryScreen(onFullScreenToggle = { isFullScreen = it })
                }
                composable(Screen.Vault.route) {
                    VaultScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }

        if (!isFullScreen) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    }
}




@Preview(showBackground = true)
@Composable
private fun PreviewHomeScreen() {
    HomeScreen()
}
