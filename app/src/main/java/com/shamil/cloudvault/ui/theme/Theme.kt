package com.shamil.cloudvault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.shamil.cloudvault.data.preferences.AppTheme
import com.shamil.cloudvault.data.preferences.AppThemeStyle

private val AzureDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val AzureLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = PrimaryDarkForest,
    onPrimary = OnPrimaryDarkForest,
    primaryContainer = PrimaryContainerDarkForest,
    onPrimaryContainer = OnPrimaryContainerDarkForest,
    secondary = SecondaryDarkForest,
    onSecondary = OnSecondaryDarkForest,
    secondaryContainer = SecondaryContainerDarkForest,
    onSecondaryContainer = OnSecondaryContainerDarkForest,
    background = BackgroundDarkForest,
    onBackground = OnBackgroundDarkForest,
    surface = SurfaceDarkForest,
    onSurface = OnSurfaceDarkForest
)

private val ForestLightColorScheme = lightColorScheme(
    primary = PrimaryLightForest,
    onPrimary = OnPrimaryLightForest,
    primaryContainer = PrimaryContainerLightForest,
    onPrimaryContainer = OnPrimaryContainerLightForest,
    secondary = SecondaryLightForest,
    onSecondary = OnSecondaryLightForest,
    secondaryContainer = SecondaryContainerLightForest,
    onSecondaryContainer = OnSecondaryContainerLightForest,
    background = BackgroundLightForest,
    onBackground = OnBackgroundLightForest,
    surface = SurfaceLightForest,
    onSurface = OnSurfaceLightForest
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = PrimaryDarkSunset,
    onPrimary = OnPrimaryDarkSunset,
    primaryContainer = PrimaryContainerDarkSunset,
    onPrimaryContainer = OnPrimaryContainerDarkSunset,
    secondary = SecondaryDarkSunset,
    onSecondary = OnSecondaryDarkSunset,
    secondaryContainer = SecondaryContainerDarkSunset,
    onSecondaryContainer = OnSecondaryContainerDarkSunset,
    background = BackgroundDarkSunset,
    onBackground = OnBackgroundDarkSunset,
    surface = SurfaceDarkSunset,
    onSurface = OnSurfaceDarkSunset
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = PrimaryLightSunset,
    onPrimary = OnPrimaryLightSunset,
    primaryContainer = PrimaryContainerLightSunset,
    onPrimaryContainer = OnPrimaryContainerLightSunset,
    secondary = SecondaryLightSunset,
    onSecondary = OnSecondaryLightSunset,
    secondaryContainer = SecondaryContainerLightSunset,
    onSecondaryContainer = OnSecondaryContainerLightSunset,
    background = BackgroundLightSunset,
    onBackground = OnBackgroundLightSunset,
    surface = SurfaceLightSunset,
    onSurface = OnSurfaceLightSunset
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = PrimaryDarkLavender,
    onPrimary = OnPrimaryDarkLavender,
    primaryContainer = PrimaryContainerDarkLavender,
    onPrimaryContainer = OnPrimaryContainerDarkLavender,
    secondary = SecondaryDarkLavender,
    onSecondary = OnSecondaryDarkLavender,
    secondaryContainer = SecondaryContainerDarkLavender,
    onSecondaryContainer = OnSecondaryContainerDarkLavender,
    background = BackgroundDarkLavender,
    onBackground = OnBackgroundDarkLavender,
    surface = SurfaceDarkLavender,
    onSurface = OnSurfaceDarkLavender
)

private val LavenderLightColorScheme = lightColorScheme(
    primary = PrimaryLightLavender,
    onPrimary = OnPrimaryLightLavender,
    primaryContainer = PrimaryContainerLightLavender,
    onPrimaryContainer = OnPrimaryContainerLightLavender,
    secondary = SecondaryLightLavender,
    onSecondary = OnSecondaryLightLavender,
    secondaryContainer = SecondaryContainerLightLavender,
    onSecondaryContainer = OnSecondaryContainerLightLavender,
    background = BackgroundLightLavender,
    onBackground = OnBackgroundLightLavender,
    surface = SurfaceLightLavender,
    onSurface = OnSurfaceLightLavender
)

@Composable
fun CloudVaultTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    appThemeStyle: AppThemeStyle = AppThemeStyle.AZURE,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> when (appThemeStyle) {
            AppThemeStyle.AZURE -> AzureDarkColorScheme
            AppThemeStyle.FOREST -> ForestDarkColorScheme
            AppThemeStyle.SUNSET -> SunsetDarkColorScheme
            AppThemeStyle.LAVENDER -> LavenderDarkColorScheme
        }

        else -> when (appThemeStyle) {
            AppThemeStyle.AZURE -> AzureLightColorScheme
            AppThemeStyle.FOREST -> ForestLightColorScheme
            AppThemeStyle.SUNSET -> SunsetLightColorScheme
            AppThemeStyle.LAVENDER -> LavenderLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}