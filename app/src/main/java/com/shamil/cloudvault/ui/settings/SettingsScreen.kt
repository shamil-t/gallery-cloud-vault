package com.shamil.cloudvault.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shamil.cloudvault.data.preferences.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val biometricLock by viewModel.biometricLock.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Appearance") {
                    SettingsClickableItem(
                        title = "Theme",
                        subtitle = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Palette,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsSwitchItem(
                        title = "Dynamic Color",
                        subtitle = "Use system accent colors",
                        icon = Icons.Default.ColorLens,
                        checked = dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            item {
                SettingsSection(title = "Security") {
                    SettingsSwitchItem(
                        title = "Biometric Lock",
                        subtitle = "Require fingerprint to open vault",
                        icon = Icons.Default.Fingerprint,
                        checked = biometricLock,
                        onCheckedChange = { viewModel.setBiometricLock(it) }
                    )
                }
            }

            item {
                val updateState by viewModel.updateState.collectAsState()
                SettingsSection(title = "Updates") {
                    when (val state = updateState) {
                        is UpdateState.Idle -> {
                            SettingsClickableItem(
                                title = "Check for Updates",
                                subtitle = "Current version: 1.0.0",
                                icon = Icons.Default.Update,
                                onClick = { viewModel.checkForUpdates() }
                            )
                        }
                        is UpdateState.Checking -> {
                            ListItem(
                                headlineContent = { Text("Checking for updates...") },
                                leadingContent = { 
                                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(strokeWidth = 2.dp)
                                    }
                                }
                            )
                        }
                        is UpdateState.Available -> {
                            SettingsClickableItem(
                                title = "Update Available: ${state.updateInfo.versionName}",
                                subtitle = "Click to download",
                                icon = Icons.Default.SystemUpdate,
                                onClick = { viewModel.downloadUpdate(state.updateInfo) }
                            )
                        }
                        is UpdateState.NotAvailable -> {
                            SettingsClickableItem(
                                title = "Up to Date",
                                subtitle = "You have the latest version",
                                icon = Icons.Default.CheckCircle,
                                onClick = { viewModel.checkForUpdates() }
                            )
                        }
                        is UpdateState.Downloading -> {
                            ListItem(
                                headlineContent = { Text("Downloading Update") },
                                supportingContent = { LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) },
                                leadingContent = { Icon(Icons.Default.Downloading, contentDescription = null) },
                                trailingContent = { Text("${state.progress}%") }
                            )
                        }
                        is UpdateState.ReadyToInstall -> {
                            SettingsClickableItem(
                                title = "Update Ready",
                                subtitle = "Click to install",
                                icon = Icons.Default.InstallMobile,
                                onClick = { viewModel.installUpdate(state.filePath) }
                            )
                        }
                        is UpdateState.Error -> {
                            SettingsClickableItem(
                                title = "Update Error",
                                subtitle = state.message,
                                icon = Icons.Default.Error,
                                onClick = { viewModel.checkForUpdates() }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsClickableItem(
                        title = "Version",
                        subtitle = "1.0.0",
                        icon = Icons.Default.Info,
                        onClick = {}
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = theme,
            onThemeSelected = {
                viewModel.setTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = theme.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
