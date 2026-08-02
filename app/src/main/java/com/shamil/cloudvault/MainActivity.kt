package com.shamil.cloudvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shamil.cloudvault.ui.screen.HomeScreen
import com.shamil.cloudvault.ui.settings.SettingsViewModel
import com.shamil.cloudvault.ui.theme.CloudVaultTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install and immediately dismiss system splash
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        // Edge-to-edge content
        enableEdgeToEdge()

        setContent {
            // Provide theme settings
            val theme by settingsViewModel.theme.collectAsState()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsState()

            CloudVaultTheme(
                appTheme = theme,
                dynamicColor = dynamicColor
            ) {
                // Track whether to show splash
                val shouldShowSplash = remember { mutableStateOf(true) }

                if (shouldShowSplash.value) {
                    SplashContentScreen(onSplashFinished = { shouldShowSplash.value = false })
                } else {
                    HomeScreen()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SplashContentScreen(onSplashFinished: () -> Unit) {
    // Automatically transition after 1 second for smooth transition from system splash
    LaunchedEffect(Unit) {
        delay(1000)
        onSplashFinished()
    }

    // Gradient background: azure blue palette
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0061A4), // PrimaryLight
            Color(0xFF00497D)  // PrimaryContainerDark / brand_secondary
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Icon with elegant styling
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "CloudVault App Icon",
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name with elegant typography
            Text(
                text = "CloudVault",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline/subtitle for branding
            Text(
                text = "Secure Cloud Storage",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
