package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AerisDarkColorScheme = darkColorScheme(
    primary = AerisDarkPrimary,
    onPrimary = AerisDarkOnPrimary,
    primaryContainer = AerisDarkPrimaryContainer,
    onPrimaryContainer = AerisDarkOnPrimaryContainer,
    secondary = AerisDarkSecondary,
    onSecondary = AerisDarkOnSecondary,
    secondaryContainer = AerisDarkSecondaryContainer,
    onSecondaryContainer = AerisDarkOnSecondaryContainer,
    tertiary = AerisDarkTertiary,
    onTertiary = AerisDarkOnTertiary,
    tertiaryContainer = AerisDarkTertiaryContainer,
    onTertiaryContainer = AerisDarkOnTertiaryContainer,
    background = AerisDarkBackground,
    onBackground = AerisDarkOnBackground,
    surface = AerisDarkSurface,
    onSurface = AerisDarkOnSurface,
    surfaceVariant = AerisDarkSurfaceVariant,
    onSurfaceVariant = AerisDarkOnSurfaceVariant,
    outline = AerisDarkOutline,
    outlineVariant = AerisDarkOutlineVariant
)

@Composable
fun AerisTheme(
    darkTheme: Boolean = true, // Default to deep futuristic dark workspace
    dynamicColor: Boolean = false, // Keep Aeris futuristic aesthetic consistent
    content: @Composable () -> Unit
) {
    val colorScheme = AerisDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AerisDarkBackground.toArgb()
                window.navigationBarColor = AerisDarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
